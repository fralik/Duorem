/*
 * Copyright (C) 2009-2010 Aubort Jean-Baptiste (Rorist)
 * Copyright (C) 2017 Vadim Frolov
 * Licensed under GNU's GPL 3 or any later version, see README
 */
package com.vadimfrolov.duorem;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.vadimfrolov.duorem.Network.HardwareAddress;
import com.vadimfrolov.duorem.Network.HostBean;
import com.vadimfrolov.duorem.Network.NetInfo;
import com.vadimfrolov.duorem.Network.NetworkAddress;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public final class DnsDiscovery implements AutoCloseable {
    private static final int WORKERS = 10;
    private final ExecutorService workers = Executors.newFixedThreadPool(WORKERS);
    private final Set<Socket> sockets = ConcurrentHashMap.newKeySet();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final DiscoveryListener listener;
    private final NetInfo network;
    private volatile boolean cancelled;

    public DnsDiscovery(DiscoveryListener listener, NetInfo network) {
        this.listener = listener;
        this.network = network;
    }

    public void start() {
        long[] range = NetworkAddress.hostRange(network.ip, network.cidr);
        long total = range[1] - range[0] + 1;
        AtomicLong next = new AtomicLong(range[0]);
        AtomicLong completed = new AtomicLong();
        AtomicInteger active = new AtomicInteger(WORKERS);
        listener.onStartDiscovering();
        for (int i = 0; i < WORKERS; i++) {
            workers.execute(() -> {
                try {
                    long address;
                    while (!cancelled && (address = next.getAndIncrement()) <= range[1]) {
                        HostBean host = probe(NetworkAddress.fromLong(address));
                        int progress = (int) (completed.incrementAndGet() * 10000 / total);
                        handler.post(() -> {
                            if (!cancelled) {
                                if (host != null) listener.onNewHost(host);
                                listener.setDiscoverProgress(progress);
                            }
                        });
                    }
                } catch (SecurityException e) {
                    Log.e("DnsDiscovery", "Local network access was denied", e);
                    handler.post(() -> {
                        if (!cancelled) listener.onDiscoveryError();
                    });
                } finally {
                    if (active.decrementAndGet() == 0) {
                        handler.post(() -> {
                            if (!cancelled) listener.onStopDiscovering();
                        });
                    }
                }
            });
        }
        workers.shutdown();
    }

    private HostBean probe(String ip) {
        if (ip.equals(network.ip) || ip.equals(network.gatewayIp)) return null;
        try {
            InetAddress address = network.network.getByName(ip);
            boolean reachable = false;
            Socket socket = new Socket();
            sockets.add(socket);
            try (socket) {
                network.network.bindSocket(socket);
                if (cancelled) return null;
                socket.connect(new InetSocketAddress(address, 22), listener.getTimeout());
                reachable = true;
            } catch (IOException e) {
                // Not every reachable device runs SSH.
                if (!cancelled) reachable = address.isReachable(listener.getTimeout());
            } finally {
                sockets.remove(socket);
            }
            if (!reachable || cancelled) return null;
            HostBean host = new HostBean();
            host.ipAddress = ip;
            host.hostname = address.getCanonicalHostName();
            host.hardwareAddress = HardwareAddress.getHardwareAddress(ip);
            host.broadcastIp = network.broadcastIp;
            host.isAlive = true;
            return host;
        } catch (IOException e) {
            Log.d("DnsDiscovery", "Probe failed for " + ip);
            return null;
        }
    }

    @Override
    public void close() {
        cancelled = true;
        workers.shutdownNow();
        handler.removeCallbacksAndMessages(null);
        for (Socket socket : sockets) {
            try {
                socket.close();
            } catch (IOException e) {
                Log.w("DnsDiscovery", "Could not close discovery socket", e);
            }
        }
        sockets.clear();
    }
}
