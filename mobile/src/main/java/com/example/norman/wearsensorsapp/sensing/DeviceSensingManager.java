package com.example.norman.wearsensorsapp.sensing;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.example.norman.shared.SensorGender;
import com.example.norman.wearsensorsapp.R;
import com.example.norman.wearsensorsapp.WearableSettingService;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.CapabilityApi;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;

/**
 * Created by norman on 08/09/17.
 * Holds the information about the Wearable device and his sensorsMap.
 * Takes care of sending start/stop messages to the wearable, sync the info about wearable's sensorsMap.
 */

public class DeviceSensingManager extends Observable {

    private final boolean[] updating = new boolean[1];
    private final boolean[] updatingNodes = new boolean[1];
    //private final boolean[] updatingListingSensors = new boolean[1];
    //private final boolean[] updatingRegisteringSensors = new boolean[1];

    private static final String debugTag = "DeviceSensingManager";
    private GoogleApiClient mGoogleApiClient;
    private List<Node> capableNodes;

    private Boolean publicSave = false;

    private Node bestNode;
    private Context appContext;

    private boolean registering;
    private SensorGender listeningSensorGender;
    private SensorGender listedSensorGender = SensorGender.STANDARD;
    private boolean rawSensors;

    private static DeviceSensingManager instance;

    private  Map<String, String> device;  //key=device_spec, value=device_spec_value

    private  Map<Integer, SensorItem> sensors;
    private  Map<Integer, SensorItem> registeringSensors;

   // private static File filesLocation;
    private String fileName = "WearSensorsApp";
    private String filesPath = "WearSensorsApp";
    private String dataFilesDir = "data";
    private String sessionTag = "WSA";
    private final SimpleDateFormat todayFormatter = new SimpleDateFormat("yyyy_MM_dd");

