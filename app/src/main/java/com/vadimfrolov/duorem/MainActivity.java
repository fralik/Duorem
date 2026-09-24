/*
 * Copyright (C) 2017 Vadim Frolov
 * Licensed under GNU's GPL 3 or any later version, see README
 */
package com.vadimfrolov.duorem;

import android.content.Intent;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.jcraft.jsch.HostKey;
import com.jcraft.jsch.JSchException;
import com.vadimfrolov.duorem.Network.HostBean;
import com.vadimfrolov.duorem.Network.NetInfo;
import com.vadimfrolov.duorem.Network.RemoteClient;
import com.vadimfrolov.duorem.Network.RemoteCommand;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class MainActivity extends ActivityNet {
    private HostBean target;
    private HostStore store;
    private TextView name;
    private TextView status;
    private ImageView alive;
    private Button power;
    private Button restart;
    private boolean resumed;
    private boolean commandRunning;
    private boolean unreadableSettings;
    private int generation;
    private String networkIdentity;
    private ScheduledFuture<?> poll;
    private AlertDialog trustDialog;
    private final Set<RemoteClient> clients = ConcurrentHashMap.newKeySet();
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_main);
        setSupportActionBar((Toolbar) findViewById(R.id.main_toolbar));
        store = new HostStore(this);
        name = findViewById(R.id.id);
        status = findViewById(R.id.text_status);
        alive = findViewById(R.id.alive);
        power = findViewById(R.id.btn_toggle_power);
        restart = findViewById(R.id.btn_restart);
        status.setMovementMethod(new ScrollingMovementMethod());
        status.setOnClickListener(view -> {
            if (!hasNetworkPermission()) requestNetworkPermission();
        });
        power.setOnClickListener(view -> {
            if (!hasNetworkPermission()) {
                requestNetworkPermission();
            } else if (target != null) {
                RemoteCommand command = new RemoteCommand(target,
                        target.isAlive && target.canUseSsh() ? RemoteCommand.SSH : RemoteCommand.WOL);
                command.command = target.sshShutdownCmd;
                runCommand(command);
            }
        });
        restart.setOnClickListener(view -> {
            if (!hasNetworkPermission()) {
                requestNetworkPermission();
            } else if (target != null) {
                RemoteCommand command = new RemoteCommand(target, RemoteCommand.SSH);
                command.command = "sudo shutdown -r now";
                runCommand(command);
            }
        });
    }

    @Override
    protected void onResume() {
        resumed = true;
        unreadableSettings = false;
        try {
            target = store.load();
        } catch (IOException | GeneralSecurityException e) {
            target = null;
            unreadableSettings = true;
            reportError(getString(R.string.settings_load_failed), e);
        }
        super.onResume();
    }

    @Override
    protected void onPause() {
        resumed = false;
        cancelNetworkWork();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }

    private void cancelNetworkWork() {
        generation++;
        if (poll != null) {
            poll.cancel(true);
            poll = null;
        }
        for (RemoteClient client : clients) client.close();
        clients.clear();
        if (commandRunning) {
            log(getString(R.string.command_cancelled));
        }
        commandRunning = false;
        if (trustDialog != null) {
            trustDialog.dismiss();
            trustDialog = null;
        }
    }

    @Override
    protected void updateNetworkStatus() {
        String identity = mNetInfo.identity();
        if (!identity.equals(networkIdentity) || !mIsConnected) {
            cancelNetworkWork();
            networkIdentity = identity;
            if (target != null) target.isAlive = false;
        }
        startPolling();
        updateView();
    }

    private void startPolling() {
        if (!resumed || !mIsConnected || target == null || !target.hasAddress()
                || poll != null || commandRunning) return;
        HostBean current = target;
        NetInfo network = mNetInfo;
        int token = generation;
        poll = executor.scheduleWithFixedDelay(() -> {
            boolean reachable = false;
            RemoteClient client = new RemoteClient(network.network);
            clients.add(client);
            try (client) {
                reachable = client.isReachable(current);
            } catch (IOException e) {
                // A closed SSH port or an offline host is a normal polling result.
                Log.d("MainActivity", "Host is not reachable");
            } catch (IllegalArgumentException | SecurityException e) {
                Log.w("MainActivity", "Invalid or inaccessible polling endpoint", e);
            } catch (CancellationException e) {
                return;
            } finally {
                clients.remove(client);
            }
            boolean result = reachable;
            runOnUiThread(() -> {
                if (resumed && token == generation && target == current) {
                    target.isAlive = result;
                    updateView();
                }
            });
        }, 0, 5, TimeUnit.SECONDS);
    }

    private void runCommand(RemoteCommand command) {
        if (!resumed || !mIsConnected || commandRunning) return;
        cancelNetworkWork();
        commandRunning = true;
        int token = generation;
        NetInfo network = mNetInfo;
        HostKeyStore keys = new HostKeyStore(this);
        RemoteClient client = new RemoteClient(network.network);
        clients.add(client);
        log(getString(R.string.command_sending));
        updateView();
        executor.execute(() -> {
            try (client) {
                if (command.commandType == RemoteCommand.WOL) {
                    client.wake(command, network.broadcastIp);
                } else {
                    client.execute(command, keys);
                }
            } catch (IOException | JSchException | IllegalArgumentException | SecurityException e) {
                command.error = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
                Log.w("MainActivity", "Remote command failed: " + e.getClass().getSimpleName());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (CancellationException e) {
                return;
            } finally {
                clients.remove(client);
            }
            runOnUiThread(() -> {
                if (!resumed || token != generation || target != command.target) return;
                commandRunning = false;
                if (keys.untrustedKey() != null) {
                    confirmHostKey(command, keys);
                } else if (command.success) {
                    log(getString(command.commandType == RemoteCommand.WOL ? R.string.wol_received
                            : command.command.equals("sudo shutdown -r now")
                            ? R.string.reboot_received : R.string.shutdown_received));
                } else {
                    log(getString(R.string.command_failed, command.error));
                }
                startPolling();
                updateView();
            });
        });
    }

    private void confirmHostKey(RemoteCommand command, HostKeyStore keys) {
        HostKey key = keys.untrustedKey();
        String fingerprint;
        try {
            fingerprint = "SHA256:" + Base64.getEncoder().withoutPadding().encodeToString(
                    MessageDigest.getInstance("SHA-256").digest(Base64.getDecoder().decode(key.getKey())));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
        trustDialog = new AlertDialog.Builder(this)
                .setTitle(keys.hasChanged() ? R.string.host_key_changed : R.string.host_key_unknown)
                .setMessage(getString(R.string.host_key_confirm, key.getHost(), key.getType(), fingerprint))
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.trust_and_retry, (dialog, which) -> {
                    try {
                        keys.trust(key);
                        runCommand(command);
                    } catch (IOException e) {
                        reportError(getString(R.string.settings_save_failed), e);
                    }
                }).show();
    }

    private void updateView() {
        if (power == null) return;
        boolean isAlive = target != null && target.isAlive;
        boolean shutdown = isAlive && target.canUseSsh();
        name.setText(target == null ? getString(R.string.no_device) : target.name());
        alive.setVisibility(target == null ? View.GONE : View.VISIBLE);
        alive.setColorFilter(ContextCompat.getColor(this,
                isAlive ? R.color.statusOnline : R.color.statusOffline));
        power.setText(shutdown ? R.string.shutdown : R.string.turn_on);
        power.setEnabled(!commandRunning && mNetInfo.isConnected && target != null
                && (shutdown || target.canWake()));
        restart.setEnabled(!commandRunning && mNetInfo.isConnected && target != null && target.canUseSsh());
        if (!hasNetworkPermission()) {
            status.setText(R.string.network_permission_required);
        } else if (!mNetInfo.isConnected) {
            status.setText(R.string.no_network);
        } else if (status.getText().toString().equals(getString(R.string.network_permission_required))
                || status.getText().toString().equals(getString(R.string.no_network))) {
            status.setText("");
        }
        invalidateOptionsMenu();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.action_add_host).setTitle(target == null ? R.string.add_host : R.string.replace_host);
        menu.findItem(R.id.action_edit_host).setEnabled(target != null);
        menu.findItem(R.id.action_delete_host).setEnabled(target != null || unreadableSettings);
        menu.findItem(R.id.action_network_permission).setVisible(!hasNetworkPermission());
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_add_host) {
            startActivity(new Intent(this, SearchConfigureActivity.class));
        } else if (id == R.id.action_edit_host && target != null) {
            startActivity(new Intent(this, TargetConfigurationActivity.class).putExtra(HostBean.EXTRA, target));
        } else if (id == R.id.action_delete_host) {
            try {
                store.delete();
                cancelNetworkWork();
                target = null;
                unreadableSettings = false;
                status.setText("");
                updateView();
            } catch (IOException e) {
                reportError(getString(R.string.settings_save_failed), e);
            }
        } else if (id == R.id.action_network_permission) {
            requestNetworkPermission();
        } else if (id == R.id.action_about) {
            AboutDialog.newInstance(getString(R.string.about_title), getVersionName(this))
                    .show(getSupportFragmentManager(), "about_dialog");
        } else {
            return super.onOptionsItemSelected(item);
        }
        return true;
    }

    private void reportError(String message, Exception error) {
        Log.e("MainActivity", message, error);
        new AlertDialog.Builder(this).setMessage(message)
                .setPositiveButton(android.R.string.ok, null).show();
    }

    private void log(String message) {
        status.append(message + "\n");
        status.post(() -> {
            if (status.getLayout() != null) {
                status.scrollTo(0, Math.max(0, status.getLayout().getHeight() - status.getHeight()));
            }
        });
    }
}
