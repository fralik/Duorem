/*
 * Copyright (C) 2017 Vadim Frolov
 * Licensed under GNU's GPL 2, see README
 */

package com.vadimfrolov.twobuttonremote;

import com.vadimfrolov.twobuttonremote.Network.HostBean;

/**
 * Interface that is used in communication between host search task and GUI.
 * Created by vadimf on 27/3/2017.
 */

public interface DiscoveryListener {
    void onNewHost(HostBean host);
    int getTimeout();
    String getGatewayIp();
    void onStartDiscovering();
    void onStopDiscovering();
    void setDiscoverProgress(int progress);
}
