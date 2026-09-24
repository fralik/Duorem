/*
 * Copyright (C) 2009-2010 Aubort Jean-Baptiste (Rorist)
 * Copyright (C) 2017 Vadim Frolov
 * Licensed under GNU's GPL 3 or any later version, see README
 */
package com.vadimfrolov.duorem.Network;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.RouteInfo;

import java.net.Inet4Address;
import java.net.NetworkInterface;
import java.net.SocketException;

public final class NetInfo {
    public static final String NOMAC = "00:00:00:00:00:00";
    public static final String NOIP = "0.0.0.0";

    public boolean isConnected;
    public Network network;
    public NetworkInterface networkInterface;
    public String ip = NOIP;
    public int cidr = 32;
    public String gatewayIp = NOIP;
    public String broadcastIp = NOIP;

    public NetInfo(Context context) {
        ConnectivityManager manager = context.getSystemService(ConnectivityManager.class);
        network = null;
        // A home LAN need not have Internet access or be the default network.
        for (Network candidate : manager.getAllNetworks()) {
            NetworkCapabilities capabilities = manager.getNetworkCapabilities(candidate);
            if (capabilities != null && (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                    || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))
                    && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)) {
                network = candidate;
                break;
            }
        }
        LinkProperties properties = network == null ? null : manager.getLinkProperties(network);
        isConnected = properties != null;
        if (properties == null) {
            return;
        }
        String interfaceName = properties.getInterfaceName();
        if (interfaceName != null) {
            try {
                networkInterface = NetworkInterface.getByName(interfaceName);
            } catch (SocketException e) {
                networkInterface = null;
            }
        }
        for (LinkAddress address : properties.getLinkAddresses()) {
            if (address.getAddress() instanceof Inet4Address) {
                ip = address.getAddress().getHostAddress();
                cidr = address.getPrefixLength();
                broadcastIp = getBroadcastFromIpAndCidr(ip, cidr);
                break;
            }
        }
        for (RouteInfo route : properties.getRoutes()) {
            if (route.isDefaultRoute() && route.getGateway() instanceof Inet4Address) {
                gatewayIp = route.getGateway().getHostAddress();
                break;
            }
        }
    }

    public String identity() {
        return network + "/" + ip + "/" + cidr;
    }

    public static String getIpFromLongUnsigned(long ip) {
        return NetworkAddress.fromLong(ip);
    }

    public static long getUnsignedLongFromIp(String ip) {
        return NetworkAddress.toLong(ip);
    }

    public static String getBroadcastFromIpAndCidr(String ip, int cidr) {
        return NOIP.equals(ip) ? NOIP : NetworkAddress.broadcast(ip, cidr);
    }
}
