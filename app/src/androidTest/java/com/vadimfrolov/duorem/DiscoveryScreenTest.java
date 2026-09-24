package com.vadimfrolov.duorem;

import android.widget.EditText;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class DiscoveryScreenTest {
    @Test
    public void manualConfigurationRemainsAvailableAndTabletDefaultsArePopulated() {
        try (ActivityScenario<SearchConfigureActivity> scenario = ActivityScenario.launch(SearchConfigureActivity.class)) {
            onView(withText(R.string.hosts_manual)).check(matches(isDisplayed()));
            scenario.onActivity(activity -> {
                EditText sshPort = activity.findViewById(R.id.edit_ssh_port);
                if (activity.getResources().getConfiguration().smallestScreenWidthDp >= 600) {
                    assertNotNull(sshPort);
                    assertEquals("22", sshPort.getText().toString());
                    ((EditText) activity.findViewById(R.id.edit_hostname)).setText("tablet draft");
                } else {
                    assertNull(sshPort);
                }
            });
            scenario.recreate();
            scenario.onActivity(activity -> {
                if (activity.getResources().getConfiguration().smallestScreenWidthDp >= 600) {
                    assertEquals("tablet draft",
                            ((EditText) activity.findViewById(R.id.edit_hostname)).getText().toString());
                }
            });
        }
    }
}
