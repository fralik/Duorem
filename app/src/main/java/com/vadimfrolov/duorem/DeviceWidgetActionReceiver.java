package com.vadimfrolov.duorem;

import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

public final class DeviceWidgetActionReceiver extends BroadcastReceiver {
    static final String ACTION_POWER = "com.vadimfrolov.duorem.action.WIDGET_POWER";
    static final String ACTION_RESTART = "com.vadimfrolov.duorem.action.WIDGET_RESTART";
    static final String INPUT_ACTION = "widget_action";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (!ACTION_POWER.equals(action) && !ACTION_RESTART.equals(action)) return;
        int appWidgetId = intent.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID
                || new WidgetStore(context).load(appWidgetId) == null) {
            return;
        }
        DeviceWidgetProvider.update(context, appWidgetId, R.string.widget_status_working, false);
        Data input = new Data.Builder()
                .putInt(DeviceWidgetProvider.INPUT_WIDGET_ID, appWidgetId)
                .putString(INPUT_ACTION, action)
                .build();
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(DeviceWidgetWorker.class)
                .setInputData(input)
                .build();
        WorkManager.getInstance(context).enqueueUniqueWork(
                DeviceWidgetProvider.workName(appWidgetId),
                ExistingWorkPolicy.REPLACE, request);
    }
}
