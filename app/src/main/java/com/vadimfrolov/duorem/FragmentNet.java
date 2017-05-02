/*
 * Copyright (C) 2017 Vadim Frolov
 * Licensed under GNU's GPL 3 or any later version, see README
 */

package com.vadimfrolov.duorem;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

import com.vadimfrolov.duorem.Network.NetInfo;

/**
 * Similar to ActivityNet, but for a Fragment.
 * Created by vadimf on 4/4/2017.
 */

public abstract class FragmentNet extends Fragment {
    protected ConnectivityManager mConnMgr;
    protected NetInfo mNetInfo = null;
    protected Context mContext;
    boolean mIsConnected;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getActivity().getApplicationContext();
        mConnMgr = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        mNetInfo = new NetInfo(mContext);
    }

    @Override
    public void onResume() {
        super.onResume();

        // Listening for network events
        IntentFilter filter = new IntentFilter();
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        getActivity().registerReceiver(mNetworkReceiver, filter);

        // check connection right away
        NetworkInfo ni = mConnMgr.getActiveNetworkInfo();
        mIsConnected = ni != null && ni.isConnectedOrConnecting();
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(mNetworkReceiver);
    }

    protected abstract void updateNetworkStatus();

    private BroadcastReceiver mNetworkReceiver = new BroadcastReceiver() {
        public void onReceive(Context ctxt, Intent intent) {
            // check if host is reachable
            NetworkInfo ni = mConnMgr.getActiveNetworkInfo();
            mIsConnected = ni != null && ni.isConnectedOrConnecting();

            if (mIsConnected) {
                mNetInfo.obtainIp();
                mNetInfo.getWifiInfo();
            }

            updateNetworkStatus();
        }
    };
}
