package com.vadimfrolov.duorem;

import android.Manifest;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.jcraft.jsch.JSchException;
import com.vadimfrolov.duorem.Network.HostBean;
import com.vadimfrolov.duorem.Network.NetInfo;
import com.vadimfrolov.duorem.Network.RemoteClient;
import com.vadimfrolov.duorem.Network.RemoteCommand;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.concurrent.CancellationException;

public final class DeviceWidgetWorker extends Worker {
    private static final String TAG = "DeviceWidgetWorker";
    static final String ACTION_REFRESH = "com.vadimfrolov.duorem.action.WIDGET_REFRESH";
    static final String INPUT_MONITOR_GENERATION = "monitor_generation";

    public DeviceWidgetWorker(@NonNull Context context, @NonNull WorkerParameters parameters) {
        super(context, parameters);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        int appWidgetId = getInputData().getInt(
                DeviceWidgetProvider.INPUT_WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        WidgetStore.Settings settings = new WidgetStore(context).load(appWidgetId);
        String action = getInputData().getString(DeviceWidgetActionReceiver.INPUT_ACTION);
        boolean refresh = ACTION_REFRESH.equals(action);
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID || settings == null) {
            return Result.failure();
        }
        try {
            if (Build.VERSION.SDK_INT >= 37
                    && context.checkSelfPermission(Manifest.permission.ACCESS_LOCAL_NETWORK)
                    != PackageManager.PERMISSION_GRANTED) {
                return finish(appWidgetId, R.string.widget_status_permission, true);
            }
            NetInfo network = new NetInfo(context);
            if (!network.isConnected) {
                return finish(appWidgetId, R.string.widget_status_no_network, true);
            }
            HostBean target = new HostStore(context).load();
            if (target == null) {
                return finish(appWidgetId, R.string.widget_status_no_device, true);
            }
            try (RemoteClient client = new RemoteClient(network.network)) {
                if (ACTION_REFRESH.equals(action)) {
                    return refresh(client, target, appWidgetId);
                }
                if (DeviceWidgetActionReceiver.ACTION_RESTART.equals(action)) {
                    return execute(client, target, "sudo shutdown -r now",
                            R.string.widget_status_restart_sent, appWidgetId);
                }
                if (!DeviceWidgetActionReceiver.ACTION_POWER.equals(action)) {
                    return finish(appWidgetId, R.string.widget_status_failed, true);
                }
                RemoteClient.ProbeResult probe = client.probe(target, 1000);
                switch (WidgetPowerDecision.from(probe)) {
                    case SHUTDOWN:
                        return execute(client, target, target.sshShutdownCmd,
                                R.string.widget_status_shutdown_sent, appWidgetId);
                    case SSH_UNAVAILABLE:
                        return finish(appWidgetId, R.string.widget_status_ssh_unavailable, true);
                    case SSH_NOT_CONFIGURED:
                        return finish(appWidgetId, R.string.widget_status_ssh_not_configured, true);
                    case WAKE:
                    default:
                        return wake(client, target, network, appWidgetId);
                }
            }
        } catch (IOException | GeneralSecurityException e) {
            Log.w(TAG, "Could not load widget device settings", e);
            return finish(appWidgetId, R.string.widget_status_settings_error, true);
        } catch (IllegalArgumentException | SecurityException e) {
            Log.w(TAG, "Widget command was rejected", e);
            return finish(appWidgetId, R.string.widget_status_failed, true);
        } catch (CancellationException e) {
            return Result.failure();
        } finally {
            if (refresh && !isStopped()) {
                WidgetStore.Settings current = new WidgetStore(context).load(appWidgetId);
                int generation = getInputData().getInt(INPUT_MONITOR_GENERATION, -1);
                if (current != null && current.monitorEnabled
                        && current.generation == generation) {
                    WidgetMonitorScheduler.scheduleNext(context, appWidgetId, current);
                }
            }
        }
    }

    private Result refresh(RemoteClient client, HostBean target, int appWidgetId) {
        RemoteClient.ProbeResult probe = client.probe(target, 1000);
        if (probe.sshAvailable) {
            return finish(appWidgetId, R.string.connection_online_ssh_ready,
                    R.color.statusOnline);
        }
        if (probe.online) {
            return finish(appWidgetId, probe.sshConfigured
                            ? R.string.connection_online_ssh_unavailable
                            : R.string.connection_online_ssh_not_configured,
                    R.color.statusWarning);
        }
        return finish(appWidgetId, R.string.connection_unreachable, R.color.statusOffline);
    }

    private Result wake(RemoteClient client, HostBean target, NetInfo network, int appWidgetId) {
        if (!target.canWake()) {
            return finish(appWidgetId, R.string.widget_status_mac_required, true);
        }
        RemoteCommand command = new RemoteCommand(target, RemoteCommand.WOL);
        try {
            client.wake(command, network.broadcastIp);
            return finish(appWidgetId, R.string.widget_status_wake_sent, false);
        } catch (IOException e) {
            Log.w(TAG, "Widget Wake-on-LAN failed", e);
            return finish(appWidgetId, R.string.widget_status_failed, true);
        }
    }

    private Result execute(RemoteClient client, HostBean target, String commandText,
                           @StringRes int successMessage, int appWidgetId) {
        if (!target.canUseSsh()) {
            return finish(appWidgetId, R.string.widget_status_ssh_not_configured, true);
        }
        RemoteCommand command = new RemoteCommand(target, RemoteCommand.SSH);
        command.command = commandText;
        HostKeyStore keys = new HostKeyStore(getApplicationContext());
        try {
            client.execute(command, keys);
            return finish(appWidgetId, successMessage, false);
        } catch (JSchException e) {
            Log.w(TAG, "Widget SSH connection failed", e);
            return finish(appWidgetId, keys.untrustedKey() == null
                    ? R.string.widget_status_failed
                    : R.string.widget_status_trust_required, true);
        } catch (IOException | IllegalStateException e) {
            Log.w(TAG, "Widget SSH command failed", e);
            return finish(appWidgetId, R.string.widget_status_failed, true);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Result.failure();
        }
    }

    private Result finish(int appWidgetId, @StringRes int message, boolean error) {
        DeviceWidgetProvider.update(getApplicationContext(), appWidgetId, message, error);
        return Result.success();
    }

    private Result finish(int appWidgetId, @StringRes int message, int color) {
        DeviceWidgetProvider.update(getApplicationContext(), appWidgetId, message, color);
        return Result.success();
    }
}
