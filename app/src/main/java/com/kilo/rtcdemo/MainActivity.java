package com.kilo.rtcdemo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import org.appspot.apprtc.util.AsyncHttpURLConnection;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity implements PeerListAdapter.OnClickListener{
    private static final String TAG = "MainActivity";
    private EditText etIp;
    private EditText etPort;
    private EditText etPeerName;
    private ListView lvPeer;
    private SPHelper spHelper;
    private int peerId;
    private List<Peer> peerList = new ArrayList<>();
    private PeerListAdapter listAdapter;
    private boolean needWait;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setupViews();
    }
    private void setupViews()
    {
        etIp = findViewById(R.id.et_ip);
        etPort = findViewById(R.id.et_port);
        etPeerName = findViewById(R.id.et_peer_name);
        spHelper = SPHelper.getInstance(this);
        etIp.setText(spHelper.getIp());
        etPort.setText(spHelper.getPort() + "");
        etPeerName.setText(spHelper.getPeerName());

        lvPeer = findViewById(R.id.lv_peer);
        listAdapter = new PeerListAdapter(this, this, peerList);
        lvPeer.setAdapter(listAdapter);
    }

    public void onClick(View v)
    {
        int id = v.getId();
        if (id == R.id.btn_sign)
        {
            final String ip = etIp.getText().toString();
            final String portStr = etPort.getText().toString();
            final String peerName = etPeerName.getText().toString();
            if (isEmpty(ip) || isEmpty(portStr) || isEmpty(peerName))
            {
                showToast("请输入必要值");
                return;
            }
            spHelper.setIp(ip);
            spHelper.setPort(Integer.parseInt(portStr));
            spHelper.setPeerName(peerName);
            sigin(ip, portStr, peerName);
        }
    }
    private void sigin(final String ip, final String port, String peerName)
    {
        final String url = "http://" + ip + ":" + port + "/sign_in?" + peerName;
        final String message = "";
        AsyncHttpURLConnection httpConnection =
                new AsyncHttpURLConnection("GET", url, message, new AsyncHttpURLConnection.AsyncHttpEvents() {
                    @Override
                    public void onHttpError(String errorMessage) {
                        Log.e(TAG, "Room connection error: " + errorMessage);
                        showToast("sign error." + errorMessage);
                    }

                    @Override
                    public void onHttpComplete(String response) {
                        showToast("sign success.");
                        showToast("sendWait:" + response);
                        if (isEmpty(response))
                        {
                            return;
                        }
                        peerList.clear();
                        String[] list = response.split("\n");
                        for (String item : list)
                        {
                            String[] detail = item.split(",");
                            if (Integer.parseInt(detail[1]) == peerId)
                            {
                                continue;
                            }
                            Peer peer = new Peer();
                            peer.setId(Integer.parseInt(detail[1]));
                            peer.setName(detail[0]);
                            peerList.add(peer);
                        }
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                listAdapter.notifyDataSetChanged();
                            }
                        });
                        sendWait(ip, port);
                    }

                    @Override
                    public void onPeerId(String pId) {
                        peerId = Integer.parseInt(pId);
                    }
                });
        httpConnection.send();
    }

    private void sendWait(final String ip, final String port)
    {
        // "GET /wait?peer_id=" + peerId + " HTTP/1.0\r\n\r\n"
        final String url = "http://" + ip + ":" + port + "/wait?peer_id=" + peerId;
        final String message = "";
        AsyncHttpURLConnection httpConnection =
                new AsyncHttpURLConnection("GET", url, message, new AsyncHttpURLConnection.AsyncHttpEvents() {
                    String toPeerId;
                    @Override
                    public void onHttpError(String errorMessage) {
                        Log.e(TAG, "Room connection error: " + errorMessage);
                        showToast("sendWait error." + errorMessage);
                    }

                    @Override
                    public void onHttpComplete(String response) {
                        showToast("sendWait:" + response);
                        if (isEmpty(response) || !needWait)
                        {
                            if (null != MyCallActivity.instance)
                            {
                                try
                                {
                                    MyCallActivity.instance.parseWaitResult(new JSONObject(response));
                                }
                                catch (Exception e)
                                {

                                }
                            }
                            return;
                        }
                        try
                        {
                            JSONObject json = new JSONObject(response);
                            if (json.has("type"))
                            {
                                if (json.getString("type").equals("offer"))
                                {
                                    Intent intent = new Intent(MainActivity.this, MyCallActivity.class);
                                    intent.putExtra("sdp", json.getString("sdp"));
                                    intent.putExtra("peer_id", peerId);
                                    intent.putExtra("to_peer_id", Integer.parseInt(toPeerId));
                                    startActivity(intent);
                                    return;
                                }
                                else if (json.getString("type").equals("candidate"))
                                {
                                    Intent intent = new Intent(MainActivity.this, MyCallActivity.class);
                                    intent.putExtra("candidate", response);
                                    intent.putExtra("peer_id", peerId);
                                    intent.putExtra("to_peer_id", Integer.parseInt(toPeerId));
                                    startActivity(intent);
                                    return;
                                }
                            }
                            else if (json.has("candidate"))
                            {
                                Intent intent = new Intent(MainActivity.this, MyCallActivity.class);
                                intent.putExtra("candidate", response);
                                intent.putExtra("peer_id", peerId);
                                intent.putExtra("to_peer_id", Integer.parseInt(toPeerId));
                                intent.putExtra("recv_from_mobile", true);
                                startActivity(intent);
                                return;
                            }
                        }
                        catch (Exception e)
                        {

                        }
                        peerList.clear();
                        String[] list = response.split("\n");
                        for (String item : list)
                        {
                            String[] detail = item.split(",");
                            Peer peer = new Peer();
                            peer.setId(Integer.parseInt(detail[1]));
                            peer.setName(detail[0]);
                            peerList.add(peer);
                        }
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                listAdapter.notifyDataSetChanged();
                            }
                        });
                        if (needWait)
                        {
                            sendWait(ip, port);
                        }
                    }

                    @Override
                    public void onPeerId(String pId) {
                        if (!isEmpty(pId))
                        {
                            toPeerId = pId;
                            showToast("wait recv peerId:" + pId);
                        }
                    }
                });
        httpConnection.send();
    }
    private void showToast(final String content)
    {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, content, Toast.LENGTH_SHORT).show();
            }
        });
        Log.d(TAG, "===>" + content);
    }

    @Override
    protected void onResume() {
        super.onResume();
        needWait = true;
    }

    private boolean isEmpty(String val)
    {
        return null == val || "".equals(val.trim());
    }

    @Override
    public void onClick(Peer peer) {
        needWait = false;
        Intent intent = new Intent(this, MyCallActivity.class);
        intent.putExtra("peer_id", peerId);
        intent.putExtra("to_peer_id", peer.getId());
        startActivity(intent);
    }
}
