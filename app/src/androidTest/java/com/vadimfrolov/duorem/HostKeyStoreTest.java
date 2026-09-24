package com.vadimfrolov.duorem;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.jcraft.jsch.HostKey;
import com.jcraft.jsch.HostKeyRepository;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class HostKeyStoreTest {
    private Context context;
    private SharedPreferences preferences;

    @Before
    public void setUp() {
        context = new ContextWrapper(InstrumentationRegistry.getInstrumentation().getTargetContext()) {
            @Override public SharedPreferences getSharedPreferences(String name, int mode) {
                return super.getSharedPreferences("test_host_keys", mode);
            }
        };
        preferences = context.getSharedPreferences("test_host_keys", Context.MODE_PRIVATE);
        preferences.edit().clear().commit();
    }

    @After public void cleanUp() { preferences.edit().clear().commit(); }

    private byte[] key(byte value) throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        DataOutputStream output = new DataOutputStream(buffer);
        byte[] type = "ssh-ed25519".getBytes(StandardCharsets.US_ASCII);
        output.writeInt(type.length);
        output.write(type);
        output.writeInt(32);
        for (int i = 0; i < 32; i++) output.write(value);
        return buffer.toByteArray();
    }

    @Test
    public void unknownChangedAndNondefaultPortKeysRequireExplicitTrust() throws Exception {
        HostKeyStore keys = new HostKeyStore(context);
        byte[] original = key((byte) 1);
        byte[] changed = key((byte) 2);
        assertEquals(HostKeyRepository.NOT_INCLUDED, keys.check("server", original));
        assertNotNull(keys.untrustedKey());
        assertFalse(keys.hasChanged());
        assertTrue(preferences.getAll().isEmpty());
        keys.trust(new HostKey("server", original));
        keys = new HostKeyStore(context);
        assertEquals(HostKeyRepository.OK, keys.check("server", original));
        assertEquals(HostKeyRepository.CHANGED, keys.check("server", changed));
        assertTrue(keys.hasChanged());
        assertEquals(HostKeyRepository.NOT_INCLUDED, keys.check("[server]:2222", original));
        assertEquals(HostKeyRepository.OK, new HostKeyStore(context).check("server", original));
    }
}
