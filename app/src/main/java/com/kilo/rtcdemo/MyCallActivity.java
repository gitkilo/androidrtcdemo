package com.kilo.rtcdemo;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.Nullable;

import org.appspot.apprtc.AppRTCAudioManager;
import org.appspot.apprtc.AppRTCClient;
import org.appspot.apprtc.DirectRTCClient;
import org.appspot.apprtc.PeerConnectionClient;
import org.appspot.apprtc.util.AsyncHttpURLConnection;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.Camera1Enumerator;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraEnumerator;
import org.webrtc.EglBase;
import org.webrtc.FileVideoCapturer;
import org.webrtc.IceCandidate;
import org.webrtc.Logging;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RendererCommon;
import org.webrtc.SessionDescription;
import org.webrtc.StatsReport;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoFrame;
import org.webrtc.VideoSink;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;

public class MyCallActivity extends Activity implements PeerConnectionClient.PeerConnectionEvents {
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
    private static final String TAG = "MyCallActivity";
    // List of mandatory application permissions.
    private static final String[] MANDATORY_PERMISSIONS = {"android.permission.MODIFY_AUDIO_SETTINGS",
            "android.permission.RECORD_AUDIO", "android.permission.INTERNET"};
    private int peerId;
    private int toPeerId;
    private String recvSdp;
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
    private final MyCallActivity.ProxyVideoSink remoteProxyRenderer = new MyCallActivity.ProxyVideoSink();
    private final MyCallActivity.ProxyVideoSink localProxyVideoSink = new MyCallActivity.ProxyVideoSink();
    private SPHelper spHelper;
    public static MyCallActivity instance;
    private List<String> ices = new ArrayList<>();
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        instance = this;
//        requestWindowFeature(Window.FEATURE_NO_TITLE);
//        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
//                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
//        getWindow().getDecorView().setSystemUiVisibility(getSystemUiVisibility());
        setContentView(R.layout.activity_mycall);
        spHelper = SPHelper.getInstance(this);
        final EglBase eglBase = EglBase.create();
        setupViews(eglBase);
        createPeerconnectParam();
        peerConnectionClient = new PeerConnectionClient(
                getApplicationContext(), eglBase, peerConnectionParameters, this);
        PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
//        if (loopback) {
//            options.networkIgnoreMask = 0;
//        }
        peerConnectionClient.createPeerConnectionFactory(options);

