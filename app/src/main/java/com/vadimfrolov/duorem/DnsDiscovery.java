/*
 * Copyright (C) 2009-2010 Aubort Jean-Baptiste (Rorist)
 * Copyright (C) 2017 Vadim Frolov
 * Licensed under GNU's GPL 3 or any later version, see README
 */

package com.vadimfrolov.duorem;

import android.util.Log;

import com.vadimfrolov.duorem.Network.HardwareAddress;
import com.vadimfrolov.duorem.Network.HostBean;
import com.vadimfrolov.duorem.Network.NetInfo;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Based on DnsDiscovery from {@link https://github.com/rorist/android-network-discovery|Android Network Discovery} app.
 * Changes:
 * 1. Perform search in batches instead of a single for loop.
 * 2. Filter gateways out.
 * 3. Do not check NIC vendor.
 */

public class DnsDiscovery extends AbstractDiscovery {

    private final String TAG = "DnsDiscovery";
    private final static long sChunkSize = 10;
    // number of threads for pool of async tasks.
    private final static int sThreads = 10;
    private ExecutorService mPool;
    private final static int TIMEOUT_SCAN = 60; // seconds
    private final static int TIMEOUT_SHUTDOWN = 10; // seconds

    public DnsDiscovery(DiscoveryListener discover) {
        super(discover);
    }

    @Override
    protected Void doInBackground(Void... params) {
        if (mDiscover != null) {
            final DiscoveryListener discover = mDiscover.get();
            if (discover != null) {
                int timeout = discover.getTimeout();
                long numChunks = mSize / sChunkSize;
                long numLeft = mSize % sChunkSize;
                mPool = Executors.newFixedThreadPool(sThreads);

                for (long i = 1; i <= numChunks; i++) {
                    long start = mStart-1 + ((i - 1) * sChunkSize + 1);
                    long stop = start + sChunkSize - 1;
                    launch(start, stop, timeout, discover.getGatewayIp());
                }
                if (numLeft > 0) {
                    long start = mStart-1 + (numChunks * sChunkSize + 1);
                    long stop = mEnd;
                    launch(start, stop, timeout, discover.getGatewayIp());
                }
                mPool.shutdown();
                try {
                    if (!mPool.awaitTermination(TIMEOUT_SCAN, TimeUnit.SECONDS)) {
                        mPool.shutdownNow();
                        if (!mPool.awaitTermination(TIMEOUT_SHUTDOWN, TimeUnit.SECONDS)) {
                            Log.w(TAG, "Pool did not shutdown");
                        }
                    }
                } catch (InterruptedException e) {
                    mPool.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }
        }
        return null;
    }

    @Override
    protected void onCancelled() {
        if (mPool != null) {
            synchronized (mPool) {
                mPool.shutdown();
            }
        }
        super.onCancelled();
    }

    private void launch(long start, long stop, int timeout, String gatewayIp) {
        if (!mPool.isShutdown()) {
            Log.d(TAG, "Making new pool " + start + "-" + stop + ", " + NetInfo.getIpFromLongUnsigned(start) + "-" + NetInfo.getIpFromLongUnsigned(stop));
            mPool.execute(new CheckRunnable(start, stop, timeout, gatewayIp));
        }
    }

    private class CheckRunnable implements Runnable {
        private final long mStart;
        private final long mStop;
        private final int mTimeout;
        private final String mGatewayIp;

        CheckRunnable(long start, long stop, int timeout, String gatewayIp) {
            mStart = start;
            mStop = stop;
            mTimeout = timeout;
            mGatewayIp = gatewayIp;
        }

        private boolean isReachable(String addr, int openPort, int timeOutMillis) {
            // Any Open port on other machine
            // openPort =  22 - ssh, 80 or 443 - webserver, 25 - mailserver etc.
            try {
                try (Socket soc = new Socket()) {
                    soc.connect(new InetSocketAddress(addr, openPort), timeOutMillis);
                }
                return true;
            } catch (IOException ex) {
                return false;
            }
        }

        public void run() {
            if (isCancelled()) {
                publishProgress((HostBean)null);
                return;
            }

            for (long i = mStart; i <= mStop; i++) {
                HostBean host = new HostBean();
                host.hardwareAddress = NetInfo.NOMAC;
                host.hostname = null;
                host.ipAddress = NetInfo.getIpFromLongUnsigned(i);
                host.broadcastIp = null;
                Log.v(TAG, i + " (" + host.ipAddress + ") reaching out");
                try {
                    InetAddress ia = InetAddress.getByName(host.ipAddress);
                    host.hostname = ia.getCanonicalHostName();
                    host.isAlive = ia.isReachable(mTimeout);
                } catch (IOException e) {
                    // not critical for us
                }
                boolean reachable = host.hostname != null && !host.hostname.equals(host.ipAddress);
                if (!reachable) {
                    Process proc = null;
                    Log.d(TAG, "Pinging");
                    try {
                        proc = Runtime.getRuntime().exec("ping -c 1 " + host.ipAddress);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    int retValue = 0;
                    try {
                        retValue = proc.waitFor();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    reachable = retValue == 0;
                }

                if (reachable) {
                    Log.v(TAG, "Reached successfully; " + host.ipAddress);
                    // Is gateway ?
                    if (mGatewayIp.equals(host.ipAddress)) {
                        Log.v(TAG, host.ipAddress + " is a gateway. Skipping it.");
                        publishProgress((HostBean) null);
                        continue;
                    }

                    // Mac address
                    host.hardwareAddress = HardwareAddress.getHardwareAddress(host.ipAddress);
                    if (host.hardwareAddress.equals(NetInfo.NOMAC)) {
                        publishProgress((HostBean) null);
                    } else {
                        Log.d(TAG, i + " (" + host.ipAddress + ") seems to be valid. Publishing it");
                        publishProgress(host);
                    }
                } else {
                    publishProgress((HostBean) null);
                }
            }
        }
    }
}
