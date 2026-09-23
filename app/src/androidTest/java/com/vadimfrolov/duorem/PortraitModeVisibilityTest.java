package com.vadimfrolov.duorem;

import org.hamcrest.Matcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.filters.LargeTest;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import android.view.View;
import androidx.appcompat.widget.Toolbar;

import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class PortraitModeVisibilityTest {

    public static Matcher<View> withMinHeight(final int minHeight) {
        return new MinHeightMatcher(minHeight);
    }

    @Rule
    public ActivityScenarioRule<MainActivity> mActivityRule = new ActivityScenarioRule<>(MainActivity.class);

    @Test
    public void minHeight() {
        onView(withId(R.id.btn_toggle_power)).check(matches(withMinHeight(100)));
        onView(withId(R.id.btn_restart)).check(matches(withMinHeight(100)));
        onView(withId(R.id.layout_host_info)).check(matches(withMinHeight(8)));
    }

    @Test
    public void aboutDialogOpensWithCurrentVersion() {
        mActivityRule.getScenario().onActivity(activity -> {
            Toolbar toolbar = activity.findViewById(R.id.main_toolbar);
            activity.onOptionsItemSelected(toolbar.getMenu().findItem(R.id.action_about));
        });
        onView(withText("1.2.0")).check(matches(isDisplayed()));
    }
}
