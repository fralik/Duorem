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
