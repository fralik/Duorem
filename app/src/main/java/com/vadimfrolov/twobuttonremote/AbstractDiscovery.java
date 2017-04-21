/*
 * Copyright (C) 2009-2010 Aubort Jean-Baptiste (Rorist)
 * Copyright (C) 2017 Vadim Frolov
 * Licensed under GNU's GPL 2, see README
 */

package com.vadimfrolov.twobuttonremote;

import android.os.AsyncTask;

import com.vadimfrolov.twobuttonremote.Network.HostBean;
import com.vadimfrolov.twobuttonremote.Network.NetInfo;

import java.lang.ref.WeakReference;

/**
 * Defines abstract interface to perform asynchronous network discovery operation.
 *
 * Based on AbstractDiscovery from android-network-discovery
 */

public abstract class AbstractDiscovery extends AsyncTask<Void, HostBean, Void> {
    final protected WeakReference<DiscoveryListener> mDiscover;

    protected long mIp;
    protected long mStart = 0;
    protected long mEnd = 0;
    protected long mSize = 0;
    // Counter that is used to report discovery progress
    protected int mHostsDone = 0;
    protected long mBroadcastIp = 0;

    public AbstractDiscovery(DiscoveryListener discover) {
        mDiscover = new WeakReference<DiscoveryListener>(discover);
    }

    public void setNetwork(long ip, long start, long end) {
        mIp = ip;
        mStart = start;
        mEnd = end;
    }

    public void setBroadcastIp(long ip) {
        mBroadcastIp = ip;
    }

    abstract protected Void doInBackground(Void... params);

    @Override
    protected void onPreExecute() {
        mSize = (int) (mEnd - mStart + 1);
        mHostsDone = 0;
        final DiscoveryListener discover = mDiscover.get();
        if (discover != null) {
            discover.onStartDiscovering();
        }
    }

    @Override
    protected void onProgressUpdate(HostBean... host) {
        final DiscoveryListener discover = mDiscover.get();
        if (discover != null) {
            if (!isCancelled()) {
                if (host[0] != null) {
                    host[0].broadcastIp = NetInfo.getIpFromLongUnsigned(mBroadcastIp);
                    discover.onNewHost(host[0]);
                }
                if (mSize > 0) {
                    mHostsDone++;
                    discover.setDiscoverProgress((int) (mHostsDone * 10000 / mSize));
                }
            }
        }
    }

    @Override
    protected void onPostExecute(Void unused) {
        final DiscoveryListener discover = mDiscover.get();
        if (discover != null) {
//            discover.makeToast(R.string.discover_finished);
            discover.onStopDiscovering();
        }
    }

    @Override
    protected void onCancelled() {
        final DiscoveryListener discover = mDiscover.get();
        if (discover != null) {
            //discover.makeToast(R.string.discover_canceled);
            discover.onStopDiscovering();
        }
        super.onCancelled();
    }
}
