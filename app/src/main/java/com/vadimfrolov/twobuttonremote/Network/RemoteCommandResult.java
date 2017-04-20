package com.vadimfrolov.twobuttonremote.Network;

/**
 * Callback to notify UI thread of remote command results.
 *
 * Created by vadimf on 3/4/2017.
 */

public interface RemoteCommandResult {
    void onRemoteCommandFinished(RemoteCommand output);
}