        // Create and audio manager that will take care of audio routing,
        // audio modes, audio device enumeration etc.
        audioManager = AppRTCAudioManager.create(getApplicationContext());
        // Store existing audio settings and change audio mode to
        // MODE_IN_COMMUNICATION for best possible VoIP performance.
        Log.d(TAG, "Starting the audio manager...");
        audioManager.start(new AppRTCAudioManager.AudioManagerEvents() {
            // This method will be called each time the number of available audio
            // devices has changed.
            @Override
            public void onAudioDeviceChanged(
                    AppRTCAudioManager.AudioDevice audioDevice, Set<AppRTCAudioManager.AudioDevice> availableAudioDevices) {
                onAudioManagerDevicesChanged(audioDevice, availableAudioDevices);
            }
        });
        Executors.newCachedThreadPool().execute(() -> startCall());
//        String candidate = getIntent().getStringExtra("candidate");
//        if (!isEmpty(candidate))
//        {
//            try
//            {
//                parseWaitResult(new JSONObject(candidate));
//            }
//            catch (JSONException e)
//            {
//
//            }
//        }
    }
    private void createPeerconnectParam()
    {
        PeerConnectionClient.DataChannelParameters dataChannelParameters = null;
//        if (intent.getBooleanExtra(EXTRA_DATA_CHANNEL_ENABLED, false)) {
//            dataChannelParameters = new PeerConnectionClient.DataChannelParameters(intent.getBooleanExtra(EXTRA_ORDERED, true),
//                    intent.getIntExtra(EXTRA_MAX_RETRANSMITS_MS, -1),
//                    intent.getIntExtra(EXTRA_MAX_RETRANSMITS, -1), intent.getStringExtra(EXTRA_PROTOCOL),
//                    intent.getBooleanExtra(EXTRA_NEGOTIATED, false), intent.getIntExtra(EXTRA_ID, -1));
//        }
        peerConnectionParameters =
                new PeerConnectionClient.PeerConnectionParameters(true, false,
                        false, 640, 480, 15,
                        256, "VP8",
                        true,
                        false,
                        0, "OPUS",
                        false,
                        false,
                        false,
                        false,
                        false,
                        true,
                        true,
                        true,
                        false,
                        dataChannelParameters);
    }
    @TargetApi(19)
    private static int getSystemUiVisibility() {
        int flags = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            flags |= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        }
        return flags;
    }
    private void setupViews(EglBase eglBase){
        fullscreenRenderer = findViewById(R.id.fullscreen_video_view);
        pipRenderer = findViewById(R.id.pip_video_view);
        Intent intent = getIntent();
        if (null == intent)
        {
            return;
        }
        peerId = intent.getIntExtra("peer_id", 0);
        toPeerId = intent.getIntExtra("to_peer_id", 0);
        recvSdp = intent.getStringExtra("response");
        pipRenderer.init(eglBase.getEglBaseContext(), null);
        pipRenderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
        if (isEmpty(recvSdp))
        {
            signalingParameters = new AppRTCClient.SignalingParameters(new ArrayList<>(),
                    true, null, null, null, null, null);
        }
        else
        {
            SessionDescription sdp = new SessionDescription(SessionDescription.Type.OFFER, recvSdp);
            signalingParameters = new AppRTCClient.SignalingParameters(new ArrayList<>(),
                    false, null, null, null, sdp, null);
        }
//        fullscreenRenderer.setOnClickListener(listener);
        remoteSinks.add(remoteProxyRenderer);

        fullscreenRenderer.init(eglBase.getEglBaseContext(), null);
        fullscreenRenderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL);

        pipRenderer.setZOrderMediaOverlay(true);
        pipRenderer.setEnableHardwareScaler(true /* enabled */);
        fullscreenRenderer.setEnableHardwareScaler(false /* enabled */);
        setSwappedFeeds(false /* isSwappedFeeds */);

        // Check for mandatory permissions.
        for (String permission : MANDATORY_PERMISSIONS) {
            if (checkCallingOrSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                showToast("Permission " + permission + " is not granted");
                setResult(RESULT_CANCELED);
                finish();
                return;
            }
        }
    }
    private void setSwappedFeeds(boolean isSwappedFeeds) {
        Logging.d(TAG, "setSwappedFeeds: " + isSwappedFeeds);
        this.isSwappedFeeds = isSwappedFeeds;
        localProxyVideoSink.setTarget(isSwappedFeeds ? fullscreenRenderer : pipRenderer);
        remoteProxyRenderer.setTarget(isSwappedFeeds ? pipRenderer : fullscreenRenderer);
        fullscreenRenderer.setMirror(isSwappedFeeds);
        pipRenderer.setMirror(!isSwappedFeeds);
    }

    private void startCall()
    {
        VideoCapturer videoCapturer = null;
        if (peerConnectionParameters.videoCallEnabled) {
            videoCapturer = createVideoCapturer();
        }
        peerConnectionClient.createPeerConnection(
                localProxyVideoSink, remoteSinks, videoCapturer, signalingParameters);
        if (signalingParameters.initiator) {
            // Create offer. Offer SDP will be sent to answering client in
            // PeerConnectionEvents.onLocalDescription event.
            peerConnectionClient.createOffer();
        } else {

            sendWait("", -1);
            if (signalingParameters.offerSdp != null) {
                peerConnectionClient.setRemoteDescription(signalingParameters.offerSdp);
                // Create answer. Answer SDP will be sent to offering client in
                // PeerConnectionEvents.onLocalDescription event.
                peerConnectionClient.createAnswer();
            }
            if (signalingParameters.iceCandidates != null) {
                // Add remote ICE candidates from room.
                for (IceCandidate iceCandidate : signalingParameters.iceCandidates) {
                    peerConnectionClient.addRemoteIceCandidate(iceCandidate);
                }
            }
        }


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
    // This method is called when the audio manager reports audio device change,
    // e.g. from wired headset to speakerphone.
    private void onAudioManagerDevicesChanged(
            final AppRTCAudioManager.AudioDevice device, final Set<AppRTCAudioManager.AudioDevice> availableDevices) {
        Log.d(TAG, "onAudioManagerDevicesChanged: " + availableDevices + ", "
                + "selected: " + device);
        // TODO(henrika): add callback handler.
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
        instance = null;
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

    private void sendSignOut()
    {
        final String url = "http://" + spHelper.getIp() + ":" + spHelper.getPort() + "/sign_out?peer_id=" + peerId;
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
        final String url = "http://" + spHelper.getIp() + ":" + spHelper.getPort() + "/message?peer_id=" + peerId +"&to=" + toPeerId;
        AsyncHttpURLConnection httpConnection =
                new AsyncHttpURLConnection("POST", url, message, new AsyncHttpURLConnection.AsyncHttpEvents() {
                    @Override
                    public void onHttpError(String errorMessage) {
                        Log.e(TAG, "Room connection error: " + errorMessage);
                        showToast("sendWait error." + errorMessage);
                    }

                    @Override
                    public void onHttpComplete(String response)
                    {
                        showToast("sendMessage response:" + response);
                    }

                    @Override
                    public void onPeerId(String pId) {
                    }
                }, peerId);
        httpConnection.send();
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
                peerConnectionClient.addRemoteIceCandidate(candidate);
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
                    peerConnectionClient.addRemoteIceCandidate(candidate);
                }
                else if (type.equals("answer"))
                {
                    SessionDescription sdp = new SessionDescription(
                            SessionDescription.Type.fromCanonicalForm(type), json.getString("sdp"));
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
                    if (!ices.isEmpty())
                    {
                        for (String ice : ices)
                        {
                            sendMessage(ice);
                        }
                    }
                }
            }
            catch (JSONException e)
            {

            }

        }

    }
    private void showToast(final String content)
    {
        runOnUiThread(() -> Toast.makeText(MyCallActivity.this, content, Toast.LENGTH_SHORT).show());
        Log.d(TAG, "===>" + content);
    }
    private boolean isEmpty(String val)
    {
        return null == val || "".equals(val.trim());
    }
    public void sendOfferSdp(final SessionDescription sdp) {
        JSONObject json = new JSONObject();
        jsonPut(json, "sdp", sdp.description);
        jsonPut(json, "type", "offer");
        sendWait(json.toString(), toPeerId);
//        sendMessage(json.toString());
//        sendWait("", -1);
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
        ices.add(json.toString());
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
    /********************************************************************************/
    @Override
    public void onLocalDescription(SessionDescription sdp) {
//        final long delta = System.currentTimeMillis() - callStartedTimeMs;
        runOnUiThread(() -> {
            showToast("Sending " + sdp.type);
            if (signalingParameters.initiator) {
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

    /********************************************************************************/
}