    private final SharedPreferences.OnSharedPreferenceChangeListener preferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
                rawSensors = sharedPreferences.getBoolean(appContext.getString(R.string.settings_require_raw_sensors_key), false);
                if( rawSensors )
                    listedSensorGender = SensorGender.RAW;
                else
                    listedSensorGender = SensorGender.STANDARD;
                updateSensors();
                updateObservers();
        }
    };

    private final GoogleApiClient.OnConnectionFailedListener connectionFailedListener = new GoogleApiClient.OnConnectionFailedListener() {
        @Override
        public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
            Log.d(debugTag, "onConnectionFailed "+connectionResult);

        }
    };

    private final CapabilityApi.CapabilityListener capabilityListener = new CapabilityApi.CapabilityListener() {
        @Override
        public void onCapabilityChanged(CapabilityInfo capabilityInfo) {
            Log.d(debugTag, "onCapabilityChanged");
        }
    };

    private final GoogleApiClient.ConnectionCallbacks connectedListener = new GoogleApiClient.ConnectionCallbacks() {
        @Override
        public void onConnected(@Nullable Bundle bundle) {
            Log.d(debugTag, "onConnected");
        }

        @Override
        public void onConnectionSuspended(int i) {
            Log.d(debugTag, "onConnectionSuspended");
        }
    };

    private DeviceSensingManager(Context context){
        this.appContext = context.getApplicationContext();
        this.mGoogleApiClient = new GoogleApiClient.Builder(this.appContext)
                    .addApi(Wearable.API)
                    .addConnectionCallbacks(this.connectedListener)
                    .addOnConnectionFailedListener(this.connectionFailedListener)
                    .build();

        mGoogleApiClient.connect();

        this.capableNodes = new ArrayList<Node>();

        if(device == null)
            device = new HashMap<>();

        if(sensors == null)
            sensors = new HashMap<Integer, SensorItem>();

        if(registeringSensors == null)
            registeringSensors = new HashMap<Integer, SensorItem>();

        //TODO SET FILE AND DIRECTORY PREFERENCES
        this.folderAndFileSettings();
        this.readDevicePrefs();
        this.readSensorsSetting();
        this.updateSensors();
    }

    public SharedPreferences.OnSharedPreferenceChangeListener getPreferenceChangeListener(){
        return preferenceChangeListener;
    }

    public static synchronized DeviceSensingManager getInstance(Context context){
        if(instance != null)
            return instance;
        instance = new DeviceSensingManager(context);
        return instance;
    }

    private void updateConnectedNodes(List<Node> nodes){
        synchronized (updatingNodes){
            capableNodes = nodes;
            bestNode = pickBestNode(nodes);
        }

    }

    public void checkForNodes(){
        Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).setResultCallback(new ResultCallback<NodeApi.GetConnectedNodesResult>() {
            @Override
            public void onResult(NodeApi.GetConnectedNodesResult getConnectedNodesResult) {
                if(getConnectedNodesResult.getNodes() != null)
                    updateConnectedNodes(getConnectedNodesResult.getNodes());
                updateObservers();
            }
        });
    }

    private void findAndSend(ResultCallback res_c){
        final ResultCallback rc = res_c;
        Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).setResultCallback(new ResultCallback<NodeApi.GetConnectedNodesResult>() {
            @Override
            public void onResult(NodeApi.GetConnectedNodesResult getConnectedNodesResult) {
                if(getConnectedNodesResult.getNodes() != null)
                    updateConnectedNodes(getConnectedNodesResult.getNodes());
                updateObservers();

                if( rc != null && bestNode != null)
                    rc.onResult(null);

            }
        });
    }

    /***
     * Return the nearby node or null (if there isn't any node)
     * @param nodes
     * @return
     */
    private Node pickBestNode(Collection<Node> nodes) {
        for (Node node : nodes) {
            if (node.isNearby()) {
                Log.d(debugTag," node found "+node.getDisplayName());
                return node;
            }
        }
        Log.d(debugTag, "NODE NOT FOUND");
        return null;
    }

    /*Setter*/
    public void setCurrentSessionTag(String tag){
        sessionTag = tag;
    }

    public void enablePublicSave(){
        publicSave = true;
    }

    public void disablePublicSave(){
        publicSave = false;
    }

    /*END Setter*/
    /*Get basic info*/
    public Boolean isWearableConnected(){
        return bestNode != null && bestNode.isNearby();
    }

    public SensorGender getListingSensorGender(){
        SensorGender s;
        synchronized (updating){
            if(this.listedSensorGender.equals(SensorGender.RAW))
                s=SensorGender.RAW;
            else
                s = SensorGender.STANDARD;
        }

        return s;
    }

    public String getCurrentSessionTag(){
        return this.sessionTag;
    }

    public String getFormattedDate(){
        return todayFormatter.format(Calendar.getInstance().getTime());
    }

    public Boolean getPublicSavePossibility(){
        return publicSave;
    }

    public String getSessionDataFileName(){
        String date = todayFormatter.format(Calendar.getInstance().getTime());
        String device_name = ( this.bestNode != null ? this.bestNode.getDisplayName(): "");
        return fileName+"_"+sessionTag+"_"+device_name+"_"+this.getListeningSensorsGender().toString()+"_Registered_Data_"+date+".csv";
    }

    public String getSessionSensorFileName(){
        String date = todayFormatter.format(Calendar.getInstance().getTime());
        String device_name = ( this.bestNode != null ? this.bestNode.getDisplayName(): "");
        return fileName+"_"+sessionTag+"_"+device_name+"_"+this.getListeningSensorsGender().toString()+"_Sensors_Specs_"+date+".xml";
    }

    public String getDeviceSensorFileName(SensorGender gender){
        String device_name = ( this.bestNode != null ? this.bestNode.getDisplayName(): "");
        return device_name+"_"+gender.toString()+"_Sensors_Specs.xml";
    }

    public String getFilesPath(){
        return filesPath;
    }

    public String getDataFilesPath(){
        return filesPath + "/" +dataFilesDir;
    }

    public SensorGender getListeningSensorsGender(){
        SensorGender s;
        synchronized (updating){
            if(this.listedSensorGender.equals(SensorGender.RAW))
                s = SensorGender.RAW;
            else
                s = SensorGender.STANDARD;
        }
        return s;
    }
    /*END Get basic info*/
    /*Messaging*/

    private void sendStartWearSensingServiceMessage(){
        final SensorGender gender = this.getListingSensorGender();
        final Map<Integer, SensorItem> lSensors = new HashMap<Integer, SensorItem>(this.getListingSensors(gender));
        String msg =  this.appContext.getString(R.string.START_SENSING_SERVICE_STANDARD_MESSAGE);
        if( gender.equals(SensorGender.RAW))
            msg =  this.appContext.getString(R.string.START_SENSING_SERVICE_RAW_MESSAGE);
        if(bestNode != null && bestNode.isNearby())
            Wearable.MessageApi.sendMessage(this.mGoogleApiClient,
                    this.bestNode.getId(),
                    msg,
                    null)
                    .setResultCallback(
                            new ResultCallback<MessageApi.SendMessageResult>() {
                                @Override
                                public void onResult(@NonNull MessageApi.SendMessageResult sendMessageResult) {
                                    if(sendMessageResult.getStatus().isSuccess()) { //if the service starts
                                        synchronized (updating) {
                                            registering = true;                       // save the data related to the sensors
                                            listeningSensorGender = gender;           // that we're going to listen
                                            registeringSensors = lSensors;
                                        }
                                        Log.d(debugTag, "message sent " + sendMessageResult.getStatus().getStatusMessage() +" - "+ sendMessageResult.getStatus());
                                    } else if(sendMessageResult.getStatus().isCanceled())
                                        Log.d(debugTag, "message canceled "+sendMessageResult.getStatus().getStatusMessage());
                                    else if(sendMessageResult.getStatus().isInterrupted())
                                        Log.d(debugTag, "message interrupted "+sendMessageResult.getStatus().getStatusMessage());
                                    else if(!sendMessageResult.getStatus().isSuccess())
                                        Log.d(debugTag, "message not sent " +sendMessageResult.getStatus().getStatusMessage());
                                    else
                                        Log.d(debugTag, "No message");
                                }
                            }
                    );
        else
            Log.d(debugTag, "NULL BEST NODE");
    }

    public void startWearSensingService(final SensorGender gender){
        synchronized (updating) {
            this.listedSensorGender = gender;
        }
        if( bestNode == null || !bestNode.isNearby() )
            this.findAndSend(new ResultCallback() {
                @Override
                public void onResult(@NonNull Result result) {
                    sendStartWearSensingServiceMessage();
                }
            });
        else
            this.sendStartWearSensingServiceMessage();

        //TODO ADD TOGGLE BUTTO TO SELECT THE GENDER
        //TODO ADD SEPARATE LISTS FOR REQUIRED SENSORS AND LISTED SENSORS
    }

    private void sendStopWearSensingServiceMessage(){
        if(bestNode != null && bestNode.isNearby())
            Wearable.MessageApi.sendMessage(this.mGoogleApiClient,
                    this.bestNode.getId(),
                    this.appContext.getString(R.string.STOP_SENSING_SERVICE_MESSAGE),
                    null)
                    .setResultCallback(
                            new ResultCallback<MessageApi.SendMessageResult>() {
                                @Override
                                public void onResult(@NonNull MessageApi.SendMessageResult sendMessageResult) {
                                    if(sendMessageResult.getStatus().isSuccess()) {
                                        synchronized (updating) {
                                            registering = false;
                                            listeningSensorGender = null;
                                            registeringSensors = null;
                                        }
                                        Log.d(debugTag, "stop message sent");
                                    }else if(sendMessageResult.getStatus().isCanceled())
                                        Log.d(debugTag, "stop message canceled");
                                    else if(sendMessageResult.getStatus().isInterrupted())
                                        Log.d(debugTag, "stop message interrupted");
                                    else if(!sendMessageResult.getStatus().isSuccess())
                                        Log.d(debugTag, "stop message not sent");
                                    else
                                        Log.d(debugTag, "No message");
                                }
                            }
                    );
        else
            Log.d(debugTag, "NULL BEST NODE");
    }

    public void stopWearSensingService(){
        if( bestNode == null || !bestNode.isNearby() )
            this.findAndSend(new ResultCallback() {
                @Override
                public void onResult(@NonNull Result result) {
                    sendStopWearSensingServiceMessage();
                }
            });
        else
            this.sendStopWearSensingServiceMessage();
    }

    private void sendSensorRequestMessage(){
        String msg = this.appContext.getString(R.string.LIST_AVAILABLE_STANDARD_SENSORS);
        if( this.getListingSensorGender().equals(SensorGender.RAW) )
            msg = this.appContext.getString(R.string.LIST_AVAILABLE_RAW_SENSORS);

        if(bestNode != null && bestNode.isNearby()) {
            final String finalMsg = msg;
            Wearable.MessageApi.sendMessage(this.mGoogleApiClient,
                    this.bestNode.getId(),
                    msg,
                    null)
                    .setResultCallback(
                            new ResultCallback<MessageApi.SendMessageResult>() {
                                @Override
                                public void onResult(@NonNull MessageApi.SendMessageResult sendMessageResult) {
                                    Log.d(debugTag, "Message "+finalMsg+" sent " +sendMessageResult.getStatus().getStatusMessage());
                                    if( sendMessageResult.getStatus().isSuccess() )
                                        Log.d(debugTag, "Message sent");
                                    else
                                        Log.d(debugTag, "MESSAGE NOT SENT");
                                }
                            }
                    );
        }
        else
            Log.d(debugTag, "NULL BEST NODE");
    }

    /*Syncronize sensorsMap data with the smartwatch*/
    private void requestWearableSensors(){

        if( bestNode == null || !bestNode.isNearby() )
            this.findAndSend(new ResultCallback() {
                @Override
                public void onResult(@NonNull Result result) {
                    sendSensorRequestMessage();
                }
            });
        else
            this.sendSensorRequestMessage();
    }

    /*End Messaging*/

    /*DataSync*/

    private void startWearSettingsService(){
        Intent intent = new Intent(this.appContext, WearableSettingService.class);
        appContext.startService(intent);
    }

    private void stopWearSettingsService(){
        Intent intent = new Intent(this.appContext, WearableSettingService.class);
        appContext.stopService(intent);
    }

    /***
     * Update the sensosrs list
     * @param map
     * @param gender
     */
    public void updateSensorsList(Map<Integer, SensorItem> map, SensorGender gender){
        synchronized (updating) {
            listedSensorGender = gender;
            sensors.clear();
            for(Integer key: map.keySet())
                sensors.put(key, map.get(key));
            updating.notifyAll();
        }
        this.saveWearableSettings();
    }
    /*End DataSync*/

    /*Sensors methods*/

    /***
     *
     * @return map of the sensors that are currently observed
     */
    public Map<Integer, SensorItem> getRegisteringSensors(){
        Map<Integer, SensorItem> map ;
        synchronized (updating){
            map = new HashMap<>(registeringSensors);
        }

        return map;
    }

    /***
     * Returns the sensors map of the required gender
     * @param gender
     * @return
     */
    public Map<Integer, SensorItem> getListingSensors(SensorGender gender){
        if( !gender.equals(getListeningSensorsGender()) || this.sensors.isEmpty() ){
            synchronized (updating) {
                this.updateSensors();

                while (!gender.equals(getListeningSensorsGender()) || this.sensors.isEmpty())
                    try {
                        updating.wait();
                    } catch (InterruptedException e) {
                        return new HashMap<Integer, SensorItem>();
                    }
            }

        }
        Map<Integer, SensorItem> map;
        synchronized (updating){
            map = new HashMap<>(sensors);
        }

        return map;
    }

    public Map<String, String> getDeviceCharacteristics(){
        return new HashMap<String, String>(device);
    }
    /*End Sensors methods*/

    /**
     * This method force the DeviceSensingManager to update the sensors list
     * */
    public void updateSensors(){
        synchronized (updating) {
            if(this.isWearableConnected()) {
                this.startWearSettingsService();
                this.requestWearableSensors();
            }
        }

    }

    private void updateObservers(){
        this.setChanged();
        this.notifyObservers();
    }

    private void readDevicePrefs(){
        Context c1 = this.appContext;
        SharedPreferences wearablePref = c1.getSharedPreferences(c1.getString(R.string.MOBILE_PREFERENCES_ACTUAL_WEARABLE), c1.MODE_PRIVATE);
        for( String key : wearablePref.getAll().keySet())
            device.put(key, wearablePref.getString(key, null));

    }

    private void readSensorsSetting(){
        Context c1 = this.appContext;
        SharedPreferences sensorsSettings = PreferenceManager.getDefaultSharedPreferences(c1);
        String key = c1.getString(R.string.settings_require_raw_sensors_key);
        this.rawSensors = sensorsSettings.getBoolean(key, false);
        this.listedSensorGender = (rawSensors ? SensorGender.RAW:SensorGender.STANDARD);
    }

    private void saveDevicePrefs(){
        Context c1 = this.appContext;
        SharedPreferences wearablePref = c1.getSharedPreferences(c1.getString(R.string.MOBILE_PREFERENCES_ACTUAL_WEARABLE), c1.MODE_PRIVATE);
        SharedPreferences.Editor editor = wearablePref.edit();
        editor.putString("device_name", bestNode.getDisplayName());
        device.put("device_name", bestNode.getDisplayName());
        editor.putString("device_id", bestNode.getId());
        device.put("device_id", bestNode.getId());

        editor.commit();
    }
    //TODO SAVE SENSOR PREFERENCES

    private void saveWearableSettings(){
        Log.d(debugTag,"Settings receive the data");
        this.saveDevicePrefs();
     //   this.saveSensorsPrefs();
     //   this.saveActiveSensorsPref();
    }

    private void folderAndFileSettings(){
        //TODO READ FILE LOCATION PREFERENCES
        /*
        filesLocation = appContext.getDir("WearSensorsApp", Context.MODE_APPEND);
        Boolean made = false;
        if (!filesLocation.exists())
            made = filesLocation.mkdirs();

        if( made )
            Log.d(debugTag, "DIRECTORY CREATED");
        else
            Log.d(debugTag, "DIRECTORY NOT CREATED");
            */
    }

    public static class SensorItem{

        public String name;
        public String type;
        public int id;
        public boolean listening;
        public SensorGender gender;

        public SensorItem(String name, String type, Integer id, Boolean listening, SensorGender gender){
            this.name = name;
            this.type = type;
            this.id = id;
            this.listening = listening;
            this.gender = gender;
        }

        public SensorItem(String name, Integer type, Integer id, Boolean listening, SensorGender gender){
            this(name, type.toString(), id, listening, gender);
        }

    }

    @Override
    public void finalize(){
        if(this.mGoogleApiClient.isConnected() || mGoogleApiClient.isConnecting())
            mGoogleApiClient.disconnect();
        synchronized (updating){
            this.stopWearSettingsService();
        }
    }
}
