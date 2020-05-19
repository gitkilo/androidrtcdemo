package org.appspot.apprtc;

import android.util.Log;

import org.appspot.apprtc.util.AsyncHttpURLConnection;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PCRTCClient implements AppRTCClient
{
    private static final String TAG = "PCRTCClient";
    private static final int DEFAULT_PORT = 8888;
    // Regex pattern used for checking if room id looks like an IP.
    static final Pattern IP_PATTERN = Pattern.compile("("
            // IPv4
            + "((\\d+\\.){3}\\d+)|"
            // IPv6
            + "\\[((([0-9a-fA-F]{1,4}:)*[0-9a-fA-F]{1,4})?::"
            + "(([0-9a-fA-F]{1,4}:)*[0-9a-fA-F]{1,4})?)\\]|"
            + "\\[(([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4})\\]|"
            // IPv6 without []
            + "((([0-9a-fA-F]{1,4}:)*[0-9a-fA-F]{1,4})?::(([0-9a-fA-F]{1,4}:)*[0-9a-fA-F]{1,4})?)|"
            + "(([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4})|"
            // Literals
            + "localhost"
            + ")"
            // Optional port number
            + "(:(\\d+))?");

    private RoomConnectionParameters connectionParameters;

    private final ExecutorService executor;
    private final SignalingEvents events;

    public enum ConnectionState { NEW, CONNECT_READY, CONNECTED, CLOSED, ERROR }

    // All alterations of the room state should be done from inside the looper thread.
    private PCRTCClient.ConnectionState roomState;
    private String peerId;
    private String toPeerId;

    public PCRTCClient(SignalingEvents events) {
        this.events = events;
        executor = Executors.newSingleThreadExecutor();
        roomState = PCRTCClient.ConnectionState.NEW;
    }

    public ConnectionState getRoomState()
    {
        return roomState;
    }
    @Override
    public void connectToRoom(RoomConnectionParameters connectionParameters) {
        this.connectionParameters = connectionParameters;
        executor.execute(() -> connectToRoomInternal());
    }

    private void connectToRoomInternal()
    {
        String endpoint = connectionParameters.roomId;

        Matcher matcher = IP_PATTERN.matcher(endpoint);
        if (!matcher.matches()) {
            reportError("roomId must match IP_PATTERN for DirectRTCClient.");
            return;
        }

        String ip = matcher.group(1);
        String portStr = matcher.group(matcher.groupCount());
        int port;

        if (portStr != null) {
            try {
                port = Integer.parseInt(portStr);
            } catch (NumberFormatException e) {
                reportError("Invalid port number: " + portStr);
                return;
            }
        } else {
            port = DEFAULT_PORT;
        }
        signIn();
    }

    @Override
    public void sendOfferSdp(SessionDescription sdp) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                if (roomState != PCRTCClient.ConnectionState.CONNECTED) {
                    reportError("Sending offer SDP in non connected state.");
                    return;
                }
                JSONObject json = new JSONObject();
                jsonPut(json, "sdp", sdp.description);
                jsonPut(json, "type", "offer");
//                sendWait(json.toString());
            }
        });
    }

    @Override
    public void sendAnswerSdp(SessionDescription sdp) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                JSONObject json = new JSONObject();
                jsonPut(json, "sdp", sdp.description);
                jsonPut(json, "type", "answer");
                sendWait(sdp);
                sendMessage(json.toString());
            }
        });
    }

    @Override
    public void sendLocalIceCandidate(IceCandidate candidate) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                JSONObject json = new JSONObject();
                jsonPut(json, "type", "candidate");
                jsonPut(json, "label", candidate.sdpMLineIndex);
                jsonPut(json, "id", candidate.sdpMid);
                jsonPut(json, "candidate", candidate.sdp);

                if (roomState != PCRTCClient.ConnectionState.CONNECTED) {
                    reportError("Sending ICE candidate in non connected state.");
                    return;
                }
                sendMessage(json.toString());
            }
        });
    }

    @Override
    public void sendLocalIceCandidateRemovals(IceCandidate[] candidates) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                JSONObject json = new JSONObject();
                jsonPut(json, "type", "remove-candidates");
                JSONArray jsonArray = new JSONArray();
                for (final IceCandidate candidate : candidates) {
                    jsonArray.put(toJsonCandidate(candidate));
                }
                jsonPut(json, "candidates", jsonArray);

                if (roomState != PCRTCClient.ConnectionState.CONNECTED) {
                    reportError("Sending ICE candidate removals in non connected state.");
                    return;
                }
                sendMessage(json.toString());
            }
        });
    }

    @Override
    public void disconnectFromRoom()
    {
        executor.execute(() -> signOut());
    }

    private void signOut()
    {
        final String reqUrl = connectionParameters.roomUrl + String.format("/sign_out?peer_id=%i", peerId);
        makeGetRequest(reqUrl, null, (response) ->
        {

        });
    }

    private void signIn()
    {
        final String reqUrl = connectionParameters.roomUrl + String.format("/sign_in?%s", "android1");
        makeGetRequest(reqUrl, null, (response) ->
        {
            Log.d(TAG, "Room response: " + response);
            if (null == response || "".equals(response.trim()))
            {
                return;
            }
            String[] resp = response.split(",");
            if (resp.length < 3)
            {
                return;
            }
            peerId = resp[1];
            sendWait(null);
        });
    }

    void sendWait(SessionDescription sdp)
    {
        JSONObject json = new JSONObject();
        String message = null;
        if (null != sdp)
        {
            jsonPut(json, "sdp", sdp.description);
            jsonPut(json, "type", "answer");
            message = json.toString();
        }
        final String reqUrl = connectionParameters.roomUrl + String.format("/wait?peer_id=%s", peerId);
        makeGetRequest(reqUrl, message, (response) ->
        {
            onParseWait(response);
        });
    }

    private void sendMessage(String message)
    {
        final String reqUrl = connectionParameters.roomUrl + String.format("/message?peer_id=%s&to=%s", peerId, toPeerId);
        makePostRequest(reqUrl, message, (response) ->
        {

        });
    }

    public void onParseWait(String msg) {
        try {
            JSONObject json = new JSONObject(msg);
            String type = json.optString("type");
            if (type.equals("candidate")) {
                events.onRemoteIceCandidate(toJavaCandidate(json));
            } else if (type.equals("remove-candidates")) {
                JSONArray candidateArray = json.getJSONArray("candidates");
                IceCandidate[] candidates = new IceCandidate[candidateArray.length()];
                for (int i = 0; i < candidateArray.length(); ++i) {
                    candidates[i] = toJavaCandidate(candidateArray.getJSONObject(i));
                }
                events.onRemoteIceCandidatesRemoved(candidates);
            } else if (type.equals("answer")) {
                SessionDescription sdp = new SessionDescription(
                        SessionDescription.Type.fromCanonicalForm(type), json.getString("sdp"));
                events.onRemoteDescription(sdp);
            } else if (type.equals("offer")) {
                SessionDescription sdp = new SessionDescription(
                        SessionDescription.Type.fromCanonicalForm(type), json.getString("sdp"));

                SignalingParameters parameters = new SignalingParameters(
                        // Ice servers are not needed for direct connections.
                        new ArrayList<>(),
                        false, // This code will only be run on the client side. So, we are not the initiator.
                        null, // clientId
                        null, // wssUrl
                        null, // wssPostUrl
                        sdp, // offerSdp
                        null // iceCandidates
                );
                roomState = PCRTCClient.ConnectionState.CONNECTED;
                events.onConnectedToRoom(parameters);
            } else {
                reportError("Unexpected TCP message: " + msg);
            }
        } catch (JSONException e) {
            reportError("TCP message JSON parsing error: " + e.toString());
        }
    }

    public void makeGetRequest(String url, String message, OnReportListener listener) {
        Log.d(TAG, "Connecting to get room: " + url);
        AsyncHttpURLConnection httpConnection =
                new AsyncHttpURLConnection("GET", url, message, new AsyncHttpURLConnection.AsyncHttpEvents() {
                    @Override
                    public void onHttpError(String errorMessage) {
                        Log.e(TAG, "Room connection error: " + errorMessage);
//                        events.onSignalingParametersError(errorMessage);
                    }

                    @Override
                    public void onHttpComplete(String response) {
                        listener.onReport(response);
//                        peerId = roomHttpResponseParse(response);
//                        sendWait();
                    }

                    @Override
                    public void onPeerId(String peerId) {
                        toPeerId = peerId;
                    }
                });
        httpConnection.send();
    }

    public void makePostRequest(String url, String message, OnReportListener listener)
    {
        Log.d(TAG, "Connecting to post room: " + url);
        AsyncHttpURLConnection httpConnection =
                new AsyncHttpURLConnection("POST", url, message, new AsyncHttpURLConnection.AsyncHttpEvents() {
                    @Override
                    public void onHttpError(String errorMessage) {
                        Log.e(TAG, "Room connection error: " + errorMessage);
                    }

                    @Override
                    public void onHttpComplete(String response) {
                        listener.onReport(response);
//                        peerId = roomHttpResponseParse(response);
//                        sendWait();
                    }

                    @Override
                    public void onPeerId(String peerId) {
                        toPeerId = peerId;
                    }
                });
        httpConnection.send();
    }

    // Converts a Java candidate to a JSONObject.
    private static JSONObject toJsonCandidate(final IceCandidate candidate) {
        JSONObject json = new JSONObject();
        jsonPut(json, "label", candidate.sdpMLineIndex);
        jsonPut(json, "id", candidate.sdpMid);
        jsonPut(json, "candidate", candidate.sdp);
        return json;
    }

    // Converts a JSON candidate to a Java object.
    private static IceCandidate toJavaCandidate(JSONObject json) throws JSONException {
        return new IceCandidate(
                json.getString("id"), json.getInt("label"), json.getString("candidate"));
    }

    // Put a |key|->|value| mapping in |json|.
    private static void jsonPut(JSONObject json, String key, Object value) {
        try {
            json.put(key, value);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    // Helper functions.
    private void reportError(final String errorMessage) {
        Log.e(TAG, errorMessage);
        executor.execute(new Runnable() {
            @Override
            public void run() {
                if (roomState != PCRTCClient.ConnectionState.ERROR)
                {
                    roomState = PCRTCClient.ConnectionState.ERROR;
                    events.onChannelError(errorMessage);
                }
            }
        });
    }

    interface OnReportListener
    {
        void onReport(String rep);
    }
}
