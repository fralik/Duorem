package com.vadimfrolov.duorem.Network;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class RootNeighborLookupTest {
    @Test
    public void parsesIpNeighborOutput() {
        String output = "192.168.1.2 dev wlan0 lladdr aa:bb:cc:dd:ee:ff STALE\n";
        assertEquals("AA:BB:CC:DD:EE:FF", RootNeighborLookup.parse(output, "192.168.1.2"));
    }

    @Test
    public void parsesProcArpOutput() {
        String output = "IP address HW type Flags HW address Mask Device\n"
                + "192.168.1.3 0x1 0x2 10:20:30:40:50:60 * wlan0\n";
        assertEquals("10:20:30:40:50:60", RootNeighborLookup.parse(output, "192.168.1.3"));
    }

    @Test
    public void ignoresOtherHostsAndInvalidMacs() {
        assertEquals(NetInfo.NOMAC, RootNeighborLookup.parse(
                "192.168.1.20 dev wlan0 lladdr aa:bb:cc:dd:ee:ff REACHABLE",
                "192.168.1.2"));
        assertEquals(NetInfo.NOMAC, RootNeighborLookup.parse(
                "192.168.1.2 dev wlan0 lladdr 00:00:00:00:00:00 FAILED",
                "192.168.1.2"));
        assertEquals(NetInfo.NOMAC, RootNeighborLookup.parse("", "not-an-ip"));
    }
}
