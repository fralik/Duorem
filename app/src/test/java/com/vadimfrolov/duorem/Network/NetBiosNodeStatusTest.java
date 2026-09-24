package com.vadimfrolov.duorem.Network;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class NetBiosNodeStatusTest {
    @Test
    public void requestUsesWildcardNodeStatusQuestion() {
        byte[] request = NetBiosNodeStatus.request(0x1234);

        assertEquals(50, request.length);
        assertArrayEquals(new byte[]{0x12, 0x34, 0, 0x10, 0, 1},
                Arrays.copyOfRange(request, 0, 6));
        assertEquals(32, request[12]);
        assertEquals('C', request[13]);
        assertEquals('K', request[14]);
        for (int i = 15; i < 45; i++) assertEquals('A', request[i]);
        assertArrayEquals(new byte[]{0, 0, 0x21, 0, 1},
                Arrays.copyOfRange(request, 45, 50));
    }

    @Test
    public void parsesUnitIdFromNodeStatusResponse() {
        byte[] response = response(new byte[]{0x10, 0x20, 0x30, 0x40, 0x50, 0x60});

        assertEquals("10:20:30:40:50:60",
                NetBiosNodeStatus.parse(response, response.length, 0x1234));
    }

    @Test
    public void rejectsWrongTransactionMalformedPacketsAndUnsetMacs() {
        byte[] response = response(new byte[6]);
        assertEquals(NetInfo.NOMAC,
                NetBiosNodeStatus.parse(response, response.length, 0x9999));
        assertEquals(NetInfo.NOMAC,
                NetBiosNodeStatus.parse(response, 20, 0x1234));
        assertEquals(NetInfo.NOMAC,
                NetBiosNodeStatus.parse(response, response.length, 0x1234));
    }

    private static byte[] response(byte[] mac) {
        byte[] response = new byte[12 + 2 + 10 + 25];
        response[0] = 0x12;
        response[1] = 0x34;
        response[2] = (byte) 0x85;
        response[3] = 0;
        response[7] = 1;
        response[12] = (byte) 0xc0;
        response[13] = 0x0c;
        response[14] = 0;
        response[15] = 0x21;
        response[16] = 0;
        response[17] = 1;
        response[22] = 0;
        response[23] = 25;
        response[24] = 1;
        byte[] name = "TEST           ".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        System.arraycopy(name, 0, response, 25, 15);
        response[40] = 0;
        response[41] = 0;
        response[42] = 0;
        System.arraycopy(mac, 0, response, 43, 6);
        return response;
    }
}
