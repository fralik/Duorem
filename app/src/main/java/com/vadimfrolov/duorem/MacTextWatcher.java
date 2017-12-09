package com.vadimfrolov.duorem;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;

/**
 * Provides the ability to switch view focus automatically after user entered two characters
 * in the edit box. It is used to enter remote host MAC address.
 */

public final class MacTextWatcher implements TextWatcher {

    private View mView = null;

    public MacTextWatcher(View nextFocus) {
        mView = nextFocus;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

    }

    @Override
    public void afterTextChanged(Editable s) {
        if (s.toString().length() == 2) {
            mView.requestFocus();
        }
    }
}
