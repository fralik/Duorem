package com.vadimfrolov.duorem;

import android.view.View;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public class MinHeightMatcher extends TypeSafeMatcher<View> {

    private final int _minHeight;

    public MinHeightMatcher(int  minHeight) {
        super(View.class);
        this._minHeight = minHeight;
    }

    @Override
    protected boolean matchesSafely(View target) {
        return target.getHeight() >= _minHeight;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("with minHeight: ");
        description.appendValue(_minHeight);
    }
}