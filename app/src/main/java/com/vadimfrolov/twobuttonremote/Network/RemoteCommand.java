/*
 * Copyright (C) 2017 Vadim Frolov
 * Licensed under GNU's GPL 2, see README
 */
package com.vadimfrolov.twobuttonremote.Network;

/**
 * Remote command for target.
 * Created by vadimf on 3/4/2017.
 */

public class RemoteCommand {
    /** This is a ping command */
    public static final int PING = 0;
    /** This is a wake on lan command */
    public static final int WOL = 1;
    /** This is an ssh command */
    public static final int SSH = 2;
    /** This is invalid command */
    public static final int INVALID_CMD = -1;

    /** Network details of the command target */
    public final HostBean target;
    /** Command to execute on remote */
    public String command = null;
    /** Result of command execution */
    public String result = null;
    public int commandType = INVALID_CMD;

    public RemoteCommand(HostBean target) {
        this.target = target;
    }

    public RemoteCommand(HostBean target, int cmd) {
        this.target = target;
        if (cmd >= 0 && cmd <= 2) {
            this.commandType = cmd;
        } else {
            this.commandType = INVALID_CMD;
        }
    }

    public int sshPort() {
        return Integer.parseInt(target.sshPort);
    }

    public int wolPort() {
        return Integer.parseInt(target.wolPort);
    }
};

