/*
 * Copyright (C) 2017 Vadim Frolov
 * Licensed under GNU's GPL 3 or any later version, see README
 */

package com.vadimfrolov.duorem;

import com.vadimfrolov.duorem.Network.HostBean;

/**
 * Interface that is used in communication between host search task and GUI.
 */

public interface DiscoveryListener {
    void onNewHost(HostBean host);
    int getTimeout();
    String getGatewayIp();
    void onStartDiscovering();
    void onStopDiscovering();
    void setDiscoverProgress(int progress);
}
