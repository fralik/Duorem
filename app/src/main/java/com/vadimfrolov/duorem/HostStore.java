package com.vadimfrolov.duorem;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.vadimfrolov.duorem.Network.HostBean;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public final class HostStore {
    private static final String LEGACY_TARGET = "target";
    private static final String TARGET = "target_encrypted_v1";
    private static final String KEY_ALIAS = "duorem.target.v1";
    private final SharedPreferences preferences;
    private final Gson gson = new Gson();

    public HostStore(Context context) {
        preferences = context.getSharedPreferences(context.getPackageName() + "_preferences", Context.MODE_PRIVATE);
    }

    private SecretKey key(boolean create) throws GeneralSecurityException, IOException {
        KeyStore store = KeyStore.getInstance("AndroidKeyStore");
        store.load(null);
        if (!store.containsAlias(KEY_ALIAS)) {
            if (!create) {
                throw new GeneralSecurityException("The saved-device encryption key is unavailable");
            }
            KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
            generator.init(new KeyGenParameterSpec.Builder(KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());
            return generator.generateKey();
        }
        return (SecretKey) store.getKey(KEY_ALIAS, null);
    }

    public HostBean load() throws IOException, GeneralSecurityException {
        String encrypted = preferences.getString(TARGET, null);
        String json = preferences.getString(LEGACY_TARGET, null);
        if (encrypted != null) {
            String[] parts = encrypted.split(":", -1);
            if (parts.length != 2) {
                throw new IOException("Invalid saved-device data");
            }
            try {
                Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
                cipher.init(Cipher.DECRYPT_MODE, key(false),
                        new GCMParameterSpec(128, Base64.decode(parts[0], Base64.NO_WRAP)));
                json = new String(cipher.doFinal(Base64.decode(parts[1], Base64.NO_WRAP)), StandardCharsets.UTF_8);
            } catch (IllegalArgumentException e) {
                throw new IOException("Invalid saved-device encoding", e);
            }
        }
        HostBean host;
        try {
            host = gson.fromJson(json, HostBean.class);
        } catch (JsonParseException e) {
            throw new IOException("Saved-device settings could not be read", e);
        }
        if (host != null) {
            host.normalize();
        }
        if (preferences.contains(LEGACY_TARGET)) {
            // One synchronous commit makes migration durable before removing plaintext.
            if (host == null) {
                delete();
            } else {
                save(host);
            }
        }
        return host;
    }

    public void save(HostBean host) throws GeneralSecurityException, IOException {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key(true));
        byte[] encrypted = cipher.doFinal(gson.toJson(host).getBytes(StandardCharsets.UTF_8));
        String value = Base64.encodeToString(cipher.getIV(), Base64.NO_WRAP) + ":"
                + Base64.encodeToString(encrypted, Base64.NO_WRAP);
        if (!preferences.edit().putString(TARGET, value).remove(LEGACY_TARGET).commit()) {
            throw new IOException("Could not save device settings");
        }
    }

    public void delete() throws IOException {
        if (!preferences.edit().remove(TARGET).remove(LEGACY_TARGET).commit()) {
            throw new IOException("Could not delete device settings");
        }
    }
}
