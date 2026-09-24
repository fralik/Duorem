/*
 * Copyright (C) 2017 Vadim Frolov
 * Licensed under GNU's GPL 3 or any later version, see README
 */

package com.vadimfrolov.duorem;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

public class AboutDialog extends DialogFragment {
    public AboutDialog() {

    }

    public static AboutDialog newInstance(String title, String version) {
        AboutDialog dlg = new AboutDialog();
        Bundle args = new Bundle();
        args.putString("title", title);
        args.putString("version", version);
        dlg.setArguments(args);
        return dlg;
    }

    @NonNull
    @Override
    public android.app.Dialog onCreateDialog(Bundle savedInstanceState) {
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.about, null);
        TextView viewVersion = view.findViewById(R.id.version);
        Bundle arguments = requireArguments();
        viewVersion.setText(arguments.getString("version"));

        return new AlertDialog.Builder(requireContext())
                .setTitle(arguments.getString("title"))
                .setIcon(R.drawable.ic_launcher)
                .setView(view)
                .setPositiveButton(R.string.close, null)
                .create();
    }
}
