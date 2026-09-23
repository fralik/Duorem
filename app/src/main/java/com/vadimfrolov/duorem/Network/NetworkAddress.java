package com.vadimfrolov.duorem.Network;

public final class NetworkAddress {
    private NetworkAddress() {}

    public static long toLong(String address) {
        if (address == null) {
            throw new IllegalArgumentException("An IPv4 address is required");
        }
        String[] parts = address.split("\\.", -1);
        if (parts.length != 4) {
            throw new IllegalArgumentException("Invalid IPv4 address");
        }
        long value = 0;
        for (String part : parts) {
            if (!part.matches("[0-9]{1,3}")) {
                throw new IllegalArgumentException("Invalid IPv4 address");
            }
            int octet = Integer.parseInt(part);
            if (octet > 255) {
                throw new IllegalArgumentException("Invalid IPv4 address");
            }
            value = (value << 8) | octet;
        }
        return value;
    }

    public static boolean isIpv4(String address) {
        try {
            toLong(address);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public static String fromLong(long address) {
        if (address < 0 || address > 0xffffffffL) {
            throw new IllegalArgumentException("IPv4 address is out of range");
        }
        return ((address >>> 24) & 255) + "." + ((address >>> 16) & 255)
                + "." + ((address >>> 8) & 255) + "." + (address & 255);
    }

    private static long hostMask(int prefix) {
        if (prefix < 0 || prefix > 32) {
            throw new IllegalArgumentException("Invalid IPv4 prefix");
        }
        return 0xffffffffL >>> prefix;
    }

    public static String broadcast(String address, int prefix) {
        return fromLong(toLong(address) | hostMask(prefix));
    }

    public static long[] hostRange(String address, int prefix) {
        long hostMask = hostMask(prefix);
        long start = toLong(address) & ~hostMask;
        long end = start | hostMask;
        return prefix < 31 ? new long[]{start + 1, end - 1} : new long[]{start, end};
    }

    public static int port(String text) {
        if (text == null || !text.matches("[0-9]{1,5}")) {
            throw new IllegalArgumentException("Port must be between 1 and 65535");
        }
        int port = Integer.parseInt(text);
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("Port must be between 1 and 65535");
        }
        return port;
    }
}
