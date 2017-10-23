package com.example.norman.wearsensorsapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.example.norman.wearsensorsapp.sensing.DeviceSensingManager;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;

public class BaseActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    protected DeviceSensingManager deviceSensingManager;
    protected static boolean isServiceRunning;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        deviceSensingManager = DeviceSensingManager.getInstance(this.getApplicationContext());
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

        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_home) {

        } else if (id == R.id.nav_display_data) {

        } else if (id == R.id.nav_help) {

        } else if (id == R.id.nav_info) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    /* called when clicking on about button in the drawer or in the settings menu*/
    public void menuInfoAction(MenuItem item){
        Intent intent = new Intent(this, AboutActivity.class);
        startActivity(intent);
    }

    /* called when clicking on home button in the drawer or in the settings menu*/
    public void menuHomeAction(MenuItem item){
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    /* called when clicking on displayData button in the drawer or in the settings menu*/
    public void menuDisplayDataAction(MenuItem item){
        Intent intent = new Intent(this, DisplayDataActivity.class);
        startActivity(intent);
    }

    /* called when clicking on settings button in the drawer or in the settings menu*/
    public void menuSettingsAction(MenuItem item){
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    public void menuSensorsAction(MenuItem item){
        Intent intent = new Intent(this, SensorsActivity.class);
        startActivity(intent);
    }

    /* called when clicking on settings button in the drawer or in the settings menu*/
    public void menuHelpAction(MenuItem item){
        Intent intent = new Intent(this, HelpActivity.class);
        startActivity(intent);
    }

    public void checkWearable(MenuItem item){
        this.deviceSensingManager.checkForNodes();
    }

}
