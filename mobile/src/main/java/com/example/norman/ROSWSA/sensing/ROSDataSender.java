package com.example.norman.ROSWSA.sensing;

import android.content.Context;
import android.hardware.Sensor;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import com.example.norman.shared.SensorType;
import com.example.norman.ROSWSA.sensing.exceptions.ROSBadMessage;
import com.example.norman.ROSWSA.sensing.exceptions.ROSEmptyURL;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by norman on 23/10/17.
 */

public class ROSDataSender {
    private Integer port = 5092;
    private String ip_address = "";
    private URL url = null;
    private String uname = "ROS";
    private String pass = "SGRA_ROS";
    private final String debugTag = "ROSSendDataHandler";
    private boolean[] running = new boolean[1];

    private Context appContext;

    private DeviceSensingManager deviceSensingManager;

    private BlockingQueue<ROSMessage> message_queue;

    private ROSSendDataHandler rosSendDataHandler;

    private static com.example.norman.ROSWSA.sensing.ROSDataSender instance;

    private ROSMessage message;

    private class ROSMessage{
        private static final String debugTag = "ROSMessage";
        Long acc_timestamp = null;
        Long gyro_timestamp = null;
        Long hr_timestamp = null;
        Float acc_x = null;
        Float acc_y = null;
        Float acc_z = null;
        Float gyro_x = null;
        Float gyro_y = null;
        Float gyro_z = null;
        Float hr = null;

        private ROSMessage(){

        }

        private ROSMessage(ROSMessage msg){
            this.acc_timestamp = msg.acc_timestamp != null ? msg.acc_timestamp.longValue(): null;
            this.gyro_timestamp = msg.gyro_timestamp != null ? msg.gyro_timestamp.longValue(): null;
            this.hr_timestamp = msg.hr_timestamp != null ? msg.hr_timestamp.longValue(): null;
            this.acc_x = msg.acc_x != null ? msg.acc_x.floatValue(): null;
            this.acc_y = msg.acc_y != null ? msg.acc_y.floatValue(): null;
            this.acc_z = msg.acc_z != null ? msg.acc_z.floatValue(): null;
            this.gyro_x = msg.gyro_x != null ? msg.gyro_x.floatValue(): null;
            this.gyro_y = msg.gyro_y != null ? msg.gyro_y.floatValue(): null;
            this.gyro_z = msg.gyro_z != null ? msg.gyro_z.floatValue(): null;
            this.hr = msg.hr != null ? msg.hr.floatValue(): null;
        }

        private boolean wellFormed(){
            return acc_x != null && acc_y != null && acc_z != null &&
                    gyro_x != null && gyro_y != null && gyro_z != null &&
                    hr != null  && this.acc_timestamp != null &&
                    this.gyro_timestamp != null && this.hr_timestamp != null;
        }

        private JSONObject toJson() throws ROSBadMessage{
            if(! this.wellFormed())
                throw new ROSBadMessage();
            JSONObject json = new JSONObject();
            Long timestamp = Math.max(Math.max(acc_timestamp, gyro_timestamp), hr_timestamp);
            try {
                json.put("timestamp", timestamp);
                json.put("acc_x", this.acc_x);
                json.put("acc_y", this.acc_y);
                json.put("acc_z", this.acc_z);
                json.put("gyro_x", this.gyro_x);
                json.put("gyro_y", this.gyro_y);
                json.put("gyro_z", this.gyro_z);
                json.put("hr", this.hr);
            } catch (JSONException e){
                Log.d(debugTag, "jsonException "+e.toString());
            }

            return json;
        }

    }

    private class ROSSendDataHandler extends HandlerThread{

        Handler handler;
        private static final String debugTag = "ROSSendDataHandler";

        public ROSSendDataHandler(String name) {
            super(name);
        }

