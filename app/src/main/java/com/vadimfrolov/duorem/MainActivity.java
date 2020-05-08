/*
 * Copyright (C) 2017 Vadim Frolov
 * Licensed under GNU's GPL 3 or any later version, see README
 */

package com.vadimfrolov.duorem;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.Toolbar;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.gson.Gson;
import com.vadimfrolov.duorem.Network.HostBean;
import com.vadimfrolov.duorem.Network.NetInfo;
import com.vadimfrolov.duorem.Network.RemoteAsyncTask;
import com.vadimfrolov.duorem.Network.RemoteCommand;
import com.vadimfrolov.duorem.Network.RemoteCommandResult;

import java.io.File;
import java.net.Socket;
import java.util.Stack;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class MainActivity extends ActivityNet
        implements SharedPreferences.OnSharedPreferenceChangeListener,
        RemoteCommandResult {

    public static final String KEY_PREF_TARGET = "target";
    private final String TAG = "MainActivity";
    // static final Integer READ_STORAGE_PERMISSION_REQUEST_CODE = 0x32;

    private HostBean mTarget;
    private TextView mViewName;
    SharedPreferences mPrefs;
    ImageView mIconAlive;
    ImageView mHostType;

    Button mBtnTogglePower;
    Button mBtnRestart;
    TextView mViewStatus;
    Stack<RemoteAsyncTask> mSshTasks;
    private RemoteCommandResult mDelegate; // reference to this, for code that can not use this directly
    private ScheduledThreadPoolExecutor mSch = null;
    private ScheduledFuture<?> mPollFuture = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Create action bar as a toolbar
        Toolbar toolbar = findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);

        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        mPrefs.registerOnSharedPreferenceChangeListener(this);

        mViewName = findViewById(R.id.id);
        mIconAlive = findViewById(R.id.alive);
        mBtnTogglePower = findViewById(R.id.btn_toggle_power);
        mBtnRestart = findViewById(R.id.btn_restart);
        mViewStatus = findViewById(R.id.text_status);
        mHostType = findViewById(R.id.main_host_type);

        mViewStatus.setMovementMethod(new ScrollingMovementMethod());

        Gson gson = new Gson();
        String targetJson =  mPrefs.getString(KEY_PREF_TARGET, "");
        if (targetJson.length() > 0) {
            mTarget = gson.fromJson(targetJson, HostBean.class);
        }

        mSshTasks = new Stack<>();
        mDelegate = this;
        mIsConnected = false;

        mBtnRestart.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                stopTargetPolling();
                RemoteCommand cmd;
                if (mTarget.isSsh) {
                    cmd = new RemoteCommand(mTarget, RemoteCommand.SSH);
                    cmd.command = "sudo shutdown -r now";

                    cmd.sshKeyFile = mContext.getFilesDir() + File.separator + "file";
                } else {
                    cmd = new RemoteCommand(mTarget, RemoteCommand.RPC);
                    cmd.command = "reboot";
                }
                mSshTasks.push(new RemoteAsyncTask(mDelegate));
                mSshTasks.peek().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, cmd);

                logForUser(getResources().getString(R.string.reboot_sent));

            }
        });
        mBtnTogglePower.setOnClickListener(mPowerActor);

        // create a pool with only 3 threads
        mSch = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(3);
        mSch.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
    }

