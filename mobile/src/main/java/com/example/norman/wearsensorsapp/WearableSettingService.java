package com.example.norman.wearsensorsapp;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.example.norman.shared.SensorGender;
import com.example.norman.wearsensorsapp.sensing.DeviceSensingManager;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.CapabilityApi;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Observable;

public class WearableSettingService extends Service implements DataApi.DataListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        CapabilityApi.CapabilityListener{

    private static final String debugTag = "WearableSettingService";

    private GoogleApiClient mGoogleApiClient;
    private DeviceSensingManager deviceSensingManager;

    public WearableSettingService() {

    }

    @Override
    public void onCreate(){
        Log.d(debugTag,"SERVICE CREATED");
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mGoogleApiClient.connect();
        deviceSensingManager = DeviceSensingManager.getInstance(this.getApplicationContext());
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        Log.d(debugTag, "Intent "+intent.getType());
        throw new UnsupportedOperationException("Not yet implemented");
    }


    @Override
    public void onDataChanged(DataEventBuffer dataEventBuffer) {
        for(DataEvent de : dataEventBuffer){
            if(de.getType() == DataEvent.TYPE_CHANGED){
                DataItem di = de.getDataItem();
                Uri URI = di.getUri();
                String path = URI.getPath();
                if( path.equals(this.getApplicationContext().getString(R.string.DATA_SYNC_DEFAULT_SENSOR_LIST)) ||
                    path.equals(this.getApplicationContext().getString(R.string.DATA_SYNC_RAW_SENSOR_LIST)))
                    this.updateSensorsList(di);
            }
        }
    }

    private void updateSensorsList(DataItem di){
        Log.d(debugTag,"Updating available sensorsMap");
        Context c = this.getApplicationContext();
        DataMap dm = DataMapItem.fromDataItem(di).getDataMap();
        ArrayList<String> names = dm.getStringArrayList(c.getString(R.string.SENSOR_LIST_NAMES_DATA_MAP));
        ArrayList<Integer> ids = dm.getIntegerArrayList(c.getString(R.string.SENSOR_LIST_IDS_DATA_MAP));
        ArrayList<Integer> types = dm.getIntegerArrayList(c.getString(R.string.SENSOR_LIST_TYPES_DATA_MAP));
        SensorGender gender = SensorGender.getFromId(dm.getInt(c.getString(R.string.DATA_SYNC_KEY_SENSOR_DATA_SENSOR_GENDER)));
        Map<Integer, DeviceSensingManager.SensorItem> sensors = new HashMap<Integer, DeviceSensingManager.SensorItem>();
            for (int i = 0; i < ids.size(); i++)
                sensors.put(ids.get(i), new DeviceSensingManager.SensorItem(names.get(i), types.get(i), ids.get(i), true, gender));

        this.deviceSensingManager.updateSensorsList(sensors, gender);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d(debugTag, "Settings service added as listener");
        Wearable.DataApi.addListener(mGoogleApiClient, this);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onDestroy(){
        if(mGoogleApiClient != null && mGoogleApiClient.isConnected())
            mGoogleApiClient.disconnect();
        Log.d(debugTag,"SERVICE DESTROYED");
    }

    @Override
    public void onCapabilityChanged(CapabilityInfo capabilityInfo) {
        Log.d(debugTag, "onCapabilityChanged");
        if(capabilityInfo.getNodes().size() > 0){
            Log.d(debugTag, "onCapabilityChanged OK!");
        }else{
            Log.d(debugTag, "onCapabilityChanged NONE!");
        }
    }

}
