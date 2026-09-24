package com.vadimfrolov.duorem;

import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.vadimfrolov.duorem.Network.HostBean;

import java.io.IOException;
import java.security.GeneralSecurityException;

public final class WidgetConfigureActivity extends BaseActivity {
    private int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    private EditText label;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setResult(RESULT_CANCELED);
        setContentView(R.layout.activity_widget_configure);
        appWidgetId = getIntent().getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
            return;
        }

        label = findViewById(R.id.widget_label_input);
        TextView device = findViewById(R.id.widget_device);
        TextView warning = findViewById(R.id.widget_warning);
        Button save = findViewById(R.id.widget_save);
        findViewById(R.id.widget_cancel).setOnClickListener(view -> finish());

        HostBean target;
        try {
            target = new HostStore(this).load();
        } catch (IOException | GeneralSecurityException e) {
            target = null;
            warning.setText(R.string.settings_load_failed);
        }
        if (target == null) {
            device.setText(R.string.no_device);
            warning.setVisibility(View.VISIBLE);
            if (warning.getText().length() == 0) {
                warning.setText(R.string.widget_configure_device_first);
            }
            save.setEnabled(false);
            return;
        }

        device.setText(target.name());
        if (!target.canWake() && !target.canUseSsh()) {
            warning.setText(R.string.widget_configure_device_incomplete);
            warning.setVisibility(View.VISIBLE);
            save.setEnabled(false);
            return;
        }
        WidgetStore.Settings existing = new WidgetStore(this).load(appWidgetId);
        label.setText(existing == null ? getString(R.string.app_name) : existing.label);
        save.setOnClickListener(view -> saveWidget());
    }

    private void saveWidget() {
        String value = label.getText().toString().trim();
        if (value.isEmpty()) {
            label.setError(getString(R.string.widget_label_required));
            return;
        }
        if (!new WidgetStore(this).save(appWidgetId, value)) {
            label.setError(getString(R.string.widget_settings_save_failed));
            return;
        }
        DeviceWidgetProvider.update(this, appWidgetId, R.string.widget_status_ready, false);
        Intent result = new Intent()
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        setResult(RESULT_OK, result);
        finish();
    }
}
