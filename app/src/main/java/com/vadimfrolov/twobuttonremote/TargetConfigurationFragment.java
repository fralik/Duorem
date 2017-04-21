/*
 * Copyright (C) 2017 Vadim Frolov
 * Licensed under GNU's GPL 2, see README
 */

package com.vadimfrolov.twobuttonremote;


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.TextInputEditText;
import android.support.v4.app.Fragment;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;

import com.google.gson.Gson;
import com.vadimfrolov.twobuttonremote.Network.HostBean;
import com.vadimfrolov.twobuttonremote.Network.NetInfo;

import java.util.ArrayList;
import java.util.List;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link TargetConfigurationFragment#newInstance} factory method to
 * create an instance of this fragment.
 *
 * This class should get host information, i.e. HostBean
 */
public class TargetConfigurationFragment extends Fragment {
    static final String ARG_BEAN = "com.vadimfrolov.TwoButtonRemote.TargetConfigurationFragment.bean";

    private HostBean mHostBean;

    private TextInputEditText mEditHostname;
    private TextInputEditText mEditIpAddress;
    private TextInputEditText mEditBroadcastAddress;
    private View mViewBroadcastLayout;
    private Button mBtnGuessBroadcast;
    private Switch mSwitchAdvanced;
    private TextInputEditText mEditWolPort;
    private View mViewWolLayout;
    private TextInputEditText mEditSshUsername;
    private TextInputEditText mEditSshPassword;
    private TextInputEditText mEditSshPort;
    private List<EditText> mEditMac;
    private boolean mIsTablet = false;
    SharedPreferences mPrefs;
    Context mContext;

    public TargetConfigurationFragment() {
        // Required empty public constructor
        mEditMac = new ArrayList<>();
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param host Parameter 1.
     * @return A new instance of fragment TargetConfigurationFragment.
     */
    public static TargetConfigurationFragment newInstance(HostBean host) {
        TargetConfigurationFragment fragment = new TargetConfigurationFragment();
        Bundle args = new Bundle();
        args.putParcelable(HostBean.EXTRA, host);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            mHostBean = args.getParcelable(HostBean.EXTRA);
        }

        mContext = (Context) getActivity();
        mPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            mHostBean = savedInstanceState.getParcelable(HostBean.EXTRA);
        }

        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_target_configuration, container, false);
        setHasOptionsMenu(true);

        mEditHostname = (TextInputEditText) v.findViewById(R.id.edit_hostname);
        mEditIpAddress = (TextInputEditText) v.findViewById(R.id.edit_ip_address);
        mEditBroadcastAddress = (TextInputEditText) v.findViewById(R.id.edit_broadcast_address);
        mViewBroadcastLayout = v.findViewById(R.id.input_layout_broadcast_address);
        mBtnGuessBroadcast = (Button) v.findViewById(R.id.btn_get_broadcast);
        mEditWolPort = (TextInputEditText)v.findViewById(R.id.edit_wol_port);
        mViewWolLayout = v.findViewById(R.id.input_layout_wol_port);
        mEditSshUsername = (TextInputEditText) v.findViewById(R.id.edit_ssh_username);
        mEditSshPassword = (TextInputEditText)v.findViewById(R.id.edit_ssh_password);
        mEditSshPort = (TextInputEditText) v.findViewById(R.id.edit_ssh_port);
        mSwitchAdvanced = (Switch) v.findViewById(R.id.switch_advanced);

        final Button btnSave = (Button)v.findViewById(R.id.btn_save);
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mHostBean.hostname = mEditHostname.getText().toString();
                mHostBean.ipAddress = mEditIpAddress.getText().toString();
                String broadcastIp = mEditBroadcastAddress.getText().toString();
                if (!broadcastIp.equals(NetInfo.NOIP)) {
                    mHostBean.broadcastIp = mEditBroadcastAddress.getText().toString();
                }
                mHostBean.wolPort = mEditWolPort.getText().toString();
                mHostBean.hardwareAddress = fields2Mac();
                mHostBean.sshUsername = mEditSshUsername.getText().toString();
                mHostBean.sshPassword = mEditSshPassword.getText().toString();
                mHostBean.sshPort = mEditSshPort.getText().toString();

                saveTargetToSettings();

                Intent startMain = new Intent(mContext, MainActivity.class);
                startMain.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(startMain);
            }
        });
