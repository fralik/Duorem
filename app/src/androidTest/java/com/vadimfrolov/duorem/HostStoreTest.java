package com.vadimfrolov.duorem;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.gson.Gson;
import com.vadimfrolov.duorem.Network.HostBean;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class HostStoreTest {
    private SharedPreferences preferences;
    private HostStore store;
    private Context context;

    @Before
    public void setUp() {
        context = new ContextWrapper(InstrumentationRegistry.getInstrumentation().getTargetContext()) {
            @Override
            public SharedPreferences getSharedPreferences(String name, int mode) {
                return super.getSharedPreferences("test_host_store", mode);
            }
        };
        preferences = context.getSharedPreferences("test_host_store", Context.MODE_PRIVATE);
        preferences.edit().clear().commit();
        store = new HostStore(context);
    }

    @After
    public void cleanUp() {
        preferences.edit().clear().commit();
    }

    @Test
    public void migratesLegacySettingsWithoutPlaintextPassword() throws Exception {
        HostBean host = new HostBean();
        host.sshPassword = "migration-test-password";
        host.sshUsername = "user";
        host.sshPort = "2222";
        host.sshShutdownCmd = "custom command";
        preferences.edit().putString("target", new Gson().toJson(host)).commit();
        HostBean migrated = store.load();
        assertEquals(host.sshPassword, migrated.sshPassword);
        assertFalse(preferences.contains("target"));
        assertFalse(preferences.getString("target_encrypted_v1", "").contains(host.sshPassword));
        HostBean loaded = new HostStore(context).load();
        assertNotNull(loaded);
        assertEquals("2222", loaded.sshPort);
        assertEquals("custom command", loaded.sshShutdownCmd);
        assertEquals(host.sshPassword, loaded.sshPassword);
    }

    @Test
    public void deletionSurvivesReload() throws Exception {
        store.save(new HostBean());
        store.delete();
        assertNull(store.load());
        assertFalse(preferences.contains("target_encrypted_v1"));
    }

    @Test
    public void corruptDataIsReportedAndNotOverwritten() {
        preferences.edit().putString("target_encrypted_v1", "invalid-data").commit();
        assertThrows(java.io.IOException.class, () -> store.load());
        assertEquals("invalid-data", preferences.getString("target_encrypted_v1", null));
    }
}
