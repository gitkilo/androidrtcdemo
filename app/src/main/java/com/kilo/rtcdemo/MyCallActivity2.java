package com.kilo.rtcdemo;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;

import org.appspot.apprtc.AppRTCAudioManager;
import org.appspot.apprtc.AppRTCClient;
import org.appspot.apprtc.PeerConnectionClient;
import org.appspot.apprtc.util.AsyncHttpURLConnection;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.Camera1Enumerator;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraEnumerator;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.Logging;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RendererCommon;
import org.webrtc.SessionDescription;
import org.webrtc.StatsReport;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoFrame;
import org.webrtc.VideoSink;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;

public class MyCallActivity2 extends Activity implements PeerConnectionClient.PeerConnectionEvents {
    private static final String TAG = "MyCallActivity2";

    private static class ProxyVideoSink implements VideoSink {
        private VideoSink target;

        @Override
        synchronized public void onFrame(VideoFrame frame) {
            if (target == null) {
                Logging.d(TAG, "Dropping frame in proxy because target is null.");
                return;
            }

            target.onFrame(frame);
        }

        synchronized public void setTarget(VideoSink target) {
            this.target = target;
        }
    }
    // List of mandatory application permissions.
    private static final String[] MANDATORY_PERMISSIONS = {"android.permission.MODIFY_AUDIO_SETTINGS",
            "android.permission.RECORD_AUDIO", "android.permission.INTERNET"};
    private int peerId;
    private int toPeerId;
    private boolean passivity; // 是否被动
    // True if local view is in the fullscreen renderer.
    private boolean isSwappedFeeds;
    @Nullable
    private SurfaceViewRenderer pipRenderer;
    @Nullable
    private SurfaceViewRenderer fullscreenRenderer;
    @Nullable
    private PeerConnectionClient peerConnectionClient;
    private final List<VideoSink> remoteSinks = new ArrayList<>();
    @Nullable
    private PeerConnectionClient.PeerConnectionParameters peerConnectionParameters;
    @Nullable
    private AppRTCClient.SignalingParameters signalingParameters;
    @Nullable
    private AppRTCAudioManager audioManager;
    private final MyCallActivity2.ProxyVideoSink remoteProxyRenderer = new MyCallActivity2.ProxyVideoSink();
    private final MyCallActivity2.ProxyVideoSink localProxyVideoSink = new MyCallActivity2.ProxyVideoSink();
    private SPHelper spHelper;
    private List<PeerConnection.IceServer> iceServers;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mycall);
        // Check for mandatory permissions.
        for (String permission : MANDATORY_PERMISSIONS) {
            if (checkCallingOrSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                showToast("Permission " + permission + " is not granted");
                setResult(RESULT_CANCELED);
                finish();
                return;
            }
        }

        spHelper = SPHelper.getInstance(this);
        final EglBase eglBase = EglBase.create();
        initData(eglBase);
        createPeerconnectParam(eglBase);

        // Create and audio manager that will take care of audio routing,
        // audio modes, audio device enumeration etc.
        audioManager = AppRTCAudioManager.create(getApplicationContext());
//        audioManager.setDefaultAudioDevice(AppRTCAudioManager.AudioDevice.SPEAKER_PHONE);
        // Store existing audio settings and change audio mode to
        // MODE_IN_COMMUNICATION for best possible VoIP performance.
        Log.d(TAG, "Starting the audio manager...");
        audioManager.start(new AppRTCAudioManager.AudioManagerEvents() {
            // This method will be called each time the number of available audio
            // devices has changed.
            @Override
            public void onAudioDeviceChanged(
                    AppRTCAudioManager.AudioDevice audioDevice, Set<AppRTCAudioManager.AudioDevice> availableAudioDevices) {
                showToast("onAudioManagerDevicesChanged: " + availableAudioDevices + ", "
                        + "selected: " + audioDevice);
            }
        });
