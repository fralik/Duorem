/*
 * Copyright (C) 2017 Vadim Frolov
 * Licensed under GNU's GPL 3 or any later version, see README
 */

package com.vadimfrolov.twobuttonremote;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.vadimfrolov.twobuttonremote.Network.NetInfo;

/**
 * Activity, which should be used instead of AppCompatActivity in order to get network status notifications.
 */

public abstract class ActivityNet extends AppCompatActivity {
    ConnectivityManager mConnMgr;
    NetInfo mNetInfo = null;
    Context mContext;
    boolean mIsConnected;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getApplicationContext();
        mConnMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        mNetInfo = new NetInfo(mContext);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Listening for network events
        IntentFilter filter = new IntentFilter();
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(mNetworkReceiver, filter);
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(mNetworkReceiver);
    }

    protected abstract void updateNetworkStatus();

    private BroadcastReceiver mNetworkReceiver = new BroadcastReceiver() {
        public void onReceive(Context ctxt, Intent intent) {
            // check if host is reachable
            NetworkInfo ni = mConnMgr.getActiveNetworkInfo();
            mIsConnected = ni != null && ni.isConnectedOrConnecting();

            updateNetworkStatus();
        }
    };
}

