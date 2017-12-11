package com.example.norman.ROSWSA;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.widget.TextView;

public class MainActivity extends Activity {

    private TextView mTextView;
    private static final String debugTag = "WearableMainActivity";
    private static final int BODY_SENSORS_PERMISSION_REQUEST_CODE = 1;
    private static String intentStatusName ;

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Extract data included in the Intent
            String msg = intent.getStringExtra("message");
            mTextView.setText(msg);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                mTextView = (TextView) stub.findViewById(R.id.text);
            }
        });
        Log.d(debugTag, "Activity Created");
        intentStatusName = this.getApplicationContext().getString(R.string.WEARABLE_LOCAL_STATUS_INTENT_NAME);
        Log.d(debugTag, "onCreate");

        boolean bodySensorsPermission = ContextCompat.checkSelfPermission(this.getApplicationContext(), Manifest.permission.BODY_SENSORS) == PackageManager.PERMISSION_GRANTED;

        if( !bodySensorsPermission ){
            Log.d(debugTag, "Requesting permission");
            ActivityCompat.requestPermissions(
                    MainActivity.this,
                    new String[]{Manifest.permission.BODY_SENSORS},
                    BODY_SENSORS_PERMISSION_REQUEST_CODE
            );

        }
    }

    @Override
    public void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this)
                .registerReceiver(mMessageReceiver,
                        new IntentFilter(intentStatusName));
        Log.d(debugTag, "onResume");
    }

    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this)
                .unregisterReceiver(mMessageReceiver);
        super.onPause();
        Log.d(debugTag, "onPause");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults){
        if( requestCode == BODY_SENSORS_PERMISSION_REQUEST_CODE && permissions[0].equals(Manifest.permission.BODY_SENSORS))
            if( grantResults[0] != PackageManager.PERMISSION_GRANTED)
                mTextView.setText(this.getApplicationContext().getString(R.string.REQUEST_PERMISSION_BODY_DENIED_MSG));
            else
                mTextView.setText(this.getApplicationContext().getString(R.string.REQUEST_PERMISSION_BODY_GRANTED_MSG));
    }

}
