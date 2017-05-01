/*
 * Copyright (C) 2009-2010 Aubort Jean-Baptiste (Rorist)
 * Copyright (C) 2017 Vadim Frolov
 * Licensed under GNU's GPL 3 or any later version, see README
 */

package com.vadimfrolov.twobuttonremote.Network;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provides information about network connection.
 * The class is based on NetInfo class from {@link https://github.com/rorist/android-network-discovery|Android Network Discovery} app.
 * Changes:
 * 1. Left only things that are relevant to TwoButtonRemote app.
 * 2. Added more comments.
 */

public class NetInfo {
    /** Invalid or unset MAC address */
    public static final String NOMAC = "00:00:00:00:00:00";
    /** Invalid or unset IPv4 address */
    public static final String NOIP = "0.0.0.0";
    /** Invalid or unset network mask */
    public static final String NOMASK = "255.255.255.255";

    /** Show current network connection status */
    public boolean isConnected = false;

    /** Network interface that was or is currently active */
    public String intf = "eth0";
    /** Last known (or current) IPv4 address */
    public String ip = NOIP;
    /** Last known (or current) CIDR block */
    public int cidr = 24;
    /** Last known (or current) network mask */
    public String netmaskIp = NOMASK;
    /** Last known (or current) IPv4 gateway address */
    public String gatewayIp = NOIP;
    /** Last known (or current) MAC address */
    public String macAddress = NOMAC;
    /** Last known (or current) IPv4 broadcast address */
    public String broadcastIp = NOIP;

    private static final int BUF = 8 * 1024;
    private static final String CMD_IP = " -f inet addr show %s";
    private static final String PTN_IP1 = "\\s*inet [0-9\\.]+\\/([0-9]+) brd [0-9\\.]+ scope global %s$";
    private static final String PTN_IP2 = "\\s*inet [0-9\\.]+ peer [0-9\\.]+\\/([0-9]+) scope global %s$"; // FIXME: Merge with PTN_IP1
    private static final String PTN_IF = "^%s: ip [0-9\\.]+ mask ([0-9\\.]+) flags.*";

    private Context mContext;

    public NetInfo(final Context context) {
        mContext = context;

        ConnectivityManager cm = (ConnectivityManager)mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        boolean isConnected = ni != null && ni.isConnectedOrConnecting();
        if (isConnected) {
            obtainIp();
            getWifiInfo();
        }
    }

    /** Constructs string representation of IP address from an unsigned long */
    public static String getIpFromLongUnsigned(long ip_long) {
        String ip = "";
        for (int k = 3; k > -1; k--) {
            ip = ip + ((ip_long >> k * 8) & 0xFF) + ".";
        }
        return ip.substring(0, ip.length() - 1);
    }

    /** Convert string representation of IP address to long */
    public static long getUnsignedLongFromIp(String ip_addr) {
        String[] a = ip_addr.split("\\.");
        return (Integer.parseInt(a[0]) * 16777216 + Integer.parseInt(a[1]) * 65536
                + Integer.parseInt(a[2]) * 256 + Integer.parseInt(a[3]));
    }

    /** Convert int representation of IP address to string */
    public static String getIpFromIntSigned(int ip_int) {
        String ip = "";
        for (int k = 0; k < 4; k++) {
            ip = ip + ((ip_int >> k * 8) & 0xFF) + ".";
        }
        return ip.substring(0, ip.length() - 1);
    }

    public static String getBroadcastFromIpAndCidr(String ip, int cidr) {
        if (ip.equals(NOIP))
            return NOIP;

        long networkIp = getUnsignedLongFromIp(ip);
        long networkStart = 0;
        long networkEnd = 0;

        // Detected IP
        int shift = (32 - cidr);
        if (cidr < 31) {
            networkStart = (networkIp >> shift << shift) + 1;
            networkEnd = (networkStart | ((1 << shift) - 1)) - 1;
        } else {
            networkStart = (networkIp >> shift << shift);
            networkEnd = (networkStart | ((1 << shift) - 1));
        }

        return getIpFromLongUnsigned(networkEnd + 1);
    }

    public void obtainIp() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface ni = en.nextElement();
                intf = ni.getName();
                ip = getInterfaceFirstIp(ni);
                if (!ip.equals(NOIP)) {
                    broadcastIp = getInterfaceBroadcastIp(ni);
                    break;
                }
            }
        } catch (SocketException e) {
            // use default value of IP address
        }
        getCidr();

        if (broadcastIp == null || broadcastIp.equals(NOIP)) {
            broadcastIp = NetInfo.getBroadcastFromIpAndCidr(ip, cidr);
        }
    }

    public boolean getWifiInfo() {
        WifiManager wifi = (WifiManager) mContext.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifi != null) {
            WifiInfo mInfo = wifi.getConnectionInfo();
            macAddress = mInfo.getMacAddress();
            DhcpInfo dhcp = wifi.getDhcpInfo();
            gatewayIp = getIpFromIntSigned(dhcp.gateway);

            //broadcastIp = getIpFromIntSigned((dhcp.ipAddress & dhcp.netmask) | ~dhcp.netmask);
            netmaskIp = getIpFromIntSigned(wifi.getDhcpInfo().netmask);
            return true;
        }
        return false;
    }

    private void getCidr() {
        if (!netmaskIp.equals(NOMASK)) {
            cidr = IpToCidr(netmaskIp);
        } else {
            String match;
            // Running ip tools
            try {
                if ((match = runCommand("/system/xbin/ip", String.format(CMD_IP, intf), String.format(PTN_IP1, intf))) != null) {
                    cidr = Integer.parseInt(match);
                } else if ((match = runCommand("/system/xbin/ip", String.format(CMD_IP, intf), String.format(PTN_IP2, intf))) != null) {
                    cidr = Integer.parseInt(match);
                } else if ((match = runCommand("/system/bin/ifconfig", " " + intf, String.format(PTN_IF, intf))) != null) {
                    cidr = IpToCidr(match);
                } else {
                    // We are going to use default value /24
                }
            } catch (NumberFormatException e) {
                // This is fine, we are going to use default value /24
            }
        }
    }

    // FIXME: Factorize, this isn't a generic runCommand()
    private String runCommand(String path, String cmd, String ptn) {
        try {
            if (new File(path).exists()) {
                String line;
                Matcher matcher;
                Pattern ptrn = Pattern.compile(ptn);
                Process p = Runtime.getRuntime().exec(path + cmd);
                BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()), BUF);
                while ((line = r.readLine()) != null) {
                    matcher = ptrn.matcher(line);
                    if (matcher.matches()) {
                        return matcher.group(1);
                    }
                }
            }
        } catch (Exception e) {
//            Log.e(TAG, "Can't use native command: " + e.getMessage());
            return null;
        }
        return null;
    }

    private String getInterfaceFirstIp(NetworkInterface ni) {
        if (ni != null) {
            for (Enumeration<InetAddress> nis = ni.getInetAddresses(); nis.hasMoreElements();) {
                InetAddress ia = nis.nextElement();
                if (!ia.isLoopbackAddress()) {
                    if (ia instanceof Inet6Address) {
                        // IPv6 detected and not supported yet!
                        continue;
                    }
                    return ia.getHostAddress();
                }
            }
        }
        return NOIP;
    }

    private String getInterfaceBroadcastIp(NetworkInterface ni) {
        try {
            if (ni != null && !ni.isLoopback()) {
                for (InterfaceAddress interfaceAddress : ni.getInterfaceAddresses()) {
                    InetAddress ia = interfaceAddress.getBroadcast();
                    if (ia != null) {
                        return ia.toString().substring(1);
                    }
                }
            }
        } catch (Exception e) {
            // Prevent crash, return NOIP
        }

        return NOIP;
    }

    private int IpToCidr(String ip) {
        double sum = -2;
        String[] part = ip.split("\\.");
        for (String p : part) {
            sum += 256D - Double.parseDouble(p);
        }
        return 32 - (int) (Math.log(sum) / Math.log(2d));
    }
}
