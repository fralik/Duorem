/*
 * Copyright (C) 2009-2010 Aubort Jean-Baptiste (Rorist)
 * Copyright (C) 2017 Vadim Frolov
 * Licensed under GNU's GPL 3 or any later version, see README
 */
package com.vadimfrolov.duorem.Network;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Stores information about network hosts and additional information about SSH authentication.
 * Logically, SSH info should not be here, but it makes things a bit easier.
 * This class is inspired by HostBean class from <a href="https://github.com/rorist/android-network-discovery">Android Network Discovery</a> app.
 */

public class HostBean implements Parcelable {
    public static final String EXTRA = "com.vadimfrolov.Duorem.extra";

    /** Default shutdown command */
    public static final String SHUTDOWN_CMD = "sudo shutdown -h now";

    /** Indicates if host is reachable AKA is alive */
    public boolean isAlive = false;
    /** String representation of IPv4 address */
    public String ipAddress = null;
    public String hostname = null;
    public String hardwareAddress = NetInfo.NOMAC;
    public String sshUsername = "";
    /** Either password or real path to SSH key */
    public String sshPassword = "";
    public String sshPort = "22";
    /** User-defined command to shutdown remote host */
    public String sshShutdownCmd = SHUTDOWN_CMD;
    /** Wake On Lan port */
    public String wolPort = "9";
    public String broadcastIp = null;
    /** Indicates if host is reachable via SSH or is it a Windows machine */
    public boolean isSsh = true;
    public String sshKey = "";

    public HostBean() {
        // New object
    }

    public HostBean(Parcel in) {
        // Object from parcel
        readFromParcel(in);
    }

    // full constructor
//    public HostBean(String hostname, String ipAddress, String wolPort, String macAddress, String sshUsername,
//                    String sshPassword, String sshPort, String broadcastIp, String sshShutdownCmd,
//                    boolean isSsh) {
//        this.hostname = hostname;
//        this.ipAddress = ipAddress;
//        this.wolPort = wolPort;
//        this.hardwareAddress = macAddress;
//        this.sshUsername = sshUsername;
//        this.sshPassword = sshPassword;
//        this.sshPort = sshPort;
//        this.broadcastIp = broadcastIp;
//        this.sshShutdownCmd = sshShutdownCmd;
//        this.isSsh = isSsh;
//    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(isAlive ? 1 : 0);
        dest.writeString(ipAddress);
        dest.writeString(hostname);
        dest.writeString(hardwareAddress);
        dest.writeString(sshUsername);
        dest.writeString(sshPassword);
        dest.writeString(sshPort);
        dest.writeString(wolPort);
        dest.writeString(broadcastIp);
        dest.writeString(sshShutdownCmd);
        dest.writeInt(isSsh ? 1 : 0);
        dest.writeString(sshKey);
    }

    private void readFromParcel(Parcel in) {
        isAlive = in.readInt() == 1;
        ipAddress = in.readString();
        hostname = in.readString();
        hardwareAddress = in.readString();
        sshUsername = in.readString();
        sshPassword = in.readString();
        sshPort = in.readString();
        wolPort = in.readString();
        broadcastIp = in.readString();
        sshShutdownCmd = in.readString();
        isSsh = in.readInt() == 1;
        sshKey = in.readString();
    }

    public String name() {
        if (hostname == null && ipAddress == null) {
            return "";
        }

        String result = null;

        boolean ipValid = ipAddress != null && !ipAddress.equals(NetInfo.NOIP);
        boolean macValid = hardwareAddress != null && !hardwareAddress.equals(NetInfo.NOMAC);
        if (hostname != null && hostname.length() > 0) {
            if (ipValid)
                result = hostname + " (" + ipAddress + ")";
            else if (macValid)
                result = hostname + " (" + hardwareAddress + ")";
            else
                result = hostname;
        }
        else {
            if (ipValid)
                result = ipAddress;
            else
                result = hardwareAddress;
        }

        if (isSsh) {
            result += ", SSH";
        } else {
            result += ", Windows";
        }

        return result;
    }

    // Make sure that all fields are initialized and ready to be presented to user
    public void resetForView() {
        ipAddress = NetInfo.NOIP;
        hardwareAddress = NetInfo.NOMAC;
        isAlive = false;
        wolPort = "9";
        sshPort = "22";
        sshUsername = "";
        sshPassword = "";
        hostname = "";
        broadcastIp = NetInfo.NOIP;
        sshShutdownCmd = SHUTDOWN_CMD;
        isSsh = true;
        sshKey = "";
    }

    public String sshKeyName() {
        if (sshKey.equals("")) {
            return "";
        }
        final String[] split = sshKey.split(":"); //split the path.
        final String[] nameSplit = split[1].split("/");
        return nameSplit[nameSplit.length - 1];
    }

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public HostBean createFromParcel(Parcel in) {
            return new HostBean(in);
        }

        public HostBean[] newArray(int size) {
            return new HostBean[size];
        }
    };
}
