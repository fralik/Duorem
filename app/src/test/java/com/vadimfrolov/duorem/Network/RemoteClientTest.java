package com.vadimfrolov.duorem.Network;

import org.junit.Test;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CancellationException;

import static org.junit.Assert.*;

public class RemoteClientTest {
    @Test
    public void pollingClosesItsSocketAndUsesConfiguredPort() throws Exception {
        try (ServerSocket server = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"));
             RemoteClient client = new RemoteClient(null)) {
            server.setSoTimeout(2000);
            HostBean host = new HostBean();
            host.ipAddress = "127.0.0.1";
            host.sshPort = String.valueOf(server.getLocalPort());
            assertTrue(client.isReachable(host));
            try (Socket accepted = server.accept()) {
                accepted.setSoTimeout(2000);
                assertEquals(-1, accepted.getInputStream().read());
            }
        }
    }

    @Test
    public void pollingCanUseHostnameWithoutAnIpAddress() throws Exception {
        try (ServerSocket server = new ServerSocket(0);
             RemoteClient client = new RemoteClient(null)) {
            HostBean host = new HostBean();
            host.hostname = "localhost";
            host.sshPort = String.valueOf(server.getLocalPort());
            assertTrue(client.isReachable(host));
        }
    }

    @Test
    public void probeDistinguishesOnlineFromSshAvailability() throws Exception {
        int closedPort;
        try (ServerSocket unused = new ServerSocket(0)) {
            closedPort = unused.getLocalPort();
        }
        InetAddress loopback = InetAddress.getByName("127.0.0.1");
        try (RemoteClient client = new RemoteClient(
                null, java.net.NetworkInterface.getByInetAddress(loopback))) {
            HostBean host = new HostBean();
            host.ipAddress = loopback.getHostAddress();
            host.sshUsername = "user";
            host.sshPort = String.valueOf(closedPort);
            RemoteClient.ProbeResult result = client.probe(host, 1000);
            assertTrue(result.online);
            assertTrue(result.sshConfigured);
            assertFalse(result.sshAvailable);
        }
    }

    @Test
    public void probeReportsSshReadyWhenConfiguredPortAcceptsConnections() throws Exception {
        try (ServerSocket server = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"));
             RemoteClient client = new RemoteClient(null)) {
            HostBean host = new HostBean();
            host.ipAddress = "127.0.0.1";
            host.sshUsername = "user";
            host.sshPort = String.valueOf(server.getLocalPort());
            RemoteClient.ProbeResult result = client.probe(host, 1000);
            assertTrue(result.online);
            assertTrue(result.sshConfigured);
            assertTrue(result.sshAvailable);
            try (Socket ignored = server.accept()) {
                // The probe only needs to establish and close the TCP connection.
            }
        }
    }

    @Test
    public void probeReportsOnlineWhenSshIsNotConfigured() {
        try (RemoteClient client = new RemoteClient(null)) {
            HostBean host = new HostBean();
            host.ipAddress = "127.0.0.1";
            RemoteClient.ProbeResult result = client.probe(host, 1000);
            assertTrue(result.online);
            assertFalse(result.sshConfigured);
            assertFalse(result.sshAvailable);
        }
    }

    @Test
    public void cancelledClientsCannotStartNewWork() {
        RemoteClient client = new RemoteClient(null);
        client.close();
        HostBean host = new HostBean();
        host.ipAddress = "127.0.0.1";
        assertThrows(CancellationException.class, () -> client.isReachable(host));
        assertThrows(CancellationException.class,
                () -> client.wake(new RemoteCommand(host, RemoteCommand.WOL), "127.0.0.1"));
    }
}
