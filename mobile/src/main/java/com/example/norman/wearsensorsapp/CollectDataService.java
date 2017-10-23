package com.example.norman.wearsensorsapp;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.IBinder;
import android.util.Log;

import com.example.norman.shared.SensorGender;
import com.example.norman.wearsensorsapp.sensing.MobileDataManager;
import com.example.norman.wearsensorsapp.sensing.DeviceSensingManager;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;

import java.util.HashMap;
import java.util.Map;


public class CollectDataService extends Service implements DataApi.DataListener{

    public static final String TAG = "com.example.norman.wearsensorapp.CollectDataService";
    private static final String debugTag = "CollectDataService";
    private static Integer period;

    private DeviceSensingManager sm;
    private GoogleApiClient mGoogleApiClient;
    private MobileDataManager mdm;

    private Map<Integer, Boolean> activeSensors;

    @Override
    public void onDataChanged(DataEventBuffer dataEventBuffer) {
        Log.d(debugTag, "DataEvent");
        Context c =this.getApplicationContext();
        for(DataEvent de : dataEventBuffer){
            if(de.getType() == DataEvent.TYPE_CHANGED){
                DataItem di = de.getDataItem();
                Uri URI = di.getUri();
                String path = URI.getPath();
                if(path.equals(c.getString(R.string.DATA_SYNC_SENSOR_DATA)))
                    this.handleSensorData(di);
            }
        }
    }

    private void handleSensorData(DataItem dataItem){
        //TODO HANDLE SENSOR DATA
        Context context = this.getApplicationContext();
        Log.d(debugTag, "HandlingSensorData");
        DataMap dm = DataMapItem.fromDataItem(dataItem).getDataMap();
        int sensorId = dm.getInt(context.getString(R.string.DATA_SYNC_KEY_SENSOR_ID));
        float[] values = dm.getFloatArray(context.getString(R.string.DATA_SYNC_KEY_SENSOR_DATA_VALUES));
        long timestamp = dm.getLong(context.getString(R.string.DATA_SYNC_KEY_TIMESTAMP));
        Integer type = dm.getInt(context.getString(R.string.DATA_SYNC_KEY_SENSOR_DATA_TYPE));
        SensorGender sg = SensorGender.getFromId(dm.getInt(context.getString(R.string.DATA_SYNC_KEY_SENSOR_DATA_SENSOR_GENDER)));

        mdm.saveSensorData(timestamp, sensorId, values, sg, type);
        Log.d(debugTag, "Timestamp "+timestamp);
    }

    @Override
    public void onCreate(){
        this.mGoogleApiClient = new GoogleApiClient.Builder(this.getApplicationContext())
                .addApi(Wearable.API)
                .build();

        this.activeSensors = new HashMap<Integer, Boolean>();

        this.sm = DeviceSensingManager.getInstance(this);
        this.mdm = MobileDataManager.getInstance(this.getApplicationContext());
        this.mdm.startWriter();

        Log.d(debugTag, "service created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
   period = 1000;     Boolean b = false;
        //Needed for wearable node connection
        mGoogleApiClient.connect();
        if( mGoogleApiClient != null && mGoogleApiClient.isConnected() )
            Log.d(debugTag, "Google api connected");
        //Add this service as a listener on Wearable data api
        Wearable.DataApi.addListener(mGoogleApiClient,this);

        (new AsyncTask (){
            @Override
            protected Object doInBackground(Object[] objects) {
                Log.d("Async", "doing");

                sm.startWearSensingService(sm.getListingSensorGender());
                return null;
            }

        }).execute(b);

        Log.d(debugTag, "service started");
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        period = 1000;

        // TODO: Return the communication channel to the service.

        Log.d(debugTag, "service binded");
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private void exitOps(){
        Log.d(debugTag,"EXIT OPS START");
        mdm.saveRegisteringSensorsSpecs();
        this.sm.stopWearSensingService();
        this.mdm.stopWriter();
        //REMOVE this service as a listener on Wearable data api
        Wearable.DataApi.removeListener(mGoogleApiClient,this);
        if (this.mGoogleApiClient.isConnected() || this.mGoogleApiClient.isConnecting())
            this.mGoogleApiClient.disconnect();

        Log.d(debugTag,"EXIT OPS END");
    }

    @Override
    public void onDestroy(){
        this.exitOps();
        Log.d(debugTag, "service destroied");
        super.onDestroy();
    }

    @Override
    public boolean stopService(Intent name){
        this.exitOps();
        Log.d(debugTag,"service stopped!!!!");
        return super.stopService(name);
    }
}
