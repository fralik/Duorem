package com.vadimfrolov.duorem;

import com.vadimfrolov.duorem.Network.RemoteClient;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class WidgetPowerDecisionTest {
    @Test
    public void sshReadyShutsDown() {
        assertEquals(WidgetPowerDecision.SHUTDOWN,
                WidgetPowerDecision.from(new RemoteClient.ProbeResult(true, true, true)));
    }

    @Test
    public void unreachableDeviceWakes() {
        assertEquals(WidgetPowerDecision.WAKE,
                WidgetPowerDecision.from(new RemoteClient.ProbeResult(false, true, false)));
    }

    @Test
    public void onlineDeviceWithoutSshDoesNothing() {
        assertEquals(WidgetPowerDecision.SSH_NOT_CONFIGURED,
                WidgetPowerDecision.from(new RemoteClient.ProbeResult(true, false, false)));
        assertEquals(WidgetPowerDecision.SSH_UNAVAILABLE,
                WidgetPowerDecision.from(new RemoteClient.ProbeResult(true, true, false)));
    }
}
