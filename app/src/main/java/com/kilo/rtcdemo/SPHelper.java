package com.kilo.rtcdemo;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import javax.annotation.Nullable;

public class SPHelper
{
    private Context mContext;
    private SharedPreferences sharedPref;
    private SharedPreferences.Editor editor;
    private static final String SERV_IP = "_serv_ip_";
    private static final String SERV_PORT = "_serv_port_";
    private static final String PEER_NAME = "_peer_name_";

    private static SPHelper instance;
    private SPHelper(@Nullable Context context)
    {
        mContext = context;
        PreferenceManager.setDefaultValues(mContext, R.xml.preferences, false);
        sharedPref = PreferenceManager.getDefaultSharedPreferences(mContext);
        editor = sharedPref.edit();
    }

    public static SPHelper getInstance(Context context)
    {
        if (null == instance)
        {
            instance = new SPHelper(context);
        }
        return instance;
    }
    public String getIp()
    {
        return sharedPref.getString(SERV_IP, "192.168.0.125");
    }
    public void setIp(String ip)
    {
        editor.putString(SERV_IP, ip);
        editor.commit();
    }
    public int getPort()
    {
        return sharedPref.getInt(SERV_PORT, 8888);
    }
    public void setPort(int port)
    {
        editor.putInt(SERV_PORT, port);
        editor.commit();
    }
    public String getPeerName()
    {
        return sharedPref.getString(PEER_NAME, "android");
    }
    public void setPeerName(String name)
    {
        editor.putString(PEER_NAME, name);
        editor.commit();
    }

    public SharedPreferences getSP() {
        return sharedPref;
    }
}
