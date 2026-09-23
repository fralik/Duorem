package com.vadimfrolov.duorem.Network;

import com.google.gson.Gson;

import org.junit.Test;

import static org.junit.Assert.*;

public class HostBeanTest {
    @Test
    public void oldSettingsKeepCredentialsCommandsAndNetworkConfiguration() {
        HostBean host = new Gson().fromJson("{\"hostname\":\"server\",\"hardwareAddress\":\"01:23:45:67:89:AB\","
                + "\"sshPassword\":\"old password\",\"sshShutdownCmd\":\"custom shutdown\","
                + "\"sshPort\":\"2222\",\"wolPort\":\"7\",\"broadcastIp\":\"10.0.0.255\",\"isAlive\":true}", HostBean.class);
        host.normalize();
        assertEquals("old password", host.sshPassword);
        assertEquals("custom shutdown", host.sshShutdownCmd);
        assertEquals("2222", host.sshPort);
        assertEquals("7", host.wolPort);
        assertEquals("10.0.0.255", host.broadcastIp);
        assertFalse(host.isAlive);
        assertTrue(host.canWake());
    }

    @Test
    public void missingLegacyFieldsAreNormalized() {
        HostBean host = new Gson().fromJson("{\"ipAddress\":null,\"hostname\":null,\"broadcastIp\":null,"
                + "\"sshShutdownCmd\":null,\"sshPort\":null,\"hardwareAddress\":null}", HostBean.class);
        host.normalize();
        assertEquals(NetInfo.NOIP, host.ipAddress);
        assertEquals(NetInfo.NOMAC, host.hardwareAddress);
        assertEquals(HostBean.SHUTDOWN_CMD, host.sshShutdownCmd);
        assertEquals("22", host.sshPort);
        assertFalse(host.hasAddress());
        assertFalse(host.canWake());
        assertFalse(host.canUseSsh());
    }

    @Test
    public void wolAndSshCanBeConfiguredIndependently() {
        HostBean host = new HostBean();
        host.hardwareAddress = "01:23:45:67:89:AB";
        assertTrue(host.canWake());
        assertFalse(host.canUseSsh());
        host.hardwareAddress = NetInfo.NOMAC;
        host.ipAddress = "192.168.1.2";
        host.sshUsername = "user";
        assertFalse(host.canWake());
        assertTrue(host.canUseSsh());
    }
}
