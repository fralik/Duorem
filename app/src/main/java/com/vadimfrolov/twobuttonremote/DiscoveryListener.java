package com.vadimfrolov.twobuttonremote;

import com.vadimfrolov.twobuttonremote.Network.HostBean;

/**
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
