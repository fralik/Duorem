package com.vadimfrolov.duorem;

import android.content.Context;
import android.content.SharedPreferences;

import com.jcraft.jsch.HostKey;
import com.jcraft.jsch.HostKeyRepository;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.UserInfo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

public final class HostKeyStore implements HostKeyRepository {
    private final SharedPreferences preferences;
    private HostKey untrusted;
    private boolean changed;

    public HostKeyStore(Context context) {
        preferences = context.getSharedPreferences("ssh_host_keys", Context.MODE_PRIVATE);
    }

    @Override
    public int check(String host, byte[] key) {
        String saved = preferences.getString(host, null);
        if (Base64.getEncoder().encodeToString(key).equals(saved)) {
            return OK;
        }
        try {
            untrusted = new HostKey(host, key);
        } catch (JSchException e) {
            throw new IllegalArgumentException("Invalid SSH host key", e);
        }
        changed = saved != null;
        return changed ? CHANGED : NOT_INCLUDED;
    }

    public HostKey untrustedKey() { return untrusted; }
    public boolean hasChanged() { return changed; }

    public void trust(HostKey key) throws IOException {
        if (!preferences.edit().putString(key.getHost(), key.getKey()).commit()) {
            throw new IOException("Could not save SSH host key");
        }
    }

    @Override public String getKnownHostsRepositoryID() { return "Duorem"; }
    @Override public HostKey[] getHostKey() { return getHostKey(null, null); }

    @Override
    public HostKey[] getHostKey(String host, String type) {
        List<HostKey> keys = new ArrayList<>();
        for (Map.Entry<String, ?> entry : preferences.getAll().entrySet()) {
            if (host == null || host.equals(entry.getKey())) {
                try {
                    HostKey key = new HostKey(entry.getKey(), Base64.getDecoder().decode((String) entry.getValue()));
                    if (type == null || type.equals(key.getType())) {
                        keys.add(key);
                    }
                } catch (JSchException | IllegalArgumentException e) {
                    throw new IllegalStateException("Could not read saved SSH host key", e);
                }
            }
        }
        return keys.toArray(new HostKey[0]);
    }

    @Override public void add(HostKey key, UserInfo info) { throw new UnsupportedOperationException("Explicit trust is required"); }
    @Override public void remove(String host, String type) { throw new UnsupportedOperationException("Explicit trust is required"); }
    @Override public void remove(String host, String type, byte[] key) { throw new UnsupportedOperationException("Explicit trust is required"); }
}
