package com.vadimfrolov.duorem;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.view.View;

import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class PortraitModeVisibilityTest {

    public static Matcher<View> withMinHeight(final int minHeight) {
        return new MinHeightMatcher(minHeight);
    }

    @Rule
    public ActivityTestRule<MainActivity> mActivityRule = new ActivityTestRule<>(MainActivity.class);

    @Test
    public void minHeight() {
        onView(withId(R.id.btn_toggle_power)).check(matches(withMinHeight(100)));
        onView(withId(R.id.btn_restart)).check(matches(withMinHeight(100)));
        onView(withId(R.id.layout_host_info)).check(matches(withMinHeight(8)));
    }
}