//        if (!passivity)
        {
            startCall();
        }
    }

    private void initData(EglBase eglBase){
        fullscreenRenderer = findViewById(R.id.fullscreen_video_view);
        pipRenderer = findViewById(R.id.pip_video_view);
        Intent intent = getIntent();
        if (null == intent)
        {
            return;
        }
        peerId = intent.getIntExtra("peer_id", 0);
        toPeerId = intent.getIntExtra("to_peer_id", 0);
        passivity = intent.getBooleanExtra("passivity", false);
        pipRenderer.init(eglBase.getEglBaseContext(), null);
        pipRenderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
//        fullscreenRenderer.setOnClickListener(listener);
        remoteSinks.add(remoteProxyRenderer);

        fullscreenRenderer.init(eglBase.getEglBaseContext(), null);
        fullscreenRenderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL);

        pipRenderer.setZOrderMediaOverlay(true);
        pipRenderer.setEnableHardwareScaler(true /* enabled */);
        fullscreenRenderer.setEnableHardwareScaler(false /* enabled */);
        setSwappedFeeds(false /* isSwappedFeeds */);
    }

    private void setSwappedFeeds(boolean isSwappedFeeds) {
        Logging.d(TAG, "setSwappedFeeds: " + isSwappedFeeds);
        this.isSwappedFeeds = isSwappedFeeds;
        localProxyVideoSink.setTarget(isSwappedFeeds ? fullscreenRenderer : pipRenderer);
        remoteProxyRenderer.setTarget(isSwappedFeeds ? pipRenderer : fullscreenRenderer);
        fullscreenRenderer.setMirror(isSwappedFeeds);
        pipRenderer.setMirror(!isSwappedFeeds);
    }
    private void createPeerconnectParam(EglBase eglBase)
    {
        PeerConnectionClient.DataChannelParameters dataChannelParameters = null;
//        if (intent.getBooleanExtra(EXTRA_DATA_CHANNEL_ENABLED, false)) {
//            dataChannelParameters = new PeerConnectionClient.DataChannelParameters(intent.getBooleanExtra(EXTRA_ORDERED, true),
//                    intent.getIntExtra(EXTRA_MAX_RETRANSMITS_MS, -1),
//                    intent.getIntExtra(EXTRA_MAX_RETRANSMITS, -1), intent.getStringExtra(EXTRA_PROTOCOL),
//                    intent.getBooleanExtra(EXTRA_NEGOTIATED, false), intent.getIntExtra(EXTRA_ID, -1));
//        }

        // Video call enabled flag.
        boolean videoCallEnabled = sharedPrefGetBoolean(R.string.pref_videocall_key, R.string.pref_videocall_default);

        // Use screencapture option.
//        boolean useScreencapture = sharedPrefGetBoolean(R.string.pref_screencapture_key, R.string.pref_screencapture_default);

        // Use Camera2 option.
//        boolean useCamera2 = sharedPrefGetBoolean(R.string.pref_camera2_key, R.string.pref_camera2_default);

        // Get default codecs.
        String videoCodec = sharedPrefGetString(R.string.pref_videocodec_key, R.string.pref_videocodec_default);
        String audioCodec = sharedPrefGetString(R.string.pref_audiocodec_key, R.string.pref_audiocodec_default);

        // Check HW codec flag.
        boolean hwCodec = sharedPrefGetBoolean(R.string.pref_hwcodec_key, R.string.pref_hwcodec_default);

        // Check Capture to texture.
//        boolean captureToTexture = sharedPrefGetBoolean(R.string.pref_capturetotexture_key, R.string.pref_capturetotexture_default);

        // Check FlexFEC.
        boolean flexfecEnabled = sharedPrefGetBoolean(R.string.pref_flexfec_key, R.string.pref_flexfec_default);

        // Check Disable Audio Processing flag.
        boolean noAudioProcessing = sharedPrefGetBoolean(R.string.pref_noaudioprocessing_key, R.string.pref_noaudioprocessing_default);

        boolean aecDump = sharedPrefGetBoolean(R.string.pref_aecdump_key, R.string.pref_aecdump_default);

        boolean saveInputAudioToFile =
                sharedPrefGetBoolean(R.string.pref_enable_save_input_audio_to_file_key,
                        R.string.pref_enable_save_input_audio_to_file_default);

        // Check OpenSL ES enabled flag.
        boolean useOpenSLES = sharedPrefGetBoolean(R.string.pref_opensles_key, R.string.pref_opensles_default);

        // Check Disable built-in AEC flag.
        boolean disableBuiltInAEC = sharedPrefGetBoolean(R.string.pref_disable_built_in_aec_key, R.string.pref_disable_built_in_aec_default);

        // Check Disable built-in AGC flag.
        boolean disableBuiltInAGC = sharedPrefGetBoolean(R.string.pref_disable_built_in_agc_key, R.string.pref_disable_built_in_agc_default);

        // Check Disable built-in NS flag.
        boolean disableBuiltInNS = sharedPrefGetBoolean(R.string.pref_disable_built_in_ns_key, R.string.pref_disable_built_in_ns_default);

        // Check Disable gain control
        boolean disableWebRtcAGCAndHPF = sharedPrefGetBoolean(
                R.string.pref_disable_webrtc_agc_and_hpf_key, R.string.pref_disable_webrtc_agc_and_hpf_key);
        boolean enableRtcEventLog = sharedPrefGetBoolean(R.string.pref_enable_rtceventlog_key, R.string.pref_enable_rtceventlog_default);
//        int audioStartBitrate = SPHelper.getInstance(this).getSP().getInt(getString(R.string.pref_startaudiobitratevalue_key), 32);
        int audioStartBitrate = Integer.parseInt(sharedPrefGetString(R.string.pref_startaudiobitratevalue_key,
                R.string.pref_startaudiobitratevalue_default));
        int videoMaxBitrate = Integer.parseInt(sharedPrefGetString(R.string.pref_maxvideobitratevalue_key,
                R.string.pref_maxvideobitratevalue_default));
        int videoWidth = 0, videoHeight = 0;
        String resolution = sharedPrefGetString(R.string.pref_resolution_key, R.string.pref_resolution_default);
        String[] dimensions = resolution.split("[ x]+");
        if (dimensions.length == 2) {
            try {
                videoWidth = Integer.parseInt(dimensions[0]);
                videoHeight = Integer.parseInt(dimensions[1]);
            } catch (NumberFormatException e) {
                videoWidth = 0;
                videoHeight = 0;
                Log.e(TAG, "Wrong video resolution setting: " + resolution);
            }
        }

        String fps = sharedPrefGetString(R.string.pref_fps_key, R.string.pref_fps_default);
        int cameraFps = 0;
        String[] fpsValues = fps.split("[ x]+");
        if (fpsValues.length == 2) {
            try {
                cameraFps = Integer.parseInt(fpsValues[0]);
            } catch (NumberFormatException e) {
                cameraFps = 0;
                Log.e(TAG, "Wrong camera fps setting: " + fps);
            }
        }
        peerConnectionParameters =
                new PeerConnectionClient.PeerConnectionParameters(videoCallEnabled, false,
                        false, videoWidth, videoHeight, cameraFps,
                        videoMaxBitrate, videoCodec,
                        hwCodec,
                        flexfecEnabled,
                        audioStartBitrate,
                        audioCodec,
                        noAudioProcessing,
                        aecDump,
                        saveInputAudioToFile,
                        useOpenSLES,
                        disableBuiltInAEC,
                        disableBuiltInAGC,
                        disableBuiltInNS,
                        disableWebRtcAGCAndHPF,
                        enableRtcEventLog,
                        dataChannelParameters);

        peerConnectionClient = new PeerConnectionClient(
                getApplicationContext(), eglBase, peerConnectionParameters, this);
        PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
//        if (loopback) {
//            options.networkIgnoreMask = 0;
//        }
        peerConnectionClient.createPeerConnectionFactory(options);
    }

    /**
     * Get a value from the shared preference or from the intent, if it does not
     * exist the default is used.
     */
    private boolean sharedPrefGetBoolean(
            int attributeId, int defaultId) {
        boolean defaultValue = Boolean.parseBoolean(getString(defaultId));
        String attributeName = getString(attributeId);
        return SPHelper.getInstance(this).getSP().getBoolean(attributeName, defaultValue);
    }

    @Nullable
    private String sharedPrefGetString(
            int attributeId, int defaultId) {
        String defaultValue = getString(defaultId);
        String attributeName = getString(attributeId);
        return SPHelper.getInstance(this).getSP().getString(attributeName, defaultValue);
    }

    private void startCall()
    {
        if (!passivity) {
            signalingParameters = new AppRTCClient.SignalingParameters(new ArrayList<>(),
                    true, null, null, null, null, null);
            openVideo();
            // Create offer. Offer SDP will be sent to answering client in
            // PeerConnectionEvents.onLocalDescription event.
            peerConnectionClient.createOffer();
        }
        else
        {
            sendWait("", -1);
        }
    }

    private void openVideo()
    {
        VideoCapturer videoCapturer = null;
        if (peerConnectionParameters.videoCallEnabled) {
            videoCapturer = createVideoCapturer();
        }

        peerConnectionClient.createPeerConnection(
                localProxyVideoSink, remoteSinks, videoCapturer, signalingParameters);
    }
    private boolean useCamera2() {
        return Camera2Enumerator.isSupported(this);
    }

    private @Nullable VideoCapturer createCameraCapturer(CameraEnumerator enumerator) {
        final String[] deviceNames = enumerator.getDeviceNames();
        // First, try to find front facing camera
        Logging.d(TAG, "Looking for front facing cameras.");
        for (String deviceName : deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                Logging.d(TAG, "Creating front facing camera capturer.");
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);

                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }
        // Front facing camera not found, try something else
        Logging.d(TAG, "Looking for other cameras.");
        for (String deviceName : deviceNames) {
            if (!enumerator.isFrontFacing(deviceName)) {
                Logging.d(TAG, "Creating other camera capturer.");
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);
                if (videoCapturer != null)
                {
                    return videoCapturer;
                }
            }
        }
        return null;
    }
    private @Nullable VideoCapturer createVideoCapturer() {
        final VideoCapturer videoCapturer;
        if (useCamera2()) {
            Logging.d(TAG, "Creating capturer using camera2 API.");
            videoCapturer = createCameraCapturer(new Camera2Enumerator(this));
        } else {
            Logging.d(TAG, "Creating capturer using camera1 API.");
            videoCapturer = createCameraCapturer(new Camera1Enumerator(true));
        }
        if (videoCapturer == null) {
            showToast("Failed to open camera");
            return null;
        }
        return videoCapturer;
    }

    private void sendSignOut()
    {
        final String url = "http://" + SPHelper.getInstance(this).getIp() + ":" + SPHelper.getInstance(this).getPort() + "/sign_out?peer_id=" + peerId;
        final String message = "";
        AsyncHttpURLConnection httpConnection =
                new AsyncHttpURLConnection("GET", url, message, new AsyncHttpURLConnection.AsyncHttpEvents() {
                    @Override
                    public void onHttpError(String errorMessage) {
                    }

                    @Override
                    public void onHttpComplete(String response) {

                    }

                    @Override
                    public void onPeerId(String pId) {
                    }
                });
        httpConnection.send();
    }

    private void sendMessage(final String message)
    {
        sendPost(message);
//        final String url = "http://" + spHelper.getIp() + ":" + spHelper.getPort() + "/message?peer_id=" + peerId +"&to=" + toPeerId;
//        AsyncHttpURLConnection httpConnection =
//                new AsyncHttpURLConnection("POST", url, message, new AsyncHttpURLConnection.AsyncHttpEvents() {
//                    @Override
//                    public void onHttpError(String errorMessage) {
//                        Log.e(TAG, "Room connection error: " + errorMessage);
//                        showToast("sendWait error." + errorMessage);
//                    }
//
//                    @Override
//                    public void onHttpComplete(String response)
//                    {
//                        showToast("sendMessage response:" + response);
//                    }
//
//                    @Override
//                    public void onPeerId(String pId) {
//                    }
//                }, peerId);
//        httpConnection.send();
    }
    private void sendPost(String message) {
//        final String url = "http://" + spHelper.getIp() + ":" + spHelper.getPort() + "/message?peer_id=" + peerId +"&to=" + toPeerId;
        Executors.newCachedThreadPool().execute(() -> {
            try {
                Socket rawSocket = new Socket(spHelper.getIp(), spHelper.getPort());
                BufferedReader in;
                PrintWriter out;
                out = new PrintWriter(
                        new OutputStreamWriter(rawSocket.getOutputStream(), Charset.forName("UTF-8")), true);
                in = new BufferedReader(
                        new InputStreamReader(rawSocket.getInputStream(), Charset.forName("UTF-8")));
                StringBuffer sb = new StringBuffer();
                sb.append("POST /message?peer_id=").append(peerId).append("&to=").append(toPeerId).append("HTTP/1.0\r\n");
                sb.append("Content-Length: " + message.length() + "\r\n");
                sb.append("Content-Type: text/plain\r\n");
                sb.append("\r\n");
                sb.append(message);
                out.write(sb.toString() + "\n");
                out.flush();

                sb.setLength(0);
                while (true) {
                    String readMsg;
                    try {
                        readMsg = in.readLine();
                    } catch (IOException e) {
                        break;
                    }
                    sb.append(readMsg);
                    if (readMsg == null) {
                        continue;
                    }
                }

                rawSocket.close();
            } catch (IOException e) {
                showToast("send message error. connect error.");
            }
        });
    }
    protected void parseWaitResult(JSONObject json)
    {
        if (!json.has("type") && json.has("candidate"))
        {
            runOnUiThread(() -> {
                if (peerConnectionClient == null) {
                    Log.e(TAG, "Received ICE candidate for a non-initialized peer connection.");
                    return;
                }
                IceCandidate candidate = null;
                try {
                    candidate = new IceCandidate(json.getString("sdpMid"),
                            json.getInt("sdpMLineIndex"), json.getString("candidate"));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                if (passivity)
                {
                    if (null == signalingParameters)
                    {
                        signalingParameters = new AppRTCClient.SignalingParameters(new ArrayList<>(),
                                false, null, null, null, null, new ArrayList<>());
                    }
                    signalingParameters.iceCandidates.add(candidate);
                }
                else
                {
                    peerConnectionClient.addRemoteIceCandidate(candidate);
                }
            });
        }
        else if (json.has("type"))
        {
            try
            {
                String type = json.getString("type");
                if (type.equals("candidate"))
                {
                    IceCandidate candidate = new IceCandidate(json.getString("id"), json.getInt("label"), json.getString("candidate"));
                    if (passivity)
                    {
                        if (null == signalingParameters)
                        {
                            signalingParameters = new AppRTCClient.SignalingParameters(new ArrayList<>(),
                                    false, null, null, null, null, new ArrayList<>());
                        }
                        signalingParameters.iceCandidates.add(candidate);
                    }
                    else
                    {
                        peerConnectionClient.addRemoteIceCandidate(candidate);
                    }
                }
                else if (type.equals("answer"))
                {
                    SessionDescription sdp = new SessionDescription(
                            SessionDescription.Type.fromCanonicalForm(type), json.getString("sdp"));
                    if (null == signalingParameters)
                    {
                        signalingParameters = new AppRTCClient.SignalingParameters(new ArrayList<>(),
                                false, null, null, null, sdp, new ArrayList<>());
                    }
                    else
                    {
                        signalingParameters.offerSdp = sdp;
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (peerConnectionClient == null) {
                                Log.e(TAG, "Received remote SDP for non-initilized peer connection.");
                                return;
                            }
                            peerConnectionClient.setRemoteDescription(sdp);
                        }
                    });
                }
                else if (type.equals("offer"))
                { // 被动
                    SessionDescription sdp = new SessionDescription(
                            SessionDescription.Type.fromCanonicalForm(type), json.getString("sdp"));
                    if (null == signalingParameters)
                    {
                        signalingParameters = new AppRTCClient.SignalingParameters(new ArrayList<>(),
                                false, null, null, null, sdp, new ArrayList<>());
                    }
                    else
                    {
                        signalingParameters.offerSdp = sdp;
                    }
                    openVideo();
                    if (signalingParameters.offerSdp != null) {
                        peerConnectionClient.setRemoteDescription(signalingParameters.offerSdp);
                        // Create answer. Answer SDP will be sent to offering client in
                        // PeerConnectionEvents.onLocalDescription event.
                    }
                    if (signalingParameters.iceCandidates != null) {
                        // Add remote ICE candidates from room.
                        for (IceCandidate iceCandidate : signalingParameters.iceCandidates) {
                            peerConnectionClient.addRemoteIceCandidate(iceCandidate);
                        }
                    }
                    peerConnectionClient.createAnswer();
                }
            }
            catch (JSONException e)
            {

            }
        }
    }

    private void sendWait(String message, int peerId)
    {
        // "GET /wait?peer_id=" + peerId + " HTTP/1.0\r\n\r\n"
        final String url = "http://" + spHelper.getIp() + ":" + spHelper.getPort() + "/wait?peer_id=" + this.peerId;
        AsyncHttpURLConnection httpConnection =
                new AsyncHttpURLConnection("GET", url, message, new AsyncHttpURLConnection.AsyncHttpEvents() {
                    @Override
                    public void onHttpError(String errorMessage) {
                        Log.e(TAG, "Room connection error: " + errorMessage);
                        showToast("sendWait error." + errorMessage);
                    }

                    @Override
                    public void onHttpComplete(String response) {
                        showToast("sendWait response:" + response);
                        if (isEmpty(response))
                        {
                            return;
                        }
                        try
                        {
                            JSONObject json = new JSONObject(response);
                            parseWaitResult(json);
                            sendWait("", -1);
                        }
                        catch (Exception e)
                        {

                        }
                    }

                    @Override
                    public void onPeerId(String pId) {
                    }
                }, peerId);
        httpConnection.send();
    }

    private void showToast(final String content)
    {
        runOnUiThread(() -> Toast.makeText(MyCallActivity2.this, content, Toast.LENGTH_SHORT).show());
        Log.d(TAG, "===>" + content);
    }

    private boolean isEmpty(String val)
    {
        return null == val || "".equals(val.trim());
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Video is not paused for screencapture. See onPause.
        if (peerConnectionClient != null) {
            peerConnectionClient.startVideoSource();
        }
//        if (cpuMonitor != null) {
//            cpuMonitor.resume();
//        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (peerConnectionClient != null) {
            peerConnectionClient.stopVideoSource();
        }
