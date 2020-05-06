/*
 * Copyright (C) 2017 Vadim Frolov
 * Licensed under GNU's GPL 3 or any later version, see README
 */

package com.vadimfrolov.duorem;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputEditText;
import android.support.v4.app.Fragment;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.Toast;

import com.google.gson.Gson;
import com.vadimfrolov.duorem.Network.HostBean;
import com.vadimfrolov.duorem.Network.NetInfo;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link TargetConfigurationFragment#newInstance} factory method to
 * create an instance of this fragment.
 *
 * This class should get host information, i.e. HostBean
 */
public class TargetConfigurationFragment extends Fragment {
    static final String ARG_BEAN = "com.vadimfrolov.Duorem.TargetConfigurationFragment.bean";
    private final String TAG = "TargetConfigurationFrag";

    private HostBean mHostBean;

    private TextInputEditText mEditHostname;
    private TextInputEditText mEditIpAddress;
    private TextInputEditText mEditBroadcastAddress;
    private View mViewBroadcastLayout;
    private Button mBtnGuessBroadcast;
    private Switch mSwitchAdvanced;
    private TextInputEditText mEditWolPort;
    private View mViewWolLayout;
    private Switch mSwitchPlatform;
    private TextInputEditText mEditSshUsername;
    private TextInputEditText mEditSshPassword;
    private TextInputEditText mEditSshPort;
    private List<EditText> mEditMac;
    private View mViewShutdownCmd;
    private TextInputEditText mEditShutdownCmd;
    private Button mBtnLoadKey;
    private LinearLayout mLayoutWindows;
    private LinearLayout mLayoutSsh;
    private EditText mEditSshKey;

    private boolean mIsTablet = false;
    SharedPreferences mPrefs;
    Context mContext;

    // A request code's purpose is to match the result of a "startActivityForResult" with
    // the type of the original request.  Choose any value.
    private static final int SELECT_KEY_REQUEST = 1337;

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
        mSwitchPlatform = (Switch) v.findViewById(R.id.switch_platform);
        mBtnLoadKey = (Button) v.findViewById(R.id.btn_select_key);
        mEditSshKey = (EditText) v.findViewById(R.id.edit_ssh_key);

        mEditShutdownCmd = (TextInputEditText) v.findViewById(R.id.edit_shutdown_cmd);
        mViewShutdownCmd = v.findViewById(R.id.input_layout_shutdown_cmd);

        mLayoutSsh = (LinearLayout) v.findViewById(R.id.layout_ssh);
        mLayoutWindows = (LinearLayout) v.findViewById(R.id.layout_windows);

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

        // This class does three things:
        // 1. Accepts full MAC address when user pastes it from the clipboard.
        // 2. Prevents entering of more than two characters in any of the MAC fields.
        // 3. Moves focus when user enters two characters in any of the MAC fields.
        TextWatcher macTextWatcher = new TextWatcher() {
            private boolean mIsPasting = false;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (count == NetInfo.NOMAC.length() && s.toString().contains(":")) {
                    String mac = s.toString();
                    String[] parts = mac.split(":");
                    if (parts.length != 6)
                        return;

                    mIsPasting = true;
                    for (int i = 0; i < 6; i++) {
                        mEditMac.get(i).setText(parts[i]);
                    }
                    mIsPasting = false;
                    mEditBroadcastAddress.requestFocus();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (mIsPasting)
                    return;

                int len = s.toString().length();
                if (len == 2) {
                    int target = -1;
                    for (int i = 0; i < mEditMac.size(); i++) {
                        EditText candidate = mEditMac.get(i);
                        if (candidate.getText().hashCode() == s.hashCode()) {
                            target = i;
                            break;
                        }
                    }
                    if (target == -1)
                        return;

                    if (target == 5) {
                        mEditBroadcastAddress.requestFocus();
                    } else {
                        mEditMac.get(target + 1).requestFocus();
                    }
                }

                if (len > 2) {
                    s.delete(2, len);
                }
            }
        };

