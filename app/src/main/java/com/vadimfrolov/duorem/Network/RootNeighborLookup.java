package com.vadimfrolov.duorem.Network;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class RootNeighborLookup implements AutoCloseable {
    private static final String TAG = "RootNeighborLookup";
    private static final int INITIAL_TIMEOUT_SECONDS = 15;
    private static final int LOOKUP_TIMEOUT_SECONDS = 3;
    private Boolean rootAvailable;
    private volatile Process activeProcess;
    private volatile boolean closed;

    public synchronized String query(String ip) {
        if (closed || !NetworkAddress.isIpv4(ip) || Boolean.FALSE.equals(rootAvailable)) {
            return NetInfo.NOMAC;
        }

        String command = commandFor(ip);
        Process process = null;
        try {
            process = new ProcessBuilder("su", "-c", command)
                    .redirectErrorStream(true)
                    .start();
            activeProcess = process;
            int timeout = rootAvailable == null ? INITIAL_TIMEOUT_SECONDS : LOOKUP_TIMEOUT_SECONDS;
            if (!process.waitFor(timeout, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                rootAvailable = false;
                Log.w(TAG, "Root neighbor lookup timed out");
                return NetInfo.NOMAC;
            }

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) output.append(line).append('\n');
            }
            rootAvailable = process.exitValue() == 0;
            return rootAvailable ? parse(output.toString(), ip) : NetInfo.NOMAC;
        } catch (IOException e) {
            rootAvailable = false;
            Log.w(TAG, "Root access is unavailable", e);
            return NetInfo.NOMAC;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return NetInfo.NOMAC;
        } finally {
            activeProcess = null;
            closeProcessStreams(process);
        }
    }

    @Override
    public void close() {
        closed = true;
        Process process = activeProcess;
        if (process != null) process.destroyForcibly();
    }

    public boolean isUnavailable() {
        return Boolean.FALSE.equals(rootAvailable);
    }

    static String parse(String output, String ip) {
        if (output == null || !NetworkAddress.isIpv4(ip)) return NetInfo.NOMAC;
        Pattern pattern = Pattern.compile("(?im)^" + Pattern.quote(ip)
                + "\\s+.*?\\b([0-9a-f]{2}(?::[0-9a-f]{2}){5})\\b");
        Matcher matcher = pattern.matcher(output);
        if (!matcher.find()) return NetInfo.NOMAC;

        String mac = matcher.group(1).toUpperCase(Locale.ROOT);
        return NetInfo.NOMAC.equals(mac) || "FF:FF:FF:FF:FF:FF".equals(mac)
                ? NetInfo.NOMAC : mac;
    }

    static String commandFor(String ip) {
        return "{ ip neigh show " + ip + " 2>/dev/null || true; "
                + "awk '$1 == \"" + ip
                + "\" { print; exit }' /proc/net/arp 2>/dev/null || true; }";
    }

    private static void closeProcessStreams(Process process) {
        if (process == null) return;
        try {
            process.getInputStream().close();
        } catch (IOException e) {
            Log.d(TAG, "Could not close root command output", e);
        }
        try {
            process.getErrorStream().close();
        } catch (IOException e) {
            Log.d(TAG, "Could not close root command error stream", e);
        }
        try {
            process.getOutputStream().close();
        } catch (IOException e) {
            Log.d(TAG, "Could not close root command input", e);
        }
    }
}
