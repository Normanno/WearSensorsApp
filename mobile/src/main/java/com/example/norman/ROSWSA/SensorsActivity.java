package com.example.norman.ROSWSA;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;

import com.example.norman.shared.SensorGender;
import com.example.norman.ROSWSA.adapters.SensorCustomAdapter;
import com.example.norman.ROSWSA.sensing.DeviceSensingManager;
import com.example.norman.ROSWSA.sensing.MobileDataManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

public class SensorsActivity extends BaseActivity implements Observer{

    private static String debugTag = "SensorsActivity";

    private Map<String, String> device;

    private Button saveButton;
    private ListView sensorsListView;
    private SensorCustomAdapter sensorsAdapter;
    private ArrayList<DeviceSensingManager.SensorItem> sensorItems;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensors);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        this.saveButton = (Button) findViewById(R.id.save_sensors_spes);
        this.saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveSensorsSpecs();
            }
        });

        this.device = new HashMap<>();
        this.sensorItems = new ArrayList<DeviceSensingManager.SensorItem>();

        this.sensorsListView = (ListView) findViewById(R.id.sensors_list_view);
        this.sensorsAdapter = new SensorCustomAdapter(this.getApplicationContext(), this.sensorItems);
        this.sensorsListView.setAdapter(sensorsAdapter);
    }

    public void saveSensorsSpecs(){
        Snackbar sensorSnack;
        MobileDataManager mdm = MobileDataManager.getInstance(this.getApplicationContext());
        if( !mdm.saveSensorsSpecs()){
            sensorSnack = Snackbar.make(findViewById(R.id.sensors_coordinator_layout),R.string.sensor_file_already_exist,Snackbar.LENGTH_LONG);

        } else {
            sensorSnack = Snackbar.make(findViewById(R.id.sensors_coordinator_layout),R.string.sensor_file_saved,Snackbar.LENGTH_LONG);
        }
        sensorSnack.show();
    }


    @Override
    public void onResume(){
        super.onResume();
        this.deviceSensingManager.addObserver(this);
        this.deviceSensingManager.checkForNodes();
        this.updateItemsList();
    }

    private void updateItemsList(){
        sensorsAdapter.clear();
        this.updateSensorsList();
        sensorsAdapter.notifyDataSetChanged();
    }

    @Override
    public void onPause(){
        super.onPause();
        this.deviceSensingManager.deleteObserver(this);
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void updateSensorsList(){
        final Activity act = this;
        AsyncTask updater = new AsyncTask() {
            @Override
            protected Object doInBackground(Object[] objects) {
                SensorGender sensorGender = deviceSensingManager.getListingSensorGender();
                Map<Integer, DeviceSensingManager.SensorItem> sensors = deviceSensingManager.getListingSensors(sensorGender);

                if(sensorItems == null)
                    sensorItems = new ArrayList<DeviceSensingManager.SensorItem>();
                else
                    sensorItems.clear();
                sensorItems.addAll(sensors.values());
                act.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        sensorsAdapter.notifyDataSetChanged();
                    }
                });
                return null;
            }
        };
        //The update operations are performd in a async-task to avoid application freeze
        updater.execute();
    }

    @Override
    public void update(Observable observable, Object o) {
        if(this.deviceSensingManager.isWearableConnected()) {
            this.saveButton.setEnabled(true);
        } else {
            this.saveButton.setEnabled(false);
        }
        this.updateItemsList();
    }
}
