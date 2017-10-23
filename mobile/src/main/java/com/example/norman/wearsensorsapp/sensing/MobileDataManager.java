package com.example.norman.wearsensorsapp.sensing;

import android.content.Context;
import android.os.Environment;
import android.os.HandlerThread;
import android.os.Message;
import android.os.Handler;
import android.util.Log;
import android.util.Xml;

import com.example.norman.shared.SensorGender;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Wearable;

import org.xmlpull.v1.XmlSerializer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * This class takes care of saving sensors data and specs.
 * Created by norman on 22/09/17.
 */

public class MobileDataManager {

    private static final String debugTag = "MobileDataManager";

    private static MobileDataManager instance;
    private File filesLocation;
    private File dataFilesLocation;
    private File dataOutFile;
    private FileOutputStream dataOutFileStream;
    private OutputStreamWriter dataOutStreamWriter;

    private String fileName;
    private String filesPath;
    private String dataFilesPath;

    private DataWriteHandler dataWriteHandlerThread;
    private BlockingQueue<String> dataQueue;
    private boolean[] running = new boolean[1];

    private Context appContext;
    private GoogleApiClient mGoogleApiClient;
    private DeviceSensingManager deviceSensingManager;

    private int readCounter;
    private int writeCounter;

    private class DataWriteHandler extends HandlerThread {

        Handler handler;

        public DataWriteHandler(String name) {
            super(name);
        }

        @Override
        protected void onLooperPrepared(){
            handler = new Handler(getLooper()){
                @Override
                public void handleMessage(Message msg){
                    String data = null;
                    try {
                        data = dataQueue.take();
                        dataOutStreamWriter.write(data+"\n");
                        writeCounter++;
                    }
                    catch (IOException e) {
                        Log.e("Exception", "File write failed: " );
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        Log.d(debugTag, "Error taking from queue");
                        e.printStackTrace();
                    }
                }
            };

        }

        public void sendMessage(Message msg){
            handler.sendMessage(msg);
        }

        public void sendEmptyMessage(){
            handler.sendEmptyMessage(1);
        }
    }

    private MobileDataManager(Context context){
        appContext = context.getApplicationContext();
        this.mGoogleApiClient = new GoogleApiClient
                .Builder(appContext)
                .addApi(Wearable.API)
                .build();

        this.mGoogleApiClient.connect();

        this.deviceSensingManager = DeviceSensingManager.getInstance(context);
        this.updateFileLocations();

        this.startWriteHandler();
    }

    public static MobileDataManager getInstance(Context context){
        if( instance == null && context == null)
            return null; //TODO throw exception

        if( instance == null )
            instance = new MobileDataManager(context);

        return instance;
    }

    public void startWriteHandler(){
        synchronized (running) {
            Log.d(debugTag, "startWriterHandler");
            if (dataWriteHandlerThread == null || !dataWriteHandlerThread.isAlive()) {
                dataWriteHandlerThread = new DataWriteHandler("DataWriteHandlerThread");
                dataWriteHandlerThread.start();
            }
            running[0] = true;
        }
    }

    public void startWriter(){
        Log.d(debugTag, "startWriter");
        this.startWriteHandler();
    }

    public synchronized void stopWriter(){
        synchronized (running) {
            if (running[0]) {
                dataWriteHandlerThread.quit();
                dataWriteHandlerThread = null;
                dataQueue = null;
                Log.d(debugTag, " Read " + readCounter + " - Write " + writeCounter);
                try {
                    if (dataOutStreamWriter != null)
                        dataOutStreamWriter.close();
                } catch (IOException e) {
                    Log.d(debugTag, "error in closing out stream writer");
                    e.printStackTrace();
                }
                this.dataOutFile = null;
                this.dataOutFileStream = null;
                this.dataOutStreamWriter = null;
            }
            running[0] = false;
        }
    }

    private void updateFileLocations(){
        File filesDir = this.appContext.getFilesDir();

        if( this.deviceSensingManager.getPublicSavePossibility() )
            filesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);

        this.filesPath = this.deviceSensingManager.getFilesPath();
        this.dataFilesPath = this.deviceSensingManager.getDataFilesPath();

        filesLocation = new File(filesDir+"/../", this.filesPath);
        dataFilesLocation = new File(filesDir+"/../", this.dataFilesPath);

        if(!filesLocation.exists() || !filesLocation.isDirectory() )
            Log.d(debugTag,"mkdirs "+filesLocation.mkdirs());

