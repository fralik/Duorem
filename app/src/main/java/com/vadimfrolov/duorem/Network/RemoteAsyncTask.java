/*
 * Copyright (C) 2017 Vadim Frolov
 * Licensed under GNU's GPL 3, see README
 */
package com.vadimfrolov.duorem.Network;

import android.os.AsyncTask;
import android.util.Log;

import com.jcraft.jsch.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Properties;

/**
 * Task to communicate with remote target.
 */
public class RemoteAsyncTask extends AsyncTask<RemoteCommand, RemoteCommand, RemoteCommand> {

    private final String TAG = "RemoteAsyncTask";
    private RemoteCommandResult mDelegate = null;
    public volatile RemoteCommand cmd = null;

    public RemoteAsyncTask(RemoteCommandResult handler) {
        super();
        mDelegate = handler;
    }

    @Override
    protected void onProgressUpdate(RemoteCommand... items) {
        mDelegate.getPassphrase(items[0], items[1].passPhrase);
    }

    @Override
    protected RemoteCommand doInBackground(RemoteCommand... params) {
        if (params.length == 0) {
            Log.d(TAG, "No parameters were passed to the task. Has nothing to do.");
            return null;
        }
        cmd = params[0];
        String result = "";
        String address = decideIpOrHost(cmd);
        try {
            switch (cmd.commandType) {
                case RemoteCommand.WOL:
                    result = sendWolPacket(cmd.target.hardwareAddress, cmd.target.broadcastIp, cmd.wolPort());
                    break;

                case RemoteCommand.SSH:
                    result = executeRemoteCommand(cmd);
                    break;

                case RemoteCommand.PING:
                    Socket socket = new Socket(address, cmd.sshPort());
                    result = "success";
                    break;

                default:
                    break;
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }

        cmd.result = result;
        return cmd;
    }

    @Override
    protected void onPostExecute(RemoteCommand result) {
        if (mDelegate != null)
            mDelegate.onRemoteCommandFinished(result);
    }

    /**
     * Decide whether to use IP or hostname for connection.
     * This function tries to connect to remote host via IP address. If this fails,
     * it then tries the hostname. This is useful in case only one of either IP or hostname
     * is valid.
     *
     * @param cmd A remote command instance
     * @return Either IP address or hostname. If both fails, IP address is returned.
     */
    private String decideIpOrHost(RemoteCommand cmd) {
        boolean ipIsValid = false;
        boolean hostIsValid = false;
        try {
            Socket socket = new Socket(cmd.target.ipAddress, cmd.sshPort());
            ipIsValid = true;
            socket.close();
        } catch (Exception e) {
            ipIsValid = false;
        }

        try {
            Socket socket = new Socket(cmd.target.hostname, cmd.sshPort());
            hostIsValid = true;
            socket.close();
        } catch (Exception e) {
            hostIsValid = false;
        }

        if (!ipIsValid && hostIsValid) {
            return cmd.target.hostname;
        }

        return cmd.target.ipAddress;
    }

    private String sendWolPacket(String mac, String broadcastIp, int wolPort) throws UnknownHostException, SocketException, IOException, IllegalArgumentException {
        if (broadcastIp == null || broadcastIp.equals(NetInfo.NOIP)) {
            return "invalid gateway";
        }

        final String[] hex = mac.split(":");

        final byte[] bytes = new byte[102];

        // convert to base16 bytes
        final byte[] macBytes = new byte[6];
        for (int i = 0; i < 6; i++) {
            macBytes[i] = (byte) Integer.parseInt(hex[i], 16);
            // fill in the first 6 bytes
            bytes[i] = (byte) 0xff;
        }

        // fill remaining bytes with target MAC
        for (int i = 6; i < bytes.length; i += macBytes.length) {
            System.arraycopy(macBytes, 0, bytes, i, macBytes.length);
        }

        // create socket to IP
        final InetAddress address = InetAddress.getByName(broadcastIp);
        final DatagramPacket packet = new DatagramPacket(bytes, bytes.length, address, wolPort);
        final DatagramSocket socket = new DatagramSocket();
        socket.send(packet);
        socket.close();

        return "success";
    }

    private String executeRemoteCommand(RemoteCommand cmd)
            throws Exception {
        String hostname = decideIpOrHost(cmd);
        int port = cmd.sshPort();
        String username = cmd.target.sshUsername;
        String password = cmd.target.sshPassword;
        String command = cmd.command;
        String keyFile = cmd.sshKeyFile;

        if (isCancelled()) {
            return "";
        }

        JSch jsch = new JSch();
        boolean usePassword = false;
        try {
            jsch.addIdentity(keyFile);
            IdentityRepository repo = jsch.getIdentityRepository();
            Identity id = (Identity) repo.getIdentities().firstElement();
            Log.d(TAG, "Identity " + id.getName() + " is encrypted: " + id.isEncrypted());
            if (id.isEncrypted()) {
                if (mDelegate != null) {
                    RemoteCommand cmd1 = new RemoteCommand(new HostBean());

//                    final String[] split = id.getName().split("/");//split the path.
                    cmd1.passPhrase = cmd.target.sshKeyName();
                    publishProgress(cmd, cmd1);
                    while (cmd.passPhrase == null) {
                        Thread.sleep(1000);
                    }
                    id.setPassphrase(cmd.passPhrase.getBytes());
                    if (id.isEncrypted()) {
                        // passphrase failed
                        return "";
                    }
                }
            }
            Log.d(TAG, "Identity " + id.getName() + " is encrypted: " + id.isEncrypted());
        } catch (JSchException e) {
            usePassword = true;
        }
        Session session = jsch.getSession(username, hostname, port);
        if (usePassword) {
            session.setPassword(password);
        }
        session.setConfig("PreferredAuthentications", "publickey,password");

        // Avoid asking for key confirmation
        Properties prop = new Properties();
        prop.put("StrictHostKeyChecking", "no");
        session.setConfig(prop);

        if (isCancelled()) {
            return "";
        }
        session.connect();

        // SSH Channel
        ChannelExec channel = (ChannelExec)session.openChannel("exec");

        // Execute command
        // man sudo
        //   -S  The -S (stdin) option causes sudo to read the password from the
        //       standard input instead of the terminal device.
        //   -p  The -p (prompt) option allows you to override the default
        //       password prompt and use a custom one.
        //channel.setCommand("sudo -S -p '' " + command);
        channel.setCommand(command);
        InputStream in = channel.getInputStream();
        OutputStream out = channel.getOutputStream();

        if (isCancelled()) {
            session.disconnect();
            return "";
        }

        channel.setPty(true);
        channel.connect();

        if (command.contains("sudo")) {
            out.write((password + "\n").getBytes());
            out.flush();
        }

        String result = "";

        byte[] tmp = new byte[1024];
        while (true) {
            while (in.available() > 0 && !isCancelled()) {
                int i = in.read(tmp, 0, 1024);
                if (i < 0) {
                    break;
                }
                result = result + new String(tmp, 0, i);
            }
            if (channel.isClosed() || isCancelled()) {
                break;
            }
            try {
                Thread.sleep(1000);
            } catch (Exception ee) {
                // not critical for us
            }
        }

        channel.disconnect();
        session.disconnect();

        return result;
    }
}
