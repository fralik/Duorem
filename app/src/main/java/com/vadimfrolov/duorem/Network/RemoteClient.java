/*
 * Copyright (C) 2017 Vadim Frolov
 * Licensed under GNU's GPL 3 or any later version, see README
 */
package com.vadimfrolov.duorem.Network;

import android.net.Network;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.vadimfrolov.duorem.HostKeyStore;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;

public final class RemoteClient implements AutoCloseable {
    private static final int CONNECT_TIMEOUT_MS = 3000;
    private static final int COMMAND_TIMEOUT_MS = 30000;
    private final Network network;
    private final NetworkInterface networkInterface;
    private volatile boolean cancelled;
    private volatile Socket activeSocket;
    private volatile Session activeSession;
    private volatile DatagramSocket activeDatagram;

    public RemoteClient(Network network) {
        this(network, null);
    }

    public RemoteClient(Network network, NetworkInterface networkInterface) {
        this.network = network;
        this.networkInterface = networkInterface;
    }

    public static final class ProbeResult {
        public final boolean online;
        public final boolean sshConfigured;
        public final boolean sshAvailable;

        public ProbeResult(boolean online, boolean sshConfigured, boolean sshAvailable) {
            this.online = online;
            this.sshConfigured = sshConfigured;
            this.sshAvailable = sshAvailable;
        }
    }

    private void checkCancelled() {
        if (cancelled || Thread.currentThread().isInterrupted()) {
            throw new CancellationException("Operation cancelled");
        }
    }

    private InetAddress resolve(String host) throws IOException {
        return network == null ? InetAddress.getByName(host) : network.getByName(host);
    }

    private Socket connect(String host, int port) throws IOException {
        checkCancelled();
        Socket socket = new Socket();
        activeSocket = socket;
        try {
            if (network != null) network.bindSocket(socket);
            socket.connect(new InetSocketAddress(resolve(host), port), CONNECT_TIMEOUT_MS);
            checkCancelled();
            return socket;
        } catch (IOException | CancellationException e) {
            socket.close();
            throw e;
        }
    }

    private String reachableAddress(HostBean target) throws IOException {
        IOException failure = new IOException("No target address is configured");
        for (String address : candidateAddresses(target)) {
            try (Socket socket = connect(address, NetworkAddress.port(target.sshPort))) {
                return address;
            } catch (IOException e) {
                failure = e;
            }
        }
        throw failure;
    }

    public boolean isReachable(HostBean target) throws IOException {
        reachableAddress(target);
        return true;
    }

    public ProbeResult probe(HostBean target, int timeout) {
        checkCancelled();
        boolean sshConfigured = target.canUseSsh();
        boolean sshAvailable = false;
        if (sshConfigured) {
            try {
                sshAvailable = isReachable(target);
            } catch (IOException | IllegalArgumentException e) {
                // Reachability is checked separately when SSH is unavailable.
            }
        }
        if (sshAvailable) return new ProbeResult(true, true, true);

        boolean online = false;
        for (String candidate : candidateAddresses(target)) {
            try {
                checkCancelled();
                InetAddress address = resolve(candidate);
                online = networkInterface != null
                        ? address.isReachable(networkInterface, 0, timeout)
                        : network == null && address.isReachable(timeout);
                if (online) break;
            } catch (IOException | SecurityException e) {
                // Try another configured address before reporting the device unreachable.
            }
        }
        checkCancelled();
        return new ProbeResult(online, sshConfigured, false);
    }

    private static List<String> candidateAddresses(HostBean target) {
        List<String> candidates = new ArrayList<>();
        if (target.ipAddress != null && !target.ipAddress.isEmpty()
                && !NetInfo.NOIP.equals(target.ipAddress)) {
            candidates.add(target.ipAddress);
        }
        if (target.hostname != null && !target.hostname.trim().isEmpty()
                && !candidates.contains(target.hostname)) {
            candidates.add(target.hostname);
        }
        return candidates;
    }

