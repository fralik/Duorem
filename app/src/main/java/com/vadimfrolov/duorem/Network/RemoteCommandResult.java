/*
 * Copyright (C) 2017 Vadim Frolov
 * Licensed under GNU's GPL 3 or any later version, see README
 */

package com.vadimfrolov.duorem.Network;

/**
 * Callback to notify UI thread of remote command results.
 */

public interface RemoteCommandResult {
    void onRemoteCommandFinished(RemoteCommand output);
}
