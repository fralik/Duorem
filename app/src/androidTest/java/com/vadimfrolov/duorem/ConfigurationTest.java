package com.vadimfrolov.duorem;

import android.content.Intent;
import android.widget.EditText;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.vadimfrolov.duorem.Network.HostBean;

import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class ConfigurationTest {
    private Intent intent() {
        HostBean host = new HostBean();
        host.hostname = "original";
        host.hardwareAddress = "01:23:45:67:89:AB";
        return new Intent(InstrumentationRegistry.getInstrumentation().getTargetContext(),
                TargetConfigurationActivity.class).putExtra(HostBean.EXTRA, host);
    }

    @Test
    public void unsavedEditsSurviveActivityRecreation() {
        try (ActivityScenario<TargetConfigurationActivity> scenario = ActivityScenario.launch(intent())) {
            scenario.onActivity(activity -> {
                ((EditText) activity.findViewById(R.id.edit_hostname)).setText("edited");
                ((EditText) activity.findViewById(R.id.edit_ssh_password)).setText("unsaved-password");
                ((EditText) activity.findViewById(R.id.edit_shutdown_cmd)).setText("custom command");
                ((EditText) activity.findViewById(R.id.edit_wol_port)).setText("7");
            });
            scenario.recreate();
            scenario.onActivity(activity -> {
                assertEquals("edited", ((EditText) activity.findViewById(R.id.edit_hostname)).getText().toString());
                assertEquals("unsaved-password", ((EditText) activity.findViewById(R.id.edit_ssh_password)).getText().toString());
                assertEquals("custom command", ((EditText) activity.findViewById(R.id.edit_shutdown_cmd)).getText().toString());
                assertEquals("7", ((EditText) activity.findViewById(R.id.edit_wol_port)).getText().toString());
            });
        }
    }

    @Test
    public void fullMacPasteFillsAllSixFields() {
        try (ActivityScenario<TargetConfigurationActivity> scenario = ActivityScenario.launch(intent())) {
            scenario.onActivity(activity -> {
                ((EditText) activity.findViewById(R.id.mac_3)).setText("AB:CD:EF:12:34:56");
                assertEquals("AB", ((EditText) activity.findViewById(R.id.mac_1)).getText().toString());
                assertEquals("EF", ((EditText) activity.findViewById(R.id.mac_3)).getText().toString());
                assertEquals("56", ((EditText) activity.findViewById(R.id.mac_6)).getText().toString());
            });
        }
    }

    @Test
    public void invalidPortKeepsEditorOpenWithAnError() {
        try (ActivityScenario<TargetConfigurationActivity> scenario = ActivityScenario.launch(intent())) {
            scenario.onActivity(activity ->
                    ((EditText) activity.findViewById(R.id.edit_ssh_port)).setText("65536"));
            onView(withId(R.id.action_save)).perform(click());
            scenario.onActivity(activity -> {
                assertFalse(activity.isFinishing());
                assertNotNull(((EditText) activity.findViewById(R.id.edit_ssh_port)).getError());
            });
        }
    }
}
