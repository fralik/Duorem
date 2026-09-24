package com.vadimfrolov.duorem;

import com.vadimfrolov.duorem.Network.RemoteClient;

enum WidgetPowerDecision {
    WAKE,
    SHUTDOWN,
    SSH_UNAVAILABLE,
    SSH_NOT_CONFIGURED;

    static WidgetPowerDecision from(RemoteClient.ProbeResult probe) {
        if (probe.sshAvailable) return SHUTDOWN;
        if (!probe.online) return WAKE;
        return probe.sshConfigured ? SSH_UNAVAILABLE : SSH_NOT_CONFIGURED;
    }
}
