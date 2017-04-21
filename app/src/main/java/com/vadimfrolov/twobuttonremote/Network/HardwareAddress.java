/*
 * Copyright (C) 2009-2010 Aubort Jean-Baptiste (Rorist)
 * Licensed under GNU's GPL 2, see README
 */

package com.vadimfrolov.twobuttonremote.Network;

import android.util.Log;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The class is from {@link https://github.com/rorist/android-network-discovery|Android Network Discovery} app.
 * Obtains MAC address from an IPv4 address.
 * Changes:
 * 1. Made class final.
 * 2. Left only one function getHardwareAddress.
 */

public final class HardwareAddress {
    private final static String TAG = "HardwareAddress";

    // 0x1 is HW Type:  Ethernet (10Mb) [JBP]
    // 0x2 is ARP Flag: completed entry (ha valid)
    private final static String MAC_RE = "^%s\\s+0x1\\s+0x2\\s+([:0-9a-fA-F]+)\\s+\\*\\s+\\w+$";
    private final static int BUF = 8 * 1024;

    public static String getHardwareAddress(String ip) {
        String hw = NetInfo.NOMAC;
        BufferedReader bufferedReader = null;
        if (ip == null) {
            return hw;
        }

        try {
            String ptrn = String.format(MAC_RE, ip.replace(".", "\\."));
            Pattern pattern = Pattern.compile(ptrn);
            bufferedReader = new BufferedReader(new FileReader("/proc/net/arp"), BUF);
            String line;
            Matcher matcher;
            while ((line = bufferedReader.readLine()) != null) {
                matcher = pattern.matcher(line);
                if (matcher.matches()) {
                    hw = matcher.group(1);
                    break;
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Can't open/read file ARP: " + e.getMessage());
            return hw;
        } finally {
            try {
                if(bufferedReader != null) {
                    bufferedReader.close();
                }
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            }
        }

        return hw;
    }
}
