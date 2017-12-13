package com.example.norman.wearsensorsapp;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.example.norman.shared.SensorType;
import com.example.norman.shared.SensorGender;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.CapabilityApi;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class WearableSensingManager extends WearableListenerService
        implements SensorEventListener , GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, CapabilityApi.CapabilityListener{

    private WearableDataSender ds;
    private GoogleApiClient mGoogleApiClient;
    private SensorManager sm;

    private Map<Integer, Sensor> sensorId_sensor;
    private Map<Integer, Boolean> selectedSensors;
    //In case the Sensor.getId() returns 0 (not supported) or 
    // -1 (identification with Sensor.Type + Sensor.Name)
    private Map<Sensor, Integer> sensor_sensorId;
    private ArrayList<Integer> sensorsIds;
    private ArrayList<Integer> sensorsTypes;
    private ArrayList<String> sensorsNames;

    private SensorGender listedSensorGender; //Holds the listed sensors gender

    private SensorGender requiredSensorGender; //Holds the listening sensors type
    private Map<Sensor, Integer> required_senorId; //holds the listening sensors and ids

    private Integer hrMeasurementDuration = 10;
    private Integer hrMeasurementDelay = 5;
    private TimeUnit hrTimeUnit = TimeUnit.SECONDS;
    private ScheduledExecutorService hrScheduler;


    private Boolean rawValues;
    private Boolean listenForSensors;

    private String mobileNodeId;

    private static String debugTag = "WearableListener";

    @Override
    public void onCreate(){
        super.onCreate();

        if( !this.checkGrantedPermissions() )
            this.requirePermissions();

        this.mGoogleApiClient = new GoogleApiClient
                .Builder(this.getApplicationContext())
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        this.mGoogleApiClient.connect();

        this.sm = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        this.ds = WearableDataSender.getInstance(this.getApplicationContext());

        this.rawValues = false;

        this.mobileNodeId = "";

        if(sensorId_sensor == null)
            sensorId_sensor = new HashMap<Integer, Sensor>();
        if(selectedSensors == null)
            selectedSensors = new HashMap<Integer, Boolean>();
        if(sensorsIds == null)
            sensorsIds = new ArrayList<Integer>();
        if(sensorsNames == null)
            sensorsNames = new ArrayList<String>();
        if(sensorsTypes == null)
            sensorsTypes = new ArrayList<Integer>();
        if( sensor_sensorId == null)
            sensor_sensorId = new HashMap<Sensor, Integer>();
        if( required_senorId == null)
            required_senorId = new HashMap<>();
        this.readPreferences();
    }

    /***
     * Check if all required dangerous permissions are granted
     *
     * @return - true if granted
     *         - false otherwise
     */
    private boolean checkGrantedPermissions(){
        return this.checkSelfPermission(Manifest.permission.BODY_SENSORS) == PackageManager.PERMISSION_GRANTED;
    }

    private void requirePermissions(){
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    private void readPreferences(){
        SharedPreferences pref = getSharedPreferences(this.getString(R.string.PREFERENCES_FILE_NAME), MODE_PRIVATE);
        Integer id = pref.getInt(this.getString(R.string.PREFERENCES_KEY_REQUIRED_SENSOR_TYPE), 1);
        requiredSensorGender = SensorGender.getFromId(id);
        rawValues = requiredSensorGender.equals(SensorGender.RAW);
    }

    private void savePreferences(){
        SharedPreferences pref = getSharedPreferences(this.getString(R.string.PREFERENCES_FILE_NAME), MODE_PRIVATE);
        SharedPreferences.Editor prefEditor = pref.edit();
        prefEditor.putInt(this.getString(R.string.PREFERENCES_KEY_REQUIRED_SENSOR_TYPE), this.requiredSensorGender.getId());
        prefEditor.commit();
    }

    /* To be called after savePreferences*/
    private void saveSensorPreferences(){
        String filename = this.getString(R.string.PREFERENCES_STANDARD_SENSORS_PREF_FILE_NAME);
        if(requiredSensorGender.equals(SensorGender.RAW))
            filename = this.getString(R.string.PREFERENCES_RAW_SENSORS_PREF_FILE_NAME);
        SharedPreferences sensorPref = getSharedPreferences(filename, MODE_PRIVATE);
        SharedPreferences.Editor sensorPrefEditor = sensorPref.edit();
        for( Sensor s : sensor_sensorId.keySet() ) {
            sensorPrefEditor.putInt(s.getName(), sensor_sensorId.get(s));
        }
        sensorPrefEditor.commit();
    }

    /* To be called after readPreferences*/
    private void readSensorPreferences(){
        String filename = this.getString(R.string.PREFERENCES_STANDARD_SENSORS_PREF_FILE_NAME);
        if(requiredSensorGender.equals(SensorGender.RAW))
            filename = this.getString(R.string.PREFERENCES_RAW_SENSORS_PREF_FILE_NAME);

        sensor_sensorId.clear();
        sensorId_sensor.clear();
        selectedSensors.clear();
        sensorsIds.clear();
        sensorsNames.clear();
        sensorsTypes.clear();

        SharedPreferences sensorPref = getSharedPreferences(filename, MODE_PRIVATE);
        Integer id;
        if(rawValues)
            for(Sensor s : sm.getSensorList(Sensor.TYPE_ALL)) {
                id = sensorPref.getInt(s.getName(), -1);
                sensor_sensorId.put(s, id);
                sensorId_sensor.put(id, s);
                sensorsIds.add(id);
                sensorsTypes.add(s.getType());
                sensorsNames.add(s.getName());
            }
        else
            for(SensorType cs : SensorType.values()){
                id = cs.getId();
                Sensor s = sm.getDefaultSensor(id);
                sensor_sensorId.put(s, id);
                sensorId_sensor.put(id, s);
                sensorsIds.add(id);
                sensorsTypes.add(s.getType());
                sensorsNames.add(cs.getName(this.getApplicationContext()));
            }

    }


    @Override
    public void onMessageReceived(MessageEvent messageEvent){
        if( messageEvent.getPath().equals(
                this.getString(R.string.START_SENSING_SERVICE_STANDARD_MESSAGE)
            ) ){
            this.listDefaultSensors();
            this.requiredSensorGender = SensorGender.STANDARD;
            this.required_senorId = new HashMap<Sensor, Integer>(this.sensor_sensorId);
            this.startSensorsListening();
            this.mobileNodeId = messageEvent.getSourceNodeId();
        } else if( messageEvent.getPath().equals(
                this.getString(R.string.START_SENSING_SERVICE_RAW_MESSAGE)
        ) ){
            this.listRawSensors();
            this.requiredSensorGender = SensorGender.RAW;
            this.required_senorId = new HashMap<Sensor, Integer>(this.sensor_sensorId);
            this.startSensorsListening();
            this.mobileNodeId = messageEvent.getSourceNodeId();
        } else if( messageEvent.getPath().equals(
                this.getString(R.string.STOP_SENSING_SERVICE_MESSAGE)
            ) ){
            this.stopSensorsListening();
        } else if( messageEvent.getPath().equals(
                this.getString(R.string.LIST_AVAILABLE_STANDARD_SENSORS)
            ) ){
            if( rawValues || sensor_sensorId.isEmpty() || sensorId_sensor.isEmpty()){
                rawValues = false;
                this.listDefaultSensors();
            }
            this.sendSensorsSpecs(SensorGender.STANDARD);
            if( requiredSensorGender != SensorGender.STANDARD) {
                requiredSensorGender = SensorGender.STANDARD;
                this.savePreferences();
            }
        } else if(messageEvent.getPath().equals(this.getString(R.string.LIST_AVAILABLE_RAW_SENSORS))) {
            if( !rawValues || sensor_sensorId.isEmpty() || sensorId_sensor.isEmpty()){
                rawValues = true;
                this.listRawSensors();
            }
            this.sendSensorsSpecs(SensorGender.RAW);
            if( requiredSensorGender != SensorGender.RAW) {
                requiredSensorGender = SensorGender.RAW;
                this.savePreferences();
            }
        }
    }

    @Override
    public void onCapabilityChanged(CapabilityInfo capabilityInfo) {
        updateSensingCapability(capabilityInfo);
    }


    private void updateSensingCapability(CapabilityInfo capabilityInfo) {
        Set<Node> connectedNodes = capabilityInfo.getNodes();
        if (connectedNodes.isEmpty()) {
            Log.d("Wearable", "NODES DISCONNECTED");
        } else {
            List<Node> availableNodes = new ArrayList<Node>(connectedNodes);
            for (Node node : connectedNodes) {
                if (node.isNearby()) {
                    mobileNodeId = node.getId();
                    Log.d("Wearable", "NODE NEARBY "+node.getDisplayName());
                    break;
                }
            }
        }
    }

/*
    private void updateStatus(){
                Wearable.CapabilityApi.getCapability(
                        mGoogleApiClient,
                        this.getApplicationContext().getString(R.string.MOBILE_COLLECTING_CAPABILITY),
                        CapabilityApi.FILTER_REACHABLE)
                        .setResultCallback(
                            new ResultCallback<CapabilityApi.GetCapabilityResult>() {
                                @Override
                                public void onResult(CapabilityApi.GetCapabilityResult result) {
                                    if (result.getStatus().isSuccess()) {
                                        Log.d(debugTag, "Success to get capabilities, " + "status: " + result.getStatus().getStatusMessage());
                                        updateSensingCapability(result.getCapability());
                                    } else {
                                        Log.d(debugTag, "Failed to get capabilities, " + "status: " + result.getStatus().getStatusMessage());
                                    }
                                }
                            });

    }
*/
    private void listRawSensors(){
        /* device sensors does not change in time */
        /* counter used to create internal ids, because sensor.getId() is not supported by all devices*/

        int counter = 0;
        listedSensorGender = SensorGender.RAW;
        sensor_sensorId.clear();
        sensorId_sensor.clear();
        List<Sensor> sensors = this.sm.getSensorList(Sensor.TYPE_ALL);
        sensorsIds = new ArrayList<Integer>();
        sensorsTypes = new ArrayList<Integer>();
        sensorsNames = new ArrayList<String>();
        for (Sensor s : sensors) {
            if (this.sm.getDefaultSensor(s.getType()) != null) {
                sensorId_sensor.put(counter, s);
                sensor_sensorId.put(s, counter);
                selectedSensors.put(counter, false);
                sensorsNames.add(sensorId_sensor.get(counter).getName());
                sensorsIds.add(counter);
                sensorsTypes.add(s.getType());
                counter++;
            }

        }
    }

    private void listDefaultSensors(){
        /* device sensors does not change in time */
        /* counter used to create internal ids, because sensor.getId() is not supported by all devices*/
        listedSensorGender = SensorGender.STANDARD;
        sensor_sensorId.clear();
        sensorId_sensor.clear();
        sensorsIds = new ArrayList<Integer>();
        sensorsNames = new ArrayList<String>();
        sensorsTypes =  new ArrayList<Integer>();
        for(SensorType cs : SensorType.values() ){
            Sensor s = this.sm.getDefaultSensor(cs.getId());
            if( s != null) {
                sensorId_sensor.put(cs.getId(), s);
                sensor_sensorId.put(s, cs.getId());
                selectedSensors.put(cs.getId(), false);
                sensorsNames.add(s.getName());
                sensorsIds.add(cs.getId());
                sensorsTypes.add(s.getType());
            }
        }
    }

    private void sendSensorsSpecs(SensorGender type){
        Log.d(debugTag, "Sending sensors specs "+type.toString());
        final String msg = type.toString();
        String requestPath = this.getString(R.string.DATA_SYNC_DEFAULT_SENSOR_LIST);
        if( type.equals(SensorGender.RAW) )
            requestPath = this.getString(R.string.DATA_SYNC_RAW_SENSOR_LIST);
        PutDataMapRequest pdmr = PutDataMapRequest.create(requestPath);
        pdmr.getDataMap().putIntegerArrayList(this.getString(R.string.SENSOR_LIST_IDS_DATA_MAP), sensorsIds);
        pdmr.getDataMap().putStringArrayList(this.getString(R.string.SENSOR_LIST_NAMES_DATA_MAP), sensorsNames);
        pdmr.getDataMap().putIntegerArrayList(this.getString(R.string.SENSOR_LIST_TYPES_DATA_MAP), sensorsTypes);
        pdmr.getDataMap().putInt(this.getString(R.string.DATA_SYNC_KEY_SENSOR_DATA_SENSOR_GENDER), type.getId());
        pdmr.getDataMap().putLong(this.getString(R.string.DATA_SYNC_KEY_TIMESTAMP), System.currentTimeMillis());// used to force the onDataChanged event
        pdmr.setUrgent();
        PutDataRequest pdr = pdmr.asPutDataRequest();
        pdr.setUrgent();
        PendingResult<DataApi.DataItemResult> pendingResult = Wearable.DataApi.putDataItem(mGoogleApiClient, pdr);
        pendingResult.setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
            @Override
            public void onResult(final DataApi.DataItemResult result) {
                Log.d(debugTag, msg + " specs sent : " + result.getDataItem().getUri() + " - " + result.getStatus().getStatusMessage());
            }
        });
    }

    @Override
    public void onConnectedNodes(List<Node> connectedNodes){
        Log.d(debugTag, "onConnectedNodes");
        if (mGoogleApiClient.isConnected()) {
            Log.d(debugTag, "nodes "+connectedNodes);
            Node mobile = null;
            for(Node n :connectedNodes)
                if(mobileNodeId.isEmpty())
                    break;
                else if(n.getId().equals(mobileNodeId)) {
                    mobile = n;
                    break;
                }

            if (mobile == null && !mobileNodeId.isEmpty())
                this.stopSensorsListening();
        } else if (!mGoogleApiClient.isConnecting()) {
            mGoogleApiClient.connect();
        }
    }

    @Override
    public void onDestroy(){
        if (this.mGoogleApiClient.isConnected() || this.mGoogleApiClient.isConnecting())
            this.mGoogleApiClient.disconnect();
        this.savePreferences();
        super.onDestroy();
    }


    private void initHrListening(final Sensor hrSensor){
        final SensorEventListener listener = this;
        hrScheduler = Executors.newScheduledThreadPool(1);
        hrScheduler.scheduleAtFixedRate(
                new Runnable() {
                    @Override
                    public void run() {
                        Boolean registered = sm.registerListener(listener, hrSensor, SensorManager.SENSOR_DELAY_NORMAL);
                        try {
                            Thread.sleep(hrMeasurementDuration * 1000);
                        } catch (InterruptedException e) {
                            Log.e(debugTag, "Interrupted while waitting to unregister Heartrate Sensor");
                        }
                        sm.unregisterListener(listener, hrSensor);
                    }
                }, 3, hrMeasurementDuration + hrMeasurementDelay, hrTimeUnit);
    }

    private void startSensorsListening(){
        this.listenForSensors = true;
        for(SensorType key : SensorType.values()){
            Sensor s = this.sm.getDefaultSensor(key.getId());
            if( s != null && key.getId() != Sensor.TYPE_HEART_RATE)
                sm.registerListener(this, s, SensorManager.SENSOR_DELAY_NORMAL);
            else if( s != null && key.getId() == Sensor.TYPE_HEART_RATE)
                this.initHrListening(s);
        }
        this.updateActivity(this.getApplicationContext().getString(R.string.wearable_sensing_active));
    }

    private void stopSensorsListening(){
        Log.d(debugTag, "StopSensorsListening");
        this.sm.unregisterListener(this);
        this.listenForSensors = false;
        if( hrScheduler != null && !hrScheduler.isTerminated()) {
            hrScheduler.shutdown();
            hrScheduler = null;
        }

        this.updateActivity(this.getApplicationContext().getString(R.string.wearable_sensing_not_active));
    }

    private void updateActivity(String status){
        String intentName = this.getApplicationContext().getString(R.string.WEARABLE_LOCAL_STATUS_INTENT_NAME);
        Intent intent = new Intent(intentName);
        intent.putExtra("message", status);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (!listenForSensors) {
            sm.unregisterListener(this);
            return;
        }
        Sensor s = sensorEvent.sensor;
        int id;
        if(requiredSensorGender.equals(SensorGender.RAW))
            id = required_senorId.get(s);
        else
            id = s.getType();

        ds.sendSensorData(id, sensorEvent, requiredSensorGender);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    @Override
    public void onConnected(Bundle connectionHint)  {
        Log.d(debugTag, "onConnected");
        Log.d(debugTag, "Connection hint "+connectionHint);
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(debugTag, "onConnectionSUspended cause "+i);
        this.stopSensorsListening();
    }

    @Override
    public void onPeerConnected(Node peer){
        Log.d(debugTag, "onPeerConnected");
        Log.d(debugTag, "Connection hint "+peer);
    }

    @Override
    public void onPeerDisconnected(Node peer){
        Log.d(debugTag, "onPeerDIsconnected");
        Log.d(debugTag, "onPeerDisconnected peer "+peer);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(debugTag, "onConnectionFailed "+connectionResult);
    }
}
