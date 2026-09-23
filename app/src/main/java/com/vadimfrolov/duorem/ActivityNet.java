/*
 * Copyright (C) 2017 Vadim Frolov
 * Licensed under GNU's GPL 3 or any later version, see README
 */
package com.vadimfrolov.duorem;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;

import com.vadimfrolov.duorem.Network.NetInfo;

public abstract class ActivityNet extends BaseActivity {
    protected NetInfo mNetInfo;
    protected boolean mIsConnected;
    private ConnectivityManager manager;
    private boolean resumed;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ActivityResultLauncher<String> permission = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), granted -> refreshNetwork());

    private final ConnectivityManager.NetworkCallback callback = new ConnectivityManager.NetworkCallback() {
        private void refresh() {
            handler.post(() -> {
                if (resumed) {
                    refreshNetwork();
                }
            });
        }

        @Override public void onAvailable(Network network) { refresh(); }
        @Override public void onLost(Network network) { refresh(); }
        @Override public void onCapabilitiesChanged(Network network, NetworkCapabilities caps) { refresh(); }
        @Override public void onLinkPropertiesChanged(Network network, LinkProperties props) { refresh(); }
    };

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        manager = getSystemService(ConnectivityManager.class);
        mNetInfo = new NetInfo(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        resumed = true;
        manager.registerNetworkCallback(new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED).build(), callback);
        refreshNetwork();
    }

    @Override
    protected void onPause() {
        resumed = false;
        manager.unregisterNetworkCallback(callback);
        handler.removeCallbacksAndMessages(null);
        super.onPause();
    }

    protected boolean hasNetworkPermission() {
        return Build.VERSION.SDK_INT < 37
                || checkSelfPermission(Manifest.permission.ACCESS_LOCAL_NETWORK) == PackageManager.PERMISSION_GRANTED;
    }

    protected void requestNetworkPermission() {
        if (Build.VERSION.SDK_INT < 37 || hasNetworkPermission()) {
            refreshNetwork();
            return;
        }
        boolean asked = getPreferences(MODE_PRIVATE).getBoolean("asked_local_network", false);
        if (asked && !shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_LOCAL_NETWORK)) {
            new AlertDialog.Builder(this).setMessage(R.string.network_permission_required)
                    .setPositiveButton(R.string.settings, (dialog, which) -> startActivity(
                            new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.parse("package:" + getPackageName()))))
                    .setNegativeButton(android.R.string.cancel, null).show();
        } else {
            new AlertDialog.Builder(this).setMessage(R.string.network_permission_required)
                    .setPositiveButton(R.string.allow_network, (dialog, which) -> {
                        getPreferences(MODE_PRIVATE).edit().putBoolean("asked_local_network", true).apply();
                        permission.launch(Manifest.permission.ACCESS_LOCAL_NETWORK);
                    })
                    .setNegativeButton(android.R.string.cancel, null).show();
        }
    }

    private void refreshNetwork() {
        mNetInfo = new NetInfo(this);
        mIsConnected = mNetInfo.isConnected && hasNetworkPermission();
        updateNetworkStatus();
    }

    protected abstract void updateNetworkStatus();

    public static String getVersionName(Context context) {
        try {
            return context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            throw new IllegalStateException("Application package is missing", e);
        }
    }
}