//        btnSave.setFocusable(true);
//        btnSave.setFocusableInTouchMode(true);

        EditText mac = (EditText)v.findViewById(R.id.mac_1);
        mEditMac.add(mac);
        mac = (EditText)v.findViewById(R.id.mac_2);
        mEditMac.add(mac);
        mac = (EditText)v.findViewById(R.id.mac_3);
        mEditMac.add(mac);
        mac = (EditText)v.findViewById(R.id.mac_4);
        mEditMac.add(mac);
        mac = (EditText)v.findViewById(R.id.mac_5);
        mEditMac.add(mac);
        mac = (EditText)v.findViewById(R.id.mac_6);
        mEditMac.add(mac);

        mBtnGuessBroadcast.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NetInfo ni = new NetInfo(mContext);
                String ip = mEditIpAddress.getText().toString();
                String broadcastIp = NetInfo.getBroadcastFromIpAndCidr(ip, ni.cidr);
                mEditBroadcastAddress.setText(broadcastIp);
            }
        });

        mSwitchAdvanced.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                setAdvancedVisibility(isChecked);
            }
        });

        return v;
    }

    public void resetAppBar() {
        // Create action bar as a toolbar
        AppCompatActivity act = (AppCompatActivity)getActivity();
        Toolbar toolbar = (Toolbar) act.findViewById(R.id.main_toolbar);
        act.setSupportActionBar(toolbar);
        if (toolbar != null) {
            act.getSupportActionBar().setTitle(getResources().getString(R.string.edit_host));
            // Add Up Navigation, part 1
            act.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void setAdvancedVisibility(boolean visible) {
        int visibility = visible ? View.VISIBLE : View.GONE;

        mViewBroadcastLayout.setVisibility(visibility);
        mBtnGuessBroadcast.setVisibility(visibility);
        mViewWolLayout.setVisibility(visibility);
    }

    private void saveTargetToSettings() {
        if (mHostBean.broadcastIp == null) {
            // host added manually, let's try to find out broadcast address
            NetInfo ni = new NetInfo(mContext);
            mHostBean.broadcastIp = NetInfo.getBroadcastFromIpAndCidr(mHostBean.ipAddress, ni.cidr);
        }
        Gson gson = new Gson();
        String targetJson = gson.toJson(mHostBean);

        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putString(MainActivity.KEY_PREF_TARGET, targetJson);
        editor.commit();
    }

    @Override
    public void onStart() {
        super.onStart();
        refreshView();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // Add Up Navigation, part 2 (final)
                NavUtils.navigateUpFromSameTask(getActivity());
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void updateTarget(HostBean item) {
        mHostBean = item;
        NetInfo ni = new NetInfo(mContext);

        refreshView();
    }

    // disable action bar
    public void prepareForTablet() {
        mIsTablet = true;
    }

    private void refreshView() {
        if (mHostBean == null) {
            mHostBean = new HostBean();
            mHostBean.resetForView();
        }
        mEditHostname.setText(mHostBean.hostname);
        mEditIpAddress.setText(mHostBean.ipAddress);
        mEditBroadcastAddress.setText(mHostBean.broadcastIp);
        mEditWolPort.setText(mHostBean.wolPort);
        mac2Fields(mHostBean.hardwareAddress);

        mEditSshUsername.setText(mHostBean.sshUsername);
        mEditSshPassword.setText(mHostBean.sshPassword);
        mEditSshPort.setText(mHostBean.sshPort);
    }

    private void mac2Fields(String macAddress) {
        String[] parts = macAddress.split(":");
        if (parts.length == 0 || mEditMac.isEmpty()) {
            return;
        }
        for (int i = 0; i < parts.length; i++) {
            mEditMac.get(i).setText(parts[i].toUpperCase());
        }
    }

    private String fields2Mac() {
        String result = "";
        for (EditText mac : mEditMac) {
            result = result + mac.getText().toString() + ":";
        }
        if (result.length() > 1) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }
}
