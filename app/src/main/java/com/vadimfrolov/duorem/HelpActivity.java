/*
 * Copyright (C) 2017 Vadim Frolov
 * Licensed under GNU's GPL 3 or any later version, see README
 */

package com.vadimfrolov.duorem;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.appcompat.widget.Toolbar;

public class HelpActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);

        setSupportActionBar((Toolbar) findViewById(R.id.main_toolbar));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.help_title);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        findViewById(R.id.report_issue).setOnClickListener(view ->
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.issues_link)))));
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
