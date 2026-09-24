package com.vadimfrolov.duorem;

import android.content.Context;

import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

final class WidgetMonitorScheduler {
    private WidgetMonitorScheduler() {
    }

    static void configure(Context context, int appWidgetId, WidgetStore.Settings settings) {
        cancel(context, appWidgetId);
        if (!settings.monitorEnabled) {
            return;
        }
        OneTimeWorkRequest immediate = new OneTimeWorkRequest.Builder(DeviceWidgetWorker.class)
                .setInputData(refreshInput(appWidgetId, settings.generation))
                .addTag(tag(appWidgetId))
                .build();
        WorkManager.getInstance(context).enqueueUniqueWork(chainName(appWidgetId),
                ExistingWorkPolicy.REPLACE, immediate);
    }

    static void scheduleNext(Context context, int appWidgetId, WidgetStore.Settings settings) {
        OneTimeWorkRequest next = new OneTimeWorkRequest.Builder(DeviceWidgetWorker.class)
                .setInputData(refreshInput(appWidgetId, settings.generation))
                .setInitialDelay(settings.monitorIntervalSeconds, TimeUnit.SECONDS)
                .addTag(tag(appWidgetId))
                .build();
        WorkManager.getInstance(context).enqueueUniqueWork(chainName(appWidgetId),
                ExistingWorkPolicy.APPEND_OR_REPLACE, next);
    }

    static void cancel(Context context, int appWidgetId) {
        WorkManager manager = WorkManager.getInstance(context);
        manager.cancelAllWorkByTag(tag(appWidgetId));
        manager.cancelUniqueWork(chainName(appWidgetId));
        manager.cancelUniqueWork(legacyPeriodicName(appWidgetId));
        manager.cancelUniqueWork(legacyImmediateName(appWidgetId));
    }

    private static Data refreshInput(int appWidgetId, int generation) {
        return new Data.Builder()
                .putInt(DeviceWidgetProvider.INPUT_WIDGET_ID, appWidgetId)
                .putInt(DeviceWidgetWorker.INPUT_MONITOR_GENERATION, generation)
                .putString(DeviceWidgetActionReceiver.INPUT_ACTION,
                        DeviceWidgetWorker.ACTION_REFRESH)
                .build();
    }

    private static String chainName(int appWidgetId) {
        return "device-widget-monitor-v2-" + appWidgetId;
    }

    private static String tag(int appWidgetId) {
        return "device-widget-monitor-tag-" + appWidgetId;
    }

    private static String legacyPeriodicName(int appWidgetId) {
        return "device-widget-monitor-" + appWidgetId;
    }

    private static String legacyImmediateName(int appWidgetId) {
        return "device-widget-monitor-now-" + appWidgetId;
    }
}