        @Override
        protected void onLooperPrepared(){
            handler = new Handler(getLooper()){
                @Override
                public void handleMessage(Message msg){
                    ROSMessage data = null;

                    try{
                        data = message_queue.take();
                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                        conn.setRequestMethod("POST");
                        conn.setRequestProperty("Content-Type", "application/json");
                        conn.setRequestProperty("Accept","application/json");
                        conn.setDoOutput(true);
                        conn.setDoInput(true);
                        conn.connect();
                        JSONObject jsonParam;

                        jsonParam = data.toJson();
                        jsonParam.put("uname", uname);
                        jsonParam.put("pass", pass);


                        DataOutputStream os = new DataOutputStream(conn.getOutputStream());
                        os.writeBytes(jsonParam.toString());

                        os.flush();
                        os.close();

                        Log.i("STATUS", String.valueOf(conn.getResponseCode()));
                        Log.i("MSG" , conn.getResponseMessage());

                        conn.disconnect();

                    } catch (InterruptedException e){
                        Log.d(debugTag, "InterruptedException: "+e.toString());
                    } catch(IOException ee){
                        Log.d(debugTag, "IOException: "+ee.toString());
                    } catch (JSONException eee){
                        Log.d(debugTag, "JSONException: "+eee.toString());
                    } catch (ROSBadMessage eeee){
                        Log.d(debugTag, "ROSBadMessage: "+eeee.toString());
                    }
                }
            };
        }

        public void sendEmptyMessage(){
            handler.sendEmptyMessage(1);
        }
    }

    private ROSDataSender(Context context) {
        this.appContext = context.getApplicationContext();
        this.deviceSensingManager = DeviceSensingManager.getInstance(context);
        this.message = new ROSMessage();
        this.message_queue = new LinkedBlockingQueue<ROSMessage>();
    }

    public static ROSDataSender getInstance(Context context) {
        if( instance == null)
            instance = new com.example.norman.ROSWSA.sensing.ROSDataSender(context);
        return instance;
    }

    public void putData(final long timestamp, final float[] values, Integer type) {
        SensorType t = SensorType.getFromId(type);
        //TODO HANDLE sensors updates
        Log.d(debugTag,"put data ");
        switch (t.getId()) {
            case (Sensor.TYPE_ACCELEROMETER):
                if( message.acc_timestamp != null && timestamp < message.acc_timestamp )
                    break;
                message.acc_x = values[0];
                message.acc_y = values[1];
                message.acc_z = values[2];
                message.acc_timestamp = timestamp;
                break;
            case (Sensor.TYPE_GYROSCOPE):
                if( message.gyro_timestamp != null && timestamp < message.gyro_timestamp )
                    break;
                message.gyro_x = values[0];
                message.gyro_y = values[1];
                message.gyro_z = values[2];
                message.gyro_timestamp = timestamp;
                break;
            case (Sensor.TYPE_HEART_RATE):
                Log.d(debugTag,"put data hr "+timestamp+ " < "+ message.hr_timestamp+"? ");
                if( message.hr_timestamp != null && timestamp < message.hr_timestamp )
                    break;
                message.hr = values[0];
                message.hr_timestamp = timestamp;
                Log.d(debugTag,"["+timestamp+"]put data hr "+values);
                break;
            default:
                //Log.d(debugTag,"["+timestamp+"]put data unknown sensor "+values);
                break;
        }

        try {
            // if the message contains all the data
            if (message.wellFormed()) {
                message_queue.put(new ROSMessage(message));
                rosSendDataHandler.sendEmptyMessage();
            }
        } catch (InterruptedException e){

        }
    }

    public boolean isRunning(){
        synchronized (running){
            return running[0];
        }
    }

    public void startSending(URL url) throws ROSEmptyURL{
        Log.d(debugTag, "Startsendi entering");
        if(url == null)
            throw new ROSEmptyURL();
        synchronized (running) {
            Log.d(debugTag, "startWriterHandler "+url.toString());
            if( !running[0] )
                this.url = url;
            if ( rosSendDataHandler == null || !rosSendDataHandler.isAlive() ) {
                rosSendDataHandler = new ROSSendDataHandler("RosDataSenderHandlerThread");
                rosSendDataHandler.start();
                running[0] = true;
            }
        }
    }

    public void stopSending(){
        synchronized (running) {
            if (running[0]) {
                rosSendDataHandler.quit();
                rosSendDataHandler = null;
            }
            running[0] = false;
        }
    }

}