//        if (cpuMonitor != null) {
//            cpuMonitor.pause();
//        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        sendSignOut();
        remoteProxyRenderer.setTarget(null);
        localProxyVideoSink.setTarget(null);
        if (pipRenderer != null)
        {
            pipRenderer.release();
            pipRenderer = null;
        }
        if (fullscreenRenderer != null)
        {
            fullscreenRenderer.release();
            fullscreenRenderer = null;
        }
        if (peerConnectionClient != null)
        {
            peerConnectionClient.close();
            peerConnectionClient = null;
        }
        if (audioManager != null)
        {
            audioManager.stop();
            audioManager = null;
        }
    }
    /********************************************************************************/
    public void sendOfferSdp(final SessionDescription sdp) {
        JSONObject json = new JSONObject();
        jsonPut(json, "sdp", sdp.description);
        jsonPut(json, "type", "offer");
//        sendWait(json.toString(), toPeerId);
        sendMessage(json.toString());
        sendWait("", -1);
    }

    public void sendAnswerSdp(final SessionDescription sdp) {
        JSONObject json = new JSONObject();
        jsonPut(json, "sdp", sdp.description);
        jsonPut(json, "type", "answer");
        sendMessage(json.toString());
    }
    // Put a |key|->|value| mapping in |json|.
    private static void jsonPut(JSONObject json, String key, Object value) {
        try {
            json.put(key, value);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }
    // Converts a Java candidate to a JSONObject.
    private static JSONObject toJsonCandidate(final IceCandidate candidate) {
        JSONObject json = new JSONObject();
        jsonPut(json, "label", candidate.sdpMLineIndex);
        jsonPut(json, "id", candidate.sdpMid);
        jsonPut(json, "candidate", candidate.sdp);
        return json;
    }

    public void sendLocalIceCandidate(final IceCandidate candidate) {
//        JSONObject json = new JSONObject();
//        jsonPut(json, "type", "candidate");
//        jsonPut(json, "label", candidate.sdpMLineIndex);
//        jsonPut(json, "id", candidate.sdpMid);
//        jsonPut(json, "candidate", candidate.sdp);
        JSONObject json = new JSONObject();
        jsonPut(json, "candidate", candidate.sdp);
        jsonPut(json, "sdpMLineIndex", candidate.sdpMLineIndex);
        jsonPut(json, "sdpMid", candidate.sdpMid);
        sendMessage(json.toString());
    }
    public void sendLocalIceCandidateRemovals(final IceCandidate[] candidates) {
        JSONObject json = new JSONObject();
        jsonPut(json, "type", "remove-candidates");
        JSONArray jsonArray = new JSONArray();
        for (final IceCandidate candidate : candidates) {
            jsonArray.put(toJsonCandidate(candidate));
        }
        jsonPut(json, "candidates", jsonArray);
//        sendMessage(json.toString());
    }
    /*******************************************************************************/
    @Override
    public void onLocalDescription(SessionDescription sdp) {
//        final long delta = System.currentTimeMillis() - callStartedTimeMs;
        runOnUiThread(() -> {
            showToast("Sending " + sdp.type);
            if (!passivity) {
                sendOfferSdp(sdp);
            } else {
                sendAnswerSdp(sdp);
            }
            if (peerConnectionParameters.videoMaxBitrate > 0) {
                Log.d(TAG, "Set video maximum bitrate: " + peerConnectionParameters.videoMaxBitrate);
                peerConnectionClient.setVideoMaxBitrate(peerConnectionParameters.videoMaxBitrate);
            }
        });
    }

    @Override
    public void onIceCandidate(IceCandidate candidate) {
        sendLocalIceCandidate(candidate);
    }

    @Override
    public void onIceCandidatesRemoved(IceCandidate[] candidates) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                sendLocalIceCandidateRemovals(candidates);
            }
        });
    }

    @Override
    public void onIceConnected() {
        showToast("onIceConnected");
    }

    @Override
    public void onIceDisconnected() {
        showToast("onIceDisconnected");
    }

    @Override
    public void onConnected() {
        showToast("onConnected");
    }

    @Override
    public void onDisconnected() {
        showToast("onDisconnected");
        finish();
    }

    @Override
    public void onPeerConnectionClosed() {
        showToast("onPeerConnectionClosed");
    }

    @Override
    public void onPeerConnectionStatsReady(StatsReport[] reports) {
        showToast("onPeerConnectionStatsReady");
    }

    @Override
    public void onPeerConnectionError(String description) {
        showToast("onPeerConnectionError");
    }
}
