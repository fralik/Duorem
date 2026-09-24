package com.vadimfrolov.duorem;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.SharedPreferences;

final class WidgetStore {
    private static final String PREFERENCES = "device_widgets";
    private static final String LABEL_PREFIX = "label_";

    static final class Settings {
        final String label;

        Settings(String label) {
            this.label = label;
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
        return new Settings(label == null ? "" : label);
    }

    boolean save(int appWidgetId, String label) {
        return preferences.edit()
                .putString(LABEL_PREFIX + appWidgetId, label)
                .commit();
    }

    void delete(int appWidgetId) {
        preferences.edit()
                .remove(LABEL_PREFIX + appWidgetId)
                .apply();
    }
}
