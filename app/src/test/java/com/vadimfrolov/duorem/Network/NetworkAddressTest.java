package com.vadimfrolov.duorem.Network;

import org.junit.Test;

import static org.junit.Assert.*;

public class NetworkAddressTest {
    @Test
    public void unsignedIpv4RoundTrips() {
        assertEquals(3232235777L, NetworkAddress.toLong("192.168.1.1"));
        assertEquals(0xffffffffL, NetworkAddress.toLong("255.255.255.255"));
        for (String address : new String[]{"0.0.0.0", "127.0.0.1", "192.168.1.1", "255.255.255.255"}) {
            assertEquals(address, NetworkAddress.fromLong(NetworkAddress.toLong(address)));
        }
    }

    @Test
    public void broadcastUsesActualPrefixIncludingBoundaryPrefixes() {
        assertEquals("192.168.3.255", NetworkAddress.broadcast("192.168.2.15", 23));
        assertEquals("10.255.255.255", NetworkAddress.broadcast("10.1.2.3", 8));
        assertEquals("255.255.255.255", NetworkAddress.broadcast("10.1.2.3", 0));
        assertEquals("192.168.1.11", NetworkAddress.broadcast("192.168.1.10", 31));
        assertEquals("192.168.1.10", NetworkAddress.broadcast("192.168.1.10", 32));
    }

    @Test
    public void hostRangeExcludesNetworkAndBroadcastExceptPointToPoint() {
        assertArrayEquals(new long[]{3232235777L, 3232236030L}, NetworkAddress.hostRange("192.168.1.42", 24));
        assertArrayEquals(new long[]{0, 1}, NetworkAddress.hostRange("0.0.0.0", 31));
        assertArrayEquals(new long[]{0xffffffffL, 0xffffffffL},
                NetworkAddress.hostRange("255.255.255.255", 32));
        assertArrayEquals(new long[]{1, 0xfffffffeL}, NetworkAddress.hostRange("10.1.2.3", 0));
    }

    @Test
    public void rejectsInvalidAddressesAndPrefixes() {
        for (String address : new String[]{null, "", "1.2.3", "1.2.3.4.5", "1.2.3.", "-1.2.3.4",
                "256.2.3.4", "host.local", " 1.2.3.4", "1.2.3.9999999999"}) {
            assertFalse(NetworkAddress.isIpv4(address));
            assertThrows(IllegalArgumentException.class, () -> NetworkAddress.toLong(address));
        }
        assertThrows(IllegalArgumentException.class, () -> NetworkAddress.broadcast("1.2.3.4", -1));
        assertThrows(IllegalArgumentException.class, () -> NetworkAddress.broadcast("1.2.3.4", 33));
    }

    @Test
    public void portsAreBounded() {
        assertEquals(1, NetworkAddress.port("1"));
        assertEquals(65535, NetworkAddress.port("65535"));
        for (String port : new String[]{null, "", "0", "-1", "65536", "9999999999", "22.0"}) {
            assertThrows(IllegalArgumentException.class, () -> NetworkAddress.port(port));
        }
    }
}
