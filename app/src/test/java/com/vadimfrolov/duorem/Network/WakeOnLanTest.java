package com.vadimfrolov.duorem.Network;

import android.net.Network;

import org.junit.Test;
import org.mockito.MockMakers;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Arrays;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class WakeOnLanTest {
    @Test
    public void magicPacketHasSixSyncBytesAndSixteenMacCopies() {
        byte[] packet = WakeOnLan.packet("01:23:45:67:89:ab");
        assertEquals(102, packet.length);
        for (int i = 0; i < 6; i++) assertEquals((byte) 0xff, packet[i]);
        byte[] mac = new byte[]{1, 0x23, 0x45, 0x67, (byte) 0x89, (byte) 0xab};
        for (int repeat = 0; repeat < 16; repeat++) {
            assertArrayEquals(mac, Arrays.copyOfRange(packet, 6 + repeat * 6, 12 + repeat * 6));
        }
    }

    @Test
    public void invalidOrUnsetMacCannotBeSent() {
        for (String mac : new String[]{null, "", "00:00:00:00:00:00", "AA:BB", "GG:BB:CC:DD:EE:FF",
                "1:23:45:67:89:AB", "01:23:45:67:89:AB:CD"}) {
            assertThrows(IllegalArgumentException.class, () -> WakeOnLan.packet(mac));
        }
    }

    @Test
    public void wakeOnlyNeedsMacBroadcastAndWolPortNotSshOrDns() throws Exception {
        try (DatagramSocket receiver = new DatagramSocket(0, InetAddress.getByName("127.0.0.1"));
             RemoteClient client = new RemoteClient(null)) {
            receiver.setSoTimeout(2000);
            HostBean host = new HostBean();
            host.hardwareAddress = "01:23:45:67:89:AB";
            host.hostname = "invalid host should never be resolved";
            host.ipAddress = NetInfo.NOIP;
            host.sshPort = "invalid SSH port should never be read";
            host.wolPort = String.valueOf(receiver.getLocalPort());
            host.broadcastIp = "127.0.0.1";
            RemoteCommand command = new RemoteCommand(host, RemoteCommand.WOL);
            client.wake(command, NetInfo.NOIP);
            DatagramPacket packet = new DatagramPacket(new byte[128], 128);
            receiver.receive(packet);
            assertTrue(command.success);
            assertEquals(102, packet.getLength());
            assertArrayEquals(WakeOnLan.packet(host.hardwareAddress), Arrays.copyOf(packet.getData(), 102));
        }
    }

    @Test
    public void wakeUsesSystemRoutingWhenNetworkBindingIsDenied() throws Exception {
        Network network = mock(Network.class, withSettings().mockMaker(MockMakers.SUBCLASS));
        doThrow(new SocketException("Binding socket to network: EPERM (Operation not permitted)"))
                .when(network).bindSocket(any(DatagramSocket.class));
        try (DatagramSocket receiver = new DatagramSocket(0, InetAddress.getByName("127.0.0.1"));
             RemoteClient client = new RemoteClient(network)) {
            receiver.setSoTimeout(2000);
            HostBean host = new HostBean();
            host.hardwareAddress = "01:23:45:67:89:AB";
            host.wolPort = String.valueOf(receiver.getLocalPort());
            for (String broadcast : new String[]{null, "", NetInfo.NOIP}) {
                host.broadcastIp = broadcast;
                RemoteCommand command = new RemoteCommand(host, RemoteCommand.WOL);
                client.wake(command, "127.0.0.1");
                DatagramPacket packet = new DatagramPacket(new byte[128], 128);
                receiver.receive(packet);
                assertTrue(command.success);
                assertEquals(102, packet.getLength());
                assertArrayEquals(WakeOnLan.packet(host.hardwareAddress),
                        Arrays.copyOf(packet.getData(), packet.getLength()));
            }
        }
        verifyNoInteractions(network);
    }

    @Test
    public void invalidBroadcastFailsInsteadOfReportingSuccess() {
        try (RemoteClient client = new RemoteClient(null)) {
            for (String broadcast : new String[]{null, "", NetInfo.NOIP, "not-an-ip", "256.0.0.1"}) {
                RemoteCommand command = new RemoteCommand(new HostBean(), RemoteCommand.WOL);
                assertThrows(java.io.IOException.class, () -> client.wake(command, broadcast));
                assertFalse(command.success);
            }
        }
    }

    @Test
    public void invalidWolPortFailsInsteadOfReportingSuccess() {
        try (RemoteClient client = new RemoteClient(null)) {
            HostBean host = new HostBean();
            host.hardwareAddress = "01:23:45:67:89:AB";
            for (String port : new String[]{null, "", "0", "65536", "invalid"}) {
                host.wolPort = port;
                RemoteCommand command = new RemoteCommand(host, RemoteCommand.WOL);
                assertThrows(IllegalArgumentException.class, () -> client.wake(command, "127.0.0.1"));
                assertFalse(command.success);
            }
        }
    }
}
