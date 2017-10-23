package com.example.norman.wearsensorsapp;

import android.content.Context;
import android.hardware.SensorEvent;
import android.util.Log;

import com.example.norman.shared.SensorGender;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by norman on 22/09/17.
 */

public class WearableDataSender {

    private static final String debugTag = "WearableDataSender";

    private static WearableDataSender instance;
    private Context appContext;
    private ExecutorService executorService;
    private GoogleApiClient mGoogleApiClient;

    private WearableDataSender(Context context){
        appContext = context.getApplicationContext();
        this.mGoogleApiClient = new GoogleApiClient
                .Builder(appContext)
                .addApi(Wearable.API)
                .build();

        this.mGoogleApiClient.connect();

        this.executorService = Executors.newCachedThreadPool();
    }

    public static WearableDataSender getInstance(Context context){
        if( instance == null && context == null)
            return null; //TODO throw exception

        if( instance == null )
            instance = new WearableDataSender(context);

        return instance;
    }

    public void sendSensorData(final int sensorId, final SensorEvent se, final SensorGender st){
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                packAndSendSensorData(sensorId, se, st);
            }
        });

    }

    private void packAndSendSensorData(int sensorId, SensorEvent se, SensorGender st){
        final SensorGender sg = st;
        PutDataMapRequest pdmr = PutDataMapRequest.create(appContext.getString(R.string.DATA_SYNC_SENSOR_DATA));
        pdmr.getDataMap().putInt(appContext.getString(R.string.DATA_SYNC_KEY_SENSOR_ID), sensorId);
        pdmr.getDataMap().putInt(appContext.getString(R.string.DATA_SYNC_KEY_SENSOR_DATA_TYPE), se.sensor.getType());
        pdmr.getDataMap().putFloatArray(appContext.getString(R.string.DATA_SYNC_KEY_SENSOR_DATA_VALUES), se.values);
        pdmr.getDataMap().putLong(appContext.getString(R.string.DATA_SYNC_KEY_TIMESTAMP), se.timestamp);
        pdmr.getDataMap().putInt(appContext.getString(R.string.DATA_SYNC_KEY_SENSOR_DATA_SENSOR_GENDER),st.getId());
        pdmr.setUrgent();
        PutDataRequest pdr = pdmr.asPutDataRequest();
        pdr.setUrgent();
        PendingResult<DataApi.DataItemResult> pendingResult = Wearable.DataApi.putDataItem(mGoogleApiClient, pdr);
        pendingResult.setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
            @Override
            public void onResult(final DataApi.DataItemResult result) {
                Log.d(debugTag, sg.toString()+" data sent to: " + result.getDataItem().getUri() + " - result: " + result.getStatus().getStatusMessage());
            }
        });

    }
}
