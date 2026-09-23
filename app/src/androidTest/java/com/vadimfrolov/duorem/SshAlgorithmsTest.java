package com.vadimfrolov.duorem;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.KeyPair;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class SshAlgorithmsTest {
    @Test
    public void ed25519IsAvailableOnAndroidRuntime() throws Exception {
        KeyPair key = KeyPair.genKeyPair(new JSch(), KeyPair.ED25519);
        try {
            assertEquals(KeyPair.ED25519, key.getKeyType());
            assertNotNull(key.getPublicKeyBlob());
            assertNotNull(key.getSignature(new byte[]{1, 2, 3}));
        } finally {
            key.dispose();
        }
    }
}