        if( !dataFilesLocation.exists() || !dataFilesLocation.isDirectory())
            Log.d(debugTag,"mkdirs external "+dataFilesLocation.mkdirs());

    }

    private void initDataFile(){
        this.fileName = this.deviceSensingManager.getSessionDataFileName();
        dataOutFile = new File(dataFilesLocation, this.fileName);
        if(! dataOutFile.exists() )
            try {
                dataOutFile.createNewFile();
            } catch (IOException e) {
                Log.d(debugTag, "error during new file creation");
                e.printStackTrace();
            }

        //TODO handle !dataOutFile exist
        try {
            dataOutFileStream = new FileOutputStream(dataOutFile, true);// append mode
        } catch (FileNotFoundException e) {
            Log.d(debugTag, "DATA OUT FILE STREAM error during new file creation");
            e.printStackTrace();
        }

        dataOutStreamWriter = new OutputStreamWriter(dataOutFileStream);
    }

    //TODO SYNCHRONIZED???
    public void saveSensorData(final long timestamp, final int sensorId, final float[] values, SensorGender gender, Integer type){
        Log.d(debugTag, "savesensordata "+gender);
        if( dataOutFile == null || dataOutFileStream == null || dataOutStreamWriter == null)
            this.initDataFile();

        if( dataQueue == null)
            dataQueue = new LinkedBlockingQueue<String>();

        String value = "";
        for( Float f : values)
            value += f+"|";
        value = "[" + value.substring(0, value.length() - 1) + "]";

        String data = timestamp+","+sensorId+","+type+","+gender.getId()+","+value;

        readCounter++;
        dataQueue.add(data);
        dataWriteHandlerThread.sendEmptyMessage();
    }

    public Boolean saveRegisteringSensorsSpecs(){
        Map<String, String> device = this.deviceSensingManager.getDeviceCharacteristics();
        Map<Integer, DeviceSensingManager.SensorItem> sensorsMap = this.deviceSensingManager.getRegisteringSensors();
        SensorGender gender = this.deviceSensingManager.getListeningSensorsGender();
        if (device == null || sensorsMap == null || gender == null) {
            Log.d(debugTag, "saveRegisteringSensorsSpecs null values");
            return false;
        }

        XmlSerializer serializer = Xml.newSerializer();
        StringWriter writer = new StringWriter();
        File sensors = new File(this.dataFilesLocation, this.deviceSensingManager.getSessionSensorFileName());
        FileOutputStream sensorsOS;
        if ( !sensors.exists())
            try {
                sensors.createNewFile();
            } catch (IOException e) {
                Log.d(debugTag, "error during new file creation "+sensors.getPath());
                e.printStackTrace();
            }
        else
            return false;

        try {
            sensorsOS = new FileOutputStream(sensors);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        }

        try{
            serializer.setOutput(writer);
            serializer.startDocument("UTF-8", true);
            serializer.startTag("","specs");
            serializer.attribute("","date",this.deviceSensingManager.getFormattedDate());
            serializer.attribute("","session",this.deviceSensingManager.getCurrentSessionTag());
            serializer.startTag("","device");
            for(String key : device.keySet()) {
                serializer.startTag("", key);
                serializer.text(device.get(key));
                serializer.endTag("", key);
            }
            serializer.endTag("","device");
            serializer.startTag("","sensors_list");
            Log.d(debugTag, "sensors "+sensorsMap.size());
            for(Integer key: sensorsMap.keySet()){
                serializer.startTag("","sensor");
                serializer.startTag("","name");
                serializer.text(sensorsMap.get(key).name);
                serializer.endTag("","name");
                serializer.startTag("","id");
                serializer.text(key.toString());
                serializer.endTag("","id");
                serializer.startTag("","type");
                serializer.text(sensorsMap.get(key).type);
                serializer.endTag("","type");
                serializer.startTag("","gender");
                serializer.text(sensorsMap.get(key).gender.toString());
                serializer.endTag("","gender");
                serializer.endTag("","sensor");
            }
            serializer.endTag("","sensors_list");
            serializer.endTag("","specs");
            serializer.endDocument();
            sensorsOS.write(writer.toString().getBytes());
        }catch (Exception e){
            Log.d(debugTag, "exception : "+e.toString());
        }


        //TODO WRITE SENSORS SPECS TO FILE
        return true;
    }

    public Boolean saveSensorsSpecs(){
        Map<String, String> device = this.deviceSensingManager.getDeviceCharacteristics();
        SensorGender gender = this.deviceSensingManager.getListingSensorGender();
        Map<Integer, DeviceSensingManager.SensorItem> sensorsMap = this.deviceSensingManager.getListingSensors(gender);

        if (device == null || sensorsMap == null || gender == null) {
            Log.d(debugTag, "saveSensorsSpecs null values");
            return false;
        }
        XmlSerializer serializer = Xml.newSerializer();
        StringWriter writer = new StringWriter();
        File sensors = new File(this.dataFilesLocation, this.deviceSensingManager.getDeviceSensorFileName(gender));
        FileOutputStream sensorsOS;
        if ( !sensors.exists())
            try {
                sensors.createNewFile();
            } catch (IOException e) {
                Log.d(debugTag, "error during new file creation "+sensors.getPath());
                e.printStackTrace();
            }
        else
            return false;

        try {
            sensorsOS = new FileOutputStream(sensors);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        }

        try{
            serializer.setOutput(writer);
            serializer.startDocument("UTF-8", true);
            serializer.startTag("","specs");
            serializer.attribute("","date",this.deviceSensingManager.getFormattedDate());
            serializer.startTag("","device");
            for(String key : device.keySet()) {
                serializer.startTag("", key);
                serializer.text(device.get(key));
                serializer.endTag("", key);
            }
            serializer.endTag("","device");
            serializer.startTag("","sensors_list");
            Log.d(debugTag, "sensors "+sensorsMap.size());
            for(Integer key: sensorsMap.keySet()){
                serializer.startTag("","sensor");
                serializer.startTag("","name");
                serializer.text(sensorsMap.get(key).name);
                serializer.endTag("","name");
                serializer.startTag("","id");
                serializer.text(key.toString());
                serializer.endTag("","id");
                serializer.startTag("","type");
                serializer.text(sensorsMap.get(key).type);
                serializer.endTag("","type");
                serializer.startTag("","gender");
                serializer.text(sensorsMap.get(key).gender.toString());
                serializer.endTag("","gender");
                serializer.endTag("","sensor");
            }
            serializer.endTag("","sensors_list");
            serializer.endTag("","specs");
            serializer.endDocument();
            sensorsOS.write(writer.toString().getBytes());
        }catch (Exception e){
            Log.d(debugTag, "exception : "+e.toString());
        }


        //TODO WRITE SENSORS SPECS TO FILE
        return true;
    }

    @Override
    public void finalize() throws Throwable {
        super.finalize();
        Log.d(debugTag, "finalizing");
        this.stopWriter();
    }
}
