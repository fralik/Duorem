package com.vadimfrolov.twobuttonremote;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.vadimfrolov.twobuttonremote.Network.HostBean;

public class TargetConfigurationActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_target_configuration);

        TargetConfigurationFragment configurationFragment = (TargetConfigurationFragment)
                getSupportFragmentManager().findFragmentById(R.id.host_details_fragement);
        Intent intent = getIntent();
        Bundle args = intent.getExtras();
        if (args != null && configurationFragment != null) {
            if (intent.hasExtra(HostBean.EXTRA)) {
                HostBean host = intent.getParcelableExtra(HostBean.EXTRA);
                configurationFragment.updateTarget(host);
            }
        }
    }
}
