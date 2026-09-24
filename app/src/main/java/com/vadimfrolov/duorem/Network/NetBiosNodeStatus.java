package com.vadimfrolov.duorem.Network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

public final class NetBiosNodeStatus {
    private static final int NBNS_PORT = 137;
    private static final int NBSTAT_TYPE = 0x21;
    private static final int HEADER_LENGTH = 12;
    private static final int MAX_RESPONSE_LENGTH = 576;

    private NetBiosNodeStatus() {}

    public static String query(DatagramSocket socket, InetAddress address, int timeout)
            throws IOException {
        int transactionId = ThreadLocalRandom.current().nextInt(0x10000);
        byte[] request = request(transactionId);
        socket.setSoTimeout(timeout);
        socket.send(new DatagramPacket(request, request.length, address, NBNS_PORT));

        DatagramPacket response = new DatagramPacket(new byte[MAX_RESPONSE_LENGTH],
                MAX_RESPONSE_LENGTH);
        socket.receive(response);
        return parse(response.getData(), response.getLength(), transactionId);
    }

    static byte[] request(int transactionId) {
        byte[] request = new byte[50];
        writeU16(request, 0, transactionId);
        writeU16(request, 2, 0x0010);
        writeU16(request, 4, 1);
        request[12] = 32;

        byte[] name = new byte[16];
        name[0] = '*';
        for (int i = 0; i < name.length; i++) {
            request[13 + i * 2] = (byte) ('A' + ((name[i] >> 4) & 0x0f));
            request[14 + i * 2] = (byte) ('A' + (name[i] & 0x0f));
        }
        request[45] = 0;
        writeU16(request, 46, NBSTAT_TYPE);
        writeU16(request, 48, 1);
        return request;
    }

    static String parse(byte[] response, int length, int transactionId) {
        if (response == null || length < HEADER_LENGTH || length > response.length
                || readU16(response, 0) != transactionId
                || (readU16(response, 2) & 0x800f) != 0x8000) {
            return NetInfo.NOMAC;
        }

        int offset = HEADER_LENGTH;
        int questions = readU16(response, 4);
        int records = readU16(response, 6) + readU16(response, 8) + readU16(response, 10);
        for (int i = 0; i < questions; i++) {
            offset = skipName(response, length, offset);
            if (offset < 0 || offset + 4 > length) return NetInfo.NOMAC;
            offset += 4;
        }

        for (int i = 0; i < records; i++) {
            offset = skipName(response, length, offset);
            if (offset < 0 || offset + 10 > length) return NetInfo.NOMAC;
            int type = readU16(response, offset);
            int dataLength = readU16(response, offset + 8);
            int dataOffset = offset + 10;
            int dataEnd = dataOffset + dataLength;
            if (dataEnd > length) return NetInfo.NOMAC;

            if (type == NBSTAT_TYPE && dataLength >= 7) {
                int names = response[dataOffset] & 0xff;
                int macOffset = dataOffset + 1 + names * 18;
                if (macOffset + 6 <= dataEnd) {
                    return formatMac(response, macOffset);
                }
            }
            offset = dataEnd;
        }
        return NetInfo.NOMAC;
    }

    private static int skipName(byte[] packet, int length, int offset) {
        while (offset < length) {
            int labelLength = packet[offset] & 0xff;
            if (labelLength == 0) return offset + 1;
            if ((labelLength & 0xc0) == 0xc0) {
                return offset + 2 <= length ? offset + 2 : -1;
            }
            if ((labelLength & 0xc0) != 0 || offset + 1 + labelLength > length) return -1;
            offset += 1 + labelLength;
        }
        return -1;
    }

    private static String formatMac(byte[] data, int offset) {
        boolean allZero = true;
        boolean allOnes = true;
        for (int i = 0; i < 6; i++) {
            allZero &= data[offset + i] == 0;
            allOnes &= (data[offset + i] & 0xff) == 0xff;
        }
        if (allZero || allOnes) return NetInfo.NOMAC;

        return String.format(Locale.ROOT, "%02X:%02X:%02X:%02X:%02X:%02X",
                data[offset] & 0xff, data[offset + 1] & 0xff, data[offset + 2] & 0xff,
                data[offset + 3] & 0xff, data[offset + 4] & 0xff, data[offset + 5] & 0xff);
    }

    private static int readU16(byte[] data, int offset) {
        return ((data[offset] & 0xff) << 8) | (data[offset + 1] & 0xff);
    }

    private static void writeU16(byte[] data, int offset, int value) {
        data[offset] = (byte) (value >> 8);
        data[offset + 1] = (byte) value;
    }
}
