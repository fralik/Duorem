/*
 * Copyright (C) 2017 Vadim Frolov
 * Licensed under GNU's GPL 3 or any later version, see README
 */

package com.vadimfrolov.duorem;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

public class AboutDialog extends DialogFragment {
    public AboutDialog() {

    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (getDialog() != null) {
            getDialog().setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.ic_launcher);
        }
    }

    public static AboutDialog newInstance(String title, String version) {
        AboutDialog dlg = new AboutDialog();
        Bundle args = new Bundle();
        args.putString("title", title);
        args.putString("version", version);
        dlg.setArguments(args);
        return dlg;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        getDialog().requestWindowFeature(Window.FEATURE_LEFT_ICON);
        return inflater.inflate(R.layout.about, container);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView viewVersion = (TextView) view.findViewById(R.id.version);
        String title = getArguments().getString("title");
        getDialog().setTitle(title);

        String version = getArguments().getString("version");
        viewVersion.setText(version);

        Button btnOk = (Button) view.findViewById(R.id.ok_button);
        if (btnOk != null) {
            btnOk.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dismiss();
                }
            });
        }
    }

}