        for (EditText macEditor : mEditMac) {
            macEditor.addTextChangedListener(macTextWatcher);
        }

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
//                setAdvancedVisibility(isChecked);
            }
        });

        mSwitchPlatform.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                setPlatformInputsVisibility(!isChecked);
            }
        });

        mEditSshKey.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                try {
                    startActivityForResult(intent, SELECT_KEY_REQUEST);
                } catch (Exception e) {
                    Log.w(TAG, "No activity");
                    Toast.makeText(getActivity(), "Can't open file on this platform. Paste the key in the password field.", Toast.LENGTH_LONG).show();
                }
            }
        });
        mBtnLoadKey.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                try {
                    startActivityForResult(intent, SELECT_KEY_REQUEST);
                } catch (Exception e) {
                    Log.w(TAG, "No activity");
                    Toast.makeText(getActivity(), "Can't open file on this platform. Paste the key in the password field.", Toast.LENGTH_LONG).show();
                }
            }
        });

        // default visibility
        mLayoutSsh.setVisibility(View.VISIBLE);
        mLayoutWindows.setVisibility(View.GONE);

        return v;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        Log.i(TAG, "Received an \"Activity Result\"");
        boolean success = requestCode == SELECT_KEY_REQUEST && resultCode == Activity.RESULT_OK;

        if (!success)
            return;

        // The document selected by the user won't be returned in the intent.
        // Instead, a URI to that document will be contained in the return intent
        // provided to this method as a parameter.
        Uri uri = null;
        if (resultData != null) {
            uri = resultData.getData();
            if (uri == null) {
                return;
            }
            String keyName = "";
//                final String[] split = uri.getPath().split(":");//split the path.
//                mEditSshPassword.setText(split[1]);
//                final String[] nameSplit = split[1].split("/");
//                mEditSshKey.setText(nameSplit[nameSplit.length - 1]);
            try {
                InputStream fileInputStream = Objects.requireNonNull(getActivity()).getContentResolver().openInputStream(uri);

                assert fileInputStream != null;
                byte[] bytes = IOUtils.toByteArray(fileInputStream);

                // One way to read content
//                FileOutputStream writer = new FileOutputStream(new File(internalKey));
//                fileInputStream.mark(0);
//                Reader reader = new InputStreamReader(fileInputStream);
//                int length;
//                byte[] bytes = new byte[1024];

//                while ((length = fileInputStream.read(bytes)) != -1) {
//                    writer.write(bytes, 0, length);
//                }

                // Get string representation of the key
                Charset charset = Charset.forName("US-ASCII");
                CharsetDecoder decoder = charset.newDecoder();
                ByteBuffer srcBuffer = ByteBuffer.wrap(bytes);
                CharBuffer cont = decoder.decode(srcBuffer);
                String keyValue = String.valueOf(cont);
                if (keyValue.contains("BEGIN OPENSSH PRIVATE KEY")
                        || keyValue.contains("BEGIN SSH2 ENCRYPTED PRIVATE KEY")) {
                    Toast.makeText((Context) getActivity(),
                            R.string.unsupported_key, Toast.LENGTH_LONG).show();
                    return;
                }

                // Write key inside the app
                String internalKey = mContext.getFilesDir() + File.separator + "file";
                FileOutputStream writer = new FileOutputStream(new File(internalKey));
                writer.write(bytes);
                writer.close();

                mEditSshKey.setText(uri.getPath());

//                Reader reader = new InputStreamReader(fileInputStream);
//                BufferedReader r = new BufferedReader(reader);
//                StringBuilder total = new StringBuilder();
//                for (String line; (line = r.readLine()) != null; ) {
//                    total.append(line);
//                }
//                String result = total.toString();
//                mEditSshPassword.setText(result);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            HostBean host = savedInstanceState.getParcelable(ARG_BEAN);
            updateTarget(host);
        }
        super.onViewStateRestored(savedInstanceState);
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

        mViewShutdownCmd.setVisibility(visibility);
    }

    private void setPlatformInputsVisibility(boolean isSsh) {
        int sshVisibility = isSsh ? View.VISIBLE : View.GONE;
        int winVisibility = isSsh ? View.GONE : View.VISIBLE;

        mLayoutSsh.setVisibility(sshVisibility);
        mLayoutWindows.setVisibility(winVisibility);
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

    // invoked when the activity may be temporarily destroyed, save the instance state here
    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (mHostBean == null) {
            mHostBean = new HostBean();
            mHostBean.resetForView();
        }
        outState.putParcelable(ARG_BEAN, mHostBean);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.save_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // Add Up Navigation, part 2 (final)
                NavUtils.navigateUpFromSameTask(getActivity());
                return true;

            case R.id.action_save:
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
                mHostBean.sshShutdownCmd = mEditShutdownCmd.getText().toString();
                mHostBean.isSsh = !mSwitchPlatform.isChecked();
                mHostBean.sshKey = mEditSshKey.getText().toString();

                saveTargetToSettings();

                Intent startMain = new Intent(mContext, MainActivity.class);
                startMain.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(startMain);
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
        mEditShutdownCmd.setText(mHostBean.sshShutdownCmd);
        mSwitchPlatform.setChecked(!mHostBean.isSsh);
        mEditSshKey.setText(mHostBean.sshKey);
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
