package com.example.norman.wearsensorsapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.example.norman.wearsensorsapp.sensing.exceptions.ROSEmptyURL;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;


public class MainActivity extends BaseActivity implements Observer {

    private static final String debugTag = "MobileMainActivity";
    private static final int PERMISSIONS_REQUEST_CODE = 2;


    private ToggleButton tb;
    private ToggleButton toggleROS;
    private TextView status;
    private TextView textViewROS;

    private TextView currentTag;
    private Button sendTag;
    private EditText editTag;

    private String currentTagText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        this.initUI();
    }

    @Override
    public void onResume(){
        super.onResume();
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            this.checkPermissions();
        this.deviceSensingManager.addObserver(this);
        this.deviceSensingManager.checkForNodes();
        this.deviceSensingManager.updateSensors();
    }

    @Override
    public void onPause(){
        super.onPause();
    }

    private void initUI(){
        this.tb = (ToggleButton) findViewById(R.id.toggleServiceButton);
        this.toggleROS = (ToggleButton) findViewById(R.id.ROSToggleButton);
        this.status = (TextView) findViewById(R.id.status_tv);
        this.currentTag = (TextView) findViewById(R.id.session_tag_current_text_view);
        this.sendTag = (Button) findViewById(R.id.session_tag_send_button);
        this.editTag = (EditText) findViewById(R.id.session_tag_edit_text);
        this.currentTagText = this.getString(R.string.session_tag_current_tag_label);

        if(this.deviceSensingManager.isWearableConnected()) {
            tb.setEnabled(true);
            if (isServiceRunning) {
                this.tb.toggle();
                this.status.setText(this.getApplicationContext().getString(R.string.wearable_sensing_active));
            }

        } else {
            tb.setEnabled(false);
        }

        String tag = this.currentTagText +this.deviceSensingManager.getCurrentSessionTag();
        this.currentTag.setText(tag);

        sendTag.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String newTag = editTag.getText().toString();
                if( newTag.isEmpty() ){
                    final Snackbar snack = Snackbar.make(findViewById(R.id.main_coordinator_layout),
                                                    getString(R.string.session_tag_empty_error),
                                                    Snackbar.LENGTH_INDEFINITE);
                    snack.setAction(getString(R.string.common_button_text_dismiss),
                            new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    snack.dismiss();
                                }
                            });
                    snack.show();

                } else {
                    deviceSensingManager.setCurrentSessionTag(newTag);
                    String tag = currentTagText +
                            deviceSensingManager.getCurrentSessionTag();
                    currentTag.setText(tag);
                    editTag.setText("");
                }
            }
        });

        tb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(b){
                    Log.d("[ToggleButton]", "toggle true");
                    isServiceRunning = true;
                    startDataService();
                    sendTag.setEnabled(false);
                    editTag.setEnabled(false);
                    toggleROS.setEnabled(false);
                }else {
                    Log.d("[ToggleButton]", "toggle false");
                    isServiceRunning = false;
                    stopDataService();
                    sendTag.setEnabled(true);
                    editTag.setEnabled(true);
                    toggleROS.setEnabled(true);
                }
            }
        });

        toggleROS.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(b){
                    Log.d("[ToggleButtonROS]", "toggle true");
                    try {
                        rosDataSender.startSending();
                    } catch(IOException e){
                        Snackbar errorSnack;
                        errorSnack = Snackbar.make(findViewById(R.id.main_coordinator_layout),R.string.ros_invalid_url_error_message,Snackbar.LENGTH_LONG);
                        errorSnack.show();
                        toggleROS.toggle();
                    } catch (ROSEmptyURL ee){
                        Snackbar errorSnack;
                        errorSnack = Snackbar.make(findViewById(R.id.main_coordinator_layout),R.string.ros_empty_host_error_message,Snackbar.LENGTH_LONG);
                        errorSnack.show();
                        toggleROS.toggle();
                    }
                }else {
                    Log.d("[ToggleButtonROS]", "toggle false");
                    rosDataSender.stopSending();
                }
            }
        });
    }

    private void startDataService(){
        Intent intent = new Intent(this, CollectDataService.class);
        startService(intent);
        this.status.setText(this.getApplicationContext().getString(R.string.wearable_sensing_active));
    }

    private void stopDataService(){
        Intent intent = new Intent(this, CollectDataService.class);
        stopService(intent);
        this.status.setText(this.getApplicationContext().getString(R.string.wearable_sensing_not_active));
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }


    public void checkPermissions(){
        Boolean write_permission = ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        Boolean read_permission = ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        Boolean internet_permission = ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) == PackageManager.PERMISSION_GRANTED;
        String[] permissions;
        List<String> permissions_list = new LinkedList<String>();
        if( write_permission && read_permission && internet_permission) {
            this.deviceSensingManager.enablePublicSave();
            return;
        }

        if(!read_permission)
            permissions_list.add(android.Manifest.permission.READ_EXTERNAL_STORAGE);

        if (!write_permission)
            permissions_list.add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (!internet_permission)
            permissions_list.add(android.Manifest.permission.INTERNET);

        permissions = new String[permissions_list.size()];
        for(int i = 0; i< permissions_list.size(); i++)
            permissions[i] = permissions_list.get(i);

        ActivityCompat.requestPermissions(this, permissions, PERMISSIONS_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults){
        Log.d("MainActivityMobile", "onPermissionResult");
        switch (requestCode) {
            case PERMISSIONS_REQUEST_CODE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    this.deviceSensingManager.enablePublicSave();

                } else {

                    this.deviceSensingManager.disablePublicSave();
                }
                return;
            }

        }

    }

    @Override
    public void update(Observable observable, Object o) {
        Log.d("DeviceSensingManager", "MainActivityUpdate");
        if( this.deviceSensingManager.isWearableConnected() ){
            this.tb.setEnabled(true);
            Log.d(debugTag," wearable is connected");
            Log.d("DeviceSensingManager", "MainActivityUpdate enables");
        } else {
            this.tb.setEnabled(false);
            this.stopDataService();
            Log.d("DeviceSensingManager", "MainActivityUpdate disables");
            Log.d(debugTag," wearable is not connected");
        }

    }
}
