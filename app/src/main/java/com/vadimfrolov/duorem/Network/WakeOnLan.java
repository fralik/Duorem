package com.vadimfrolov.duorem.Network;

import java.util.Arrays;

public final class WakeOnLan {
    private WakeOnLan() {}

    public static boolean isValidMac(String mac) {
        return mac != null && mac.matches("(?i)[0-9a-f]{2}(:[0-9a-f]{2}){5}");
    }

    public static byte[] packet(String mac) {
        if (!isValidMac(mac) || NetInfo.NOMAC.equals(mac)) {
            throw new IllegalArgumentException("A non-zero MAC address is required");
        }
        byte[] packet = new byte[102];
        Arrays.fill(packet, 0, 6, (byte) 0xff);
        String[] parts = mac.split(":");
        for (int repeat = 0; repeat < 16; repeat++) {
            for (int octet = 0; octet < 6; octet++) {
                packet[6 + repeat * 6 + octet] = (byte) Integer.parseInt(parts[octet], 16);
            }
        }
        return packet;
    }
}
