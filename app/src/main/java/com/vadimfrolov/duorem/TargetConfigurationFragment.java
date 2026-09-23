/*
 * Copyright (C) 2017 Vadim Frolov
 * Licensed under GNU's GPL 3 or any later version, see README
 */
package com.vadimfrolov.duorem;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Switch;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.NavUtils;
import androidx.fragment.app.Fragment;

import com.vadimfrolov.duorem.Network.HostBean;
import com.vadimfrolov.duorem.Network.NetInfo;
import com.vadimfrolov.duorem.Network.NetworkAddress;
import com.vadimfrolov.duorem.Network.WakeOnLan;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TargetConfigurationFragment extends Fragment {
    private static final String ARG_BEAN = "com.vadimfrolov.Duorem.TargetConfigurationFragment.bean";
    private HostBean host;
    private EditText hostname;
    private EditText ip;
    private EditText broadcast;
    private EditText wolPort;
    private EditText username;
    private EditText password;
    private EditText sshPort;
    private EditText shutdown;
    private Switch advanced;
    private View commandLayout;
    private final List<EditText> macFields = new ArrayList<>();
    private boolean updatingMac;

    public static TargetConfigurationFragment newInstance(HostBean host) {
        TargetConfigurationFragment fragment = new TargetConfigurationFragment();
        Bundle args = new Bundle();
        args.putParcelable(HostBean.EXTRA, host);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        setHasOptionsMenu(true);
        if (state != null) {
            host = state.getParcelable(ARG_BEAN);
        } else if (getArguments() != null) {
            host = getArguments().getParcelable(HostBean.EXTRA);
        }
        if (host == null) host = new HostBean();
        host.normalize();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle state) {
        View view = inflater.inflate(R.layout.fragment_target_configuration, container, false);
        hostname = view.findViewById(R.id.edit_hostname);
        ip = view.findViewById(R.id.edit_ip_address);
        broadcast = view.findViewById(R.id.edit_broadcast_address);
        wolPort = view.findViewById(R.id.edit_wol_port);
        username = view.findViewById(R.id.edit_ssh_username);
        password = view.findViewById(R.id.edit_ssh_password);
        sshPort = view.findViewById(R.id.edit_ssh_port);
        shutdown = view.findViewById(R.id.edit_shutdown_cmd);
        advanced = view.findViewById(R.id.switch_advanced);
        commandLayout = view.findViewById(R.id.input_layout_shutdown_cmd);
        macFields.clear();
        for (int id : new int[]{R.id.mac_1, R.id.mac_2, R.id.mac_3, R.id.mac_4, R.id.mac_5, R.id.mac_6}) {
            macFields.add(view.findViewById(id));
        }
        refreshView();
        for (int index = 0; index < macFields.size(); index++) {
            final int position = index;
            macFields.get(index).addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override
                public void afterTextChanged(Editable value) {
                    if (updatingMac) return;
                    if (WakeOnLan.isValidMac(value.toString())) {
                        setMac(value.toString());
                        broadcast.requestFocus();
                    } else if (value.length() > 2) {
                        value.delete(2, value.length());
                    } else if (value.length() == 2 && macFields.get(position).hasFocus()) {
                        if (position == 5) broadcast.requestFocus();
                        else macFields.get(position + 1).requestFocus();
                    }
                }
            });
        }
        advanced.setOnCheckedChangeListener((button, checked) ->
                commandLayout.setVisibility(checked ? View.VISIBLE : View.GONE));
        view.findViewById(R.id.btn_get_broadcast).setOnClickListener(button -> {
            NetInfo network = new NetInfo(requireContext());
            String address = text(ip);
            if (address.isEmpty() || NetInfo.NOIP.equals(address)) address = network.ip;
            if (!NetworkAddress.isIpv4(address) || NetInfo.NOIP.equals(network.ip)) {
                broadcast.setError(getString(R.string.broadcast_guess_failed));
                return;
            }
            broadcast.setText(NetInfo.getBroadcastFromIpAndCidr(address, network.cidr));
        });
        return view;
    }

    @Override
    public void onViewStateRestored(Bundle state) {
        super.onViewStateRestored(state);
        commandLayout.setVisibility(advanced.isChecked() ? View.VISIBLE : View.GONE);
    }

    public void resetAppBar() {
        AppCompatActivity activity = (AppCompatActivity) requireActivity();
        activity.setSupportActionBar((Toolbar) activity.findViewById(R.id.main_toolbar));
        if (activity.getSupportActionBar() != null) {
            activity.getSupportActionBar().setTitle(R.string.edit_host);
            activity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    public void updateTarget(HostBean item) {
        host = item == null ? new HostBean() : item;
        host.normalize();
        if (getView() != null) refreshView();
    }

    private void refreshView() {
        hostname.setText(host.hostname);
        ip.setText(host.ipAddress);
        broadcast.setText(host.broadcastIp);
        wolPort.setText(host.wolPort);
        username.setText(host.sshUsername);
        password.setText(host.sshPassword);
        sshPort.setText(host.sshPort);
        shutdown.setText(host.sshShutdownCmd);
        setMac(host.hardwareAddress);
    }

    private void setMac(String mac) {
        updatingMac = true;
        String[] parts = (WakeOnLan.isValidMac(mac) ? mac : NetInfo.NOMAC).split(":");
        for (int i = 0; i < 6; i++) macFields.get(i).setText(parts[i].toUpperCase(Locale.ROOT));
        updatingMac = false;
    }

    private static String text(EditText field) {
        return field.getText().toString().trim();
    }

    private void readFields() {
        host.hostname = text(hostname);
        host.ipAddress = text(ip);
        host.broadcastIp = text(broadcast);
        host.wolPort = text(wolPort);
        host.sshPort = text(sshPort);
        host.sshUsername = text(username);
        host.sshPassword = password.getText().toString();
        host.sshShutdownCmd = text(shutdown);
        StringBuilder mac = new StringBuilder();
        for (EditText field : macFields) {
            if (mac.length() > 0) mac.append(':');
            mac.append(text(field));
        }
        host.hardwareAddress = mac.toString();
    }

    private boolean validate() {
        boolean valid = true;
        for (EditText address : new EditText[]{ip, broadcast}) {
            address.setError(null);
            if (!text(address).isEmpty() && !NetworkAddress.isIpv4(text(address))) {
                address.setError(getString(R.string.invalid_ip));
                valid = false;
            }
        }
        for (EditText port : new EditText[]{wolPort, sshPort}) {
            port.setError(null);
            try {
                NetworkAddress.port(text(port));
            } catch (IllegalArgumentException e) {
                port.setError(getString(R.string.invalid_port));
                valid = false;
            }
        }
        macFields.get(0).setError(null);
        if (!WakeOnLan.isValidMac(host.hardwareAddress)) {
            macFields.get(0).setError(getString(R.string.invalid_mac));
            valid = false;
        }
        if (valid) {
            host.normalize();
            if (!host.canWake() && !host.hasAddress()) {
                ip.setError(getString(R.string.target_required));
                valid = false;
            }
        }
        return valid;
    }

    @Override
    public void onSaveInstanceState(Bundle state) {
        if (getView() != null) readFields();
        state.putParcelable(ARG_BEAN, host);
        super.onSaveInstanceState(state);
    }

    @Override
    public void onDestroyView() {
        readFields();
        macFields.clear();
        hostname = ip = broadcast = wolPort = username = password = sshPort = shutdown = null;
        advanced = null;
        commandLayout = null;
        super.onDestroyView();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.save_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            NavUtils.navigateUpFromSameTask(requireActivity());
            return true;
        }
        if (item.getItemId() == R.id.action_save) {
            readFields();
            if (!validate()) return true;
            try {
                new HostStore(requireContext()).save(host);
            } catch (IOException | GeneralSecurityException e) {
                Log.e("TargetConfiguration", "Could not save device", e);
                new AlertDialog.Builder(requireContext()).setMessage(R.string.settings_save_failed)
                        .setPositiveButton(android.R.string.ok, null).show();
                return true;
            }
            startActivity(new Intent(requireContext(), MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
