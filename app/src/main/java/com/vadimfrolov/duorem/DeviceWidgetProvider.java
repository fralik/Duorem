package com.vadimfrolov.duorem;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.RemoteViews;

import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import androidx.work.WorkManager;

public final class DeviceWidgetProvider extends AppWidgetProvider {
    static final String INPUT_WIDGET_ID = "widget_id";

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (Intent.ACTION_CONFIGURATION_CHANGED.equals(intent.getAction())) {
            AppWidgetManager manager = AppWidgetManager.getInstance(context);
            int[] ids = manager.getAppWidgetIds(
                    new ComponentName(context, DeviceWidgetProvider.class));
            onUpdate(context, manager, ids);
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            update(context, appWidgetId, R.string.widget_status_ready, false);
        }
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        WidgetStore store = new WidgetStore(context);
        WorkManager workManager = WorkManager.getInstance(context);
        for (int appWidgetId : appWidgetIds) {
            store.delete(appWidgetId);
            workManager.cancelUniqueWork(workName(appWidgetId));
        }
    }

    static void update(Context context, int appWidgetId,
                       @StringRes int statusText, boolean error) {
        WidgetStore.Settings settings = new WidgetStore(context).load(appWidgetId);
        if (settings == null) return;
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.device_widget);
        views.setTextViewText(R.id.widget_label, settings.label);
        views.setTextViewText(R.id.widget_status, context.getString(statusText));
        views.setTextColor(R.id.widget_status, ContextCompat.getColor(context,
                error ? R.color.statusOffline : R.color.widgetStatus));
        String description = context.getString(R.string.widget_content_description,
                settings.label, context.getString(statusText));
        views.setContentDescription(R.id.widget_root, description);

        PendingIntent power = actionIntent(context, appWidgetId,
                DeviceWidgetActionReceiver.ACTION_POWER, appWidgetId * 2);
        PendingIntent restart = actionIntent(context, appWidgetId,
                DeviceWidgetActionReceiver.ACTION_RESTART, appWidgetId * 2 + 1);
        views.setOnClickPendingIntent(R.id.widget_power, power);
        views.setOnClickPendingIntent(R.id.widget_restart, restart);
        views.setContentDescription(R.id.widget_power, context.getString(
                R.string.widget_power_content_description, settings.label));
        views.setContentDescription(R.id.widget_restart, context.getString(
                R.string.widget_restart_content_description, settings.label));
        AppWidgetManager.getInstance(context).updateAppWidget(appWidgetId, views);
    }

    private static PendingIntent actionIntent(Context context, int appWidgetId,
                                              String action, int requestCode) {
        Intent run = new Intent(context, DeviceWidgetActionReceiver.class)
                .setAction(action)
                .setData(Uri.parse("duorem://widget/" + appWidgetId + "/" + action))
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        return PendingIntent.getBroadcast(context, requestCode, run,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    static String workName(int appWidgetId) {
        return "device-widget-" + appWidgetId;
    }
}
