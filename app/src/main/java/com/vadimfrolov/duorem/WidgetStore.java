package com.vadimfrolov.duorem;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.SharedPreferences;

final class WidgetStore {
    private static final String PREFERENCES = "device_widgets";
    private static final String LABEL_PREFIX = "label_";
    private static final String LEGACY_MONITOR_PREFIX = "monitor_";
    private static final String LEGACY_INTERVAL_PREFIX = "interval_";
    private static final String INTERVAL_SECONDS_PREFIX = "interval_seconds_";
    private static final String GENERATION_PREFIX = "generation_";
    static final int DEFAULT_MONITOR_INTERVAL_SECONDS = 30 * 60;

    static final class Settings {
        final String label;
        final boolean monitorEnabled;
        final int monitorIntervalSeconds;
        final int generation;

        Settings(String label, int monitorIntervalSeconds, int generation) {
            this.label = label;
            this.monitorIntervalSeconds = normalizeInterval(monitorIntervalSeconds);
            this.monitorEnabled = this.monitorIntervalSeconds > 0;
            this.generation = generation;
        }
    }

    private final SharedPreferences preferences;

    WidgetStore(Context context) {
        preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
    }

    Settings load(int appWidgetId) {
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID
                || !preferences.contains(LABEL_PREFIX + appWidgetId)) {
            return null;
        }
        String label = preferences.getString(LABEL_PREFIX + appWidgetId, "");
        String intervalKey = INTERVAL_SECONDS_PREFIX + appWidgetId;
        int intervalSeconds;
        if (preferences.contains(intervalKey)) {
            intervalSeconds = preferences.getInt(
                    intervalKey, DEFAULT_MONITOR_INTERVAL_SECONDS);
        } else if (preferences.getBoolean(LEGACY_MONITOR_PREFIX + appWidgetId, false)) {
            intervalSeconds = preferences.getInt(LEGACY_INTERVAL_PREFIX + appWidgetId, 30) * 60;
        } else {
            intervalSeconds = 0;
        }
        return new Settings(label == null ? "" : label, intervalSeconds,
                preferences.getInt(GENERATION_PREFIX + appWidgetId, 0));
    }

    boolean save(int appWidgetId, String label, int monitorIntervalSeconds) {
        int generation = preferences.getInt(GENERATION_PREFIX + appWidgetId, 0) + 1;
        return preferences.edit()
                .putString(LABEL_PREFIX + appWidgetId, label)
                .putInt(INTERVAL_SECONDS_PREFIX + appWidgetId,
                        normalizeInterval(monitorIntervalSeconds))
                .putInt(GENERATION_PREFIX + appWidgetId, generation)
                .remove(LEGACY_MONITOR_PREFIX + appWidgetId)
                .remove(LEGACY_INTERVAL_PREFIX + appWidgetId)
                .commit();
    }

    void delete(int appWidgetId) {
        preferences.edit()
                .remove(LABEL_PREFIX + appWidgetId)
                .remove(LEGACY_MONITOR_PREFIX + appWidgetId)
                .remove(LEGACY_INTERVAL_PREFIX + appWidgetId)
                .remove(INTERVAL_SECONDS_PREFIX + appWidgetId)
                .remove(GENERATION_PREFIX + appWidgetId)
                .apply();
    }

    static int normalizeInterval(int seconds) {
        switch (seconds) {
            case 0:
            case 30:
            case 60:
            case 5 * 60:
            case 10 * 60:
            case 30 * 60:
            case 60 * 60:
            case 2 * 60 * 60:
                return seconds;
            default:
                return DEFAULT_MONITOR_INTERVAL_SECONDS;
        }
    }
}