    public void wake(RemoteCommand command, String currentBroadcast) throws IOException {
        checkCancelled();
        String broadcast = command.target.broadcastIp;
        if (broadcast == null || broadcast.isEmpty() || NetInfo.NOIP.equals(broadcast)) {
            broadcast = currentBroadcast;
        }
        if (!NetworkAddress.isIpv4(broadcast) || NetInfo.NOIP.equals(broadcast)) {
            throw new IOException("No usable IPv4 broadcast address; configure one in device settings");
        }
        byte[] bytes = WakeOnLan.packet(command.target.hardwareAddress);
        try (DatagramSocket socket = new DatagramSocket()) {
            activeDatagram = socket;
            if (network != null) network.bindSocket(socket);
            socket.setBroadcast(true);
            checkCancelled();
            socket.send(new DatagramPacket(bytes, bytes.length, resolve(broadcast), command.wolPort()));
        } finally {
            activeDatagram = null;
        }
        command.success = true;
    }

    public void execute(RemoteCommand command, HostKeyStore hostKeys)
            throws IOException, JSchException, InterruptedException {
        String address = reachableAddress(command.target);
        JSch jsch = new JSch();
        jsch.setHostKeyRepository(hostKeys);
        Session session = jsch.getSession(command.target.sshUsername, address, command.sshPort());
        activeSession = session;
        session.setConfig("StrictHostKeyChecking", "yes");
        session.setConfig("PreferredAuthentications", "password,keyboard-interactive");
        session.setPassword(command.target.sshPassword);
        session.setSocketFactory(new com.jcraft.jsch.SocketFactory() {
            @Override public Socket createSocket(String host, int port) throws IOException { return connect(host, port); }
            @Override public InputStream getInputStream(Socket socket) throws IOException { return socket.getInputStream(); }
            @Override public OutputStream getOutputStream(Socket socket) throws IOException { return socket.getOutputStream(); }
        });
        ChannelExec channel = null;
        try {
            checkCancelled();
            session.connect(CONNECT_TIMEOUT_MS);
            session.setTimeout(COMMAND_TIMEOUT_MS);
            channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(command.command);
            channel.setPty(true);
            // Do not let the PTY echo the sudo password back into command output.
            channel.setTerminalMode(new byte[]{53, 0, 0, 0, 0, 0});
            InputStream input = channel.getInputStream();
            OutputStream output = channel.getOutputStream();
            checkCancelled();
            channel.connect(CONNECT_TIMEOUT_MS);
            if (command.command.contains("sudo")) {
                output.write((command.target.sshPassword + "\n").getBytes(StandardCharsets.UTF_8));
                output.flush();
            }
            long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(COMMAND_TIMEOUT_MS);
            byte[] buffer = new byte[1024];
            while (true) {
                checkCancelled();
                if (System.nanoTime() >= deadline) {
                    throw new SocketTimeoutException("Remote command timed out; its outcome is unknown");
                }
                while (input.available() > 0) {
                    if (input.read(buffer) < 0) break;
                    checkCancelled();
                    if (System.nanoTime() >= deadline) {
                        throw new SocketTimeoutException("Remote command timed out; its outcome is unknown");
                    }
                }
                if (channel.isClosed()) {
                    int status = channel.getExitStatus();
                    if (status != 0) {
                        throw new IOException(status < 0
                                ? "Connection closed without exit status; command outcome is unknown"
                                : "Remote command failed (exit status " + status + ")");
                    }
                    command.success = true;
                    return;
                }
                Thread.sleep(100);
            }
        } finally {
            if (channel != null) channel.disconnect();
            session.disconnect();
            activeSession = null;
        }
    }

    @Override
    public void close() {
        cancelled = true;
        Session session = activeSession;
        if (session != null) session.disconnect();
        DatagramSocket datagram = activeDatagram;
        if (datagram != null) datagram.close();
        Socket socket = activeSocket;
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                android.util.Log.w("RemoteClient", "Could not close socket", e);
            }
        }
    }
}