//    public boolean checkPermissionForReadExtertalStorage() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            int result = this.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE);
//            return result == PackageManager.PERMISSION_GRANTED;
//        }
//        return false;
//    }
//
//    public void requestPermissionForReadExtertalStorage() throws Exception {
//        try {
//            ActivityCompat.requestPermissions((Activity) this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
//                    READ_STORAGE_PERMISSION_REQUEST_CODE);
//        } catch (Exception e) {
//            e.printStackTrace();
//            throw e;
//        }
//    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        try {
            Gson gson = new Gson();
            String targetJson = gson.toJson(mTarget);

            SharedPreferences.Editor editor = mPrefs.edit();
            editor.putString(MainActivity.KEY_PREF_TARGET, targetJson);
            editor.apply();
        } catch (Exception e) {
            Log.d(TAG, "Failed to save target as JSON: " + e.getMessage());
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mIsConnected && mTarget != null && mTarget.ipAddress != null && !mTarget.ipAddress.equals(NetInfo.NOIP)) {
            startTargetPolling();
        }
        updateView();
    }

    @Override
    public void onPause() {
        super.onPause();

        stopTargetPolling();
        while (!mSshTasks.isEmpty()) {
            mSshTasks.pop().cancel(true);
        }
        // unregisterReceiver(mNetworkReceiver);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        switch (item.getItemId()) {
            case R.id.action_add_host:
                intent = new Intent(this, SearchConfigureActivity.class);
//                PendingIntent pendingIntent = TaskStackBuilder.create(this)
//                        .addNextIntentWithParentStack(intent)
//                        .getPendingIntent(0, PendingIntent.FLAG_CANCEL_CURRENT);
//                NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
//                builder.setContentIntent(pendingIntent);
                this.startActivity(intent);
                break;

            case R.id.action_edit_host:
                intent = new Intent(this, TargetConfigurationActivity.class);
                intent.putExtra(HostBean.EXTRA, mTarget);
                this.startActivity(intent);
                break;

            case R.id.action_delete_host:
                mPrefs.edit().remove(KEY_PREF_TARGET).apply();
                mTarget = null;
                updateView();
                break;

            case R.id.action_about:
                String version = ActivityNet.getVersionName(mContext);
                FragmentManager fm = getSupportFragmentManager();
                AboutDialog dlg = AboutDialog.newInstance(getResources().getString(R.string.about_title), version);
                dlg.show(fm, "about_dialog");
                break;

            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem miAdd = menu.findItem(R.id.action_add_host);
        MenuItem miEdit = menu.findItem(R.id.action_edit_host);
        MenuItem miDelete = menu.findItem(R.id.action_delete_host);
        boolean targetIsValid = mTarget != null &&
                ((mTarget.ipAddress != null && !mTarget.ipAddress.equals(NetInfo.NOIP))
                || (mTarget.hardwareAddress != null && !mTarget.hardwareAddress.equals(NetInfo.NOMAC)));

        if (targetIsValid) {
            miAdd.setTitle(getResources().getString(R.string.replace_host));
        } else
        {
            miAdd.setTitle(getResources().getString(R.string.add_host));
        }
        miEdit.setEnabled(targetIsValid);
        miDelete.setEnabled(targetIsValid);

        miEdit.setShowAsAction(targetIsValid ? MenuItem.SHOW_AS_ACTION_ALWAYS : MenuItem.SHOW_AS_ACTION_NEVER);
        miDelete.setShowAsAction(targetIsValid ? MenuItem.SHOW_AS_ACTION_ALWAYS : MenuItem.SHOW_AS_ACTION_NEVER);
        // hide replace menu item under the unfoldable menu
        miAdd.setShowAsAction(targetIsValid ? MenuItem.SHOW_AS_ACTION_NEVER : MenuItem.SHOW_AS_ACTION_ALWAYS);

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key != null && key.equals(KEY_PREF_TARGET)) {
            Gson gson = new Gson();
            String json = sharedPreferences.getString(KEY_PREF_TARGET, "");
            mTarget = gson.fromJson(json, HostBean.class);
            updateView();
        }
    }

    protected void updateView() {
        mBtnTogglePower.setEnabled(mIsConnected && mTarget != null);
        if (!mIsConnected) {
            mViewStatus.setText(getResources().getString(R.string.no_network));
        }

        boolean isTargetValid = true;
        boolean isTargetAlive = mTarget != null && mTarget.isAlive;
        if (mTarget == null || (mTarget.ipAddress.equals(NetInfo.NOIP) && mTarget.hardwareAddress.equals(NetInfo.NOMAC))) {
            isTargetValid = false;
            mViewName.setText(getResources().getString(R.string.no_device));
        }

        if (isTargetValid) {
            mViewName.setText(mTarget.name());
        }

        mBtnRestart.setEnabled(mIsConnected && isTargetAlive);
        int color = isTargetAlive ? Color.GREEN : Color.RED;
        mIconAlive.setColorFilter(color);
        mIconAlive.setVisibility(isTargetValid ? View.VISIBLE : View.GONE);
        int textId = isTargetAlive ? R.string.shutdown : R.string.turn_on;
        mBtnTogglePower.setText(getResources().getString(textId));
        if (mTarget != null) {
            if (mTarget.isSsh) {
                mHostType.setImageDrawable(getApplication().getDrawable(R.drawable.ic_ssh));
            } else {
                mHostType.setImageDrawable(getApplication().getDrawable(R.drawable.ic_win));
            }
        }

        invalidateOptionsMenu();
    }

    protected void updateNetworkStatus() {
        if (mIsConnected) {
            mViewStatus.setText("");
            if (mTarget != null) {
                if (mTarget.broadcastIp == null || mTarget.broadcastIp.equals(NetInfo.NOIP)) {
                    mTarget.broadcastIp = mNetInfo.broadcastIp;
                }
                startTargetPolling();
            }
        } else {
            if (mTarget != null) {
                mTarget.isAlive = false;
            }
            stopTargetPolling();
        }
        updateView();
    }

    private View.OnClickListener mPowerActor = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mTarget == null) {
                return;
            }

            RemoteCommand cmd = new RemoteCommand(mTarget);
            if (mTarget.isAlive) {
                cmd.commandType = RemoteCommand.SSH;
                cmd.command = mTarget.sshShutdownCmd;
                if (!mTarget.isSsh) {
                    cmd.commandType = RemoteCommand.RPC;
                    cmd.command = "shutdown";
                }

                logForUser(getResources().getString(R.string.shutdown_sent));
            } else {
                cmd.commandType = RemoteCommand.WOL;

                logForUser(getResources().getString(R.string.wol_sent));
            }
            mSshTasks.push(new RemoteAsyncTask(mDelegate));
            mSshTasks.peek().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, cmd);
        }
    };

    private void startTargetPolling() {
        Runnable pollTask = new Runnable() {
            @Override
            public void run() {
                if (mIsConnected && mTarget != null) {
                    RemoteCommand cmd = new RemoteCommand(mTarget, RemoteCommand.PING);

                    // Prevent memory leak by adding to the queue
                    RemoteAsyncTask task = new RemoteAsyncTask(mDelegate);
                    mSshTasks.push(task);
                    mSshTasks.peek().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, cmd);
                }
            }
        };
        if (mSch != null) {
            if (mPollFuture == null) {
                mPollFuture = mSch.scheduleWithFixedDelay(pollTask, 0, 5, TimeUnit.SECONDS);
            }

            // check if target is reachable right away. This is good for UI.
            boolean isAlive = true;
            try {
                Socket socket = new Socket(mTarget.ipAddress, Integer.parseInt(mTarget.sshPort));
                socket.close();
            } catch (Exception e) {
                isAlive = false;
            }
            mTarget.isAlive = isAlive;
        }
    }

    private void stopTargetPolling() {
        if (mSch != null && mPollFuture != null) {
            mPollFuture.cancel(true);
            mPollFuture = null;
        }
    }

    @Override
    public void onRemoteCommandFinished(RemoteCommand output) {
        if (mTarget == null || mViewStatus == null || output == null)
            return;
        final boolean success = output.result.equals("success");

        switch (output.commandType) {
            case RemoteCommand.PING:
                mTarget.isAlive = success;
                break;

            case RemoteCommand.WOL:
                if (success) {
                    logForUser(getResources().getString(R.string.wol_received));
                }
                if (output.result.equals("invalid gateway")) {
                    logForUser(getResources().getString(R.string.no_gateway));
                }
                break;

            case RemoteCommand.SSH:
                if (output.command.contains("-r now")) {
                    if (output.result.length() > 0) {
                        logForUser(getResources().getString(R.string.reboot_received));
                    } else {
                        logForUser(getResources().getString(R.string.reboot_failed));
                    }
                    startTargetPolling();
                } else if (output.command.contains(mTarget.sshShutdownCmd)) {
                    if (output.result.length() > 0) {
                        logForUser(getResources().getString(R.string.shutdown_received));
                    } else {
                        logForUser(getResources().getString(R.string.shutdown_failed));
                    }
                }
                break;

                case RemoteCommand.RPC:
                    if (output.command.equals("reboot")) {
                        if (success) {
                            logForUser(getResources().getString(R.string.reboot_received));
                        } else {
                            logForUser(getResources().getString(R.string.reboot_failed));
                        }
                        startTargetPolling();
                    } else {
                        if (success) {
                            logForUser(getResources().getString(R.string.shutdown_received));
                        } else {
                            logForUser(getResources().getString(R.string.shutdown_failed));
                        }
                    }
                    break;

            default:
                break;
        }

        updateView();
    }

    @Override
    public void getPassphrase(final RemoteCommand cmd, final String identityName) {
        LayoutInflater li = LayoutInflater.from(this);
        View propmtView = li.inflate(R.layout.passphrase_prompt, null);
        AlertDialog.Builder dlgBuilder = new AlertDialog.Builder(this);
        dlgBuilder.setTitle(getResources().getString(R.string.dlg_passphrase_title));
        dlgBuilder.setMessage(getResources().getString(R.string.dlg_passphrase_msg));
        dlgBuilder.setView(propmtView);

        TextView labelPassphrase = propmtView.findViewById(R.id.passphrase_key_name);
        final EditText editPassphrase = propmtView.findViewById(R.id.edit_passphrase);
        labelPassphrase.setText(identityName);

        dlgBuilder.setCancelable(true).setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
                cmd.passPhrase = editPassphrase.getText().toString();
            }
        });
        dlgBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                cmd.passPhrase = "";
                dialog.cancel();
            }
        });
        AlertDialog dlg = dlgBuilder.create();
        dlg.show();
    }

    public void logForUser(String msg) {
        if (mViewStatus == null)
            return;

        mViewStatus.setText(String.format("%s%s\n", mViewStatus.getText().toString(), msg));
        try {
            // find the amount we need to scroll.  This works by
            // asking the TextView's internal layout for the position
            // of the final line and then subtracting the TextView's height
            final int scrollAmount = mViewStatus.getLayout().getLineTop(mViewStatus.getLineCount()) - mViewStatus.getHeight();
            // if there is no need to scroll, scrollAmount will be <=0
            if (scrollAmount > 0) {
                mViewStatus.scrollTo(0, scrollAmount);
            } else {
                mViewStatus.scrollTo(0, 0);
            }
        } catch (Exception e) {
            // we do not bother if we can not scroll
        }
    }
}
