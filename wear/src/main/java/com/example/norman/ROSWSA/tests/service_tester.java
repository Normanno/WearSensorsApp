package com.example.norman.ROSWSA.tests;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.util.Log;

import com.example.norman.ROSWSA.R;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

/**
 * Created by norman on 19/09/17.
 */

public class service_tester {
    private HandlerThread mHandlerThread;
    private Looper mServiceLooper;
    private ServiceHandler mServiceHandler;
    private GoogleApiClient mGoogleApiClient;
    private Context context;
    private static String debugTag = "WearableListener";

    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }
        @Override
        public void handleMessage(Message msg) {
            for( int i = 0; i < 5; i++){
                PutDataMapRequest pdmr = PutDataMapRequest.create(context.getString(R.string.DATA_SYNC_SENSOR_DATA));
                pdmr.getDataMap().putLong("timestamp",System.currentTimeMillis());// used to force the onDataChanged event
                // pdmr.getDataMap().putLong("test",2);// used to force the onDataChanged event
                pdmr.setUrgent();
                PutDataRequest pdr = pdmr.asPutDataRequest();
                pdr.setUrgent();
                PendingResult<DataApi.DataItemResult> pendingResult = Wearable.DataApi.putDataItem(mGoogleApiClient, pdr);
                pendingResult.setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                    @Override
                    public void onResult(final DataApi.DataItemResult result) {
                        Log.d(debugTag, "Tester: " + result.getDataItem().getUri() + " - " + result.getStatus().getStatusMessage());
                    }
                });
            }
        }
    }

    public service_tester(Context c){
        this.context = c;
        this.mHandlerThread= new HandlerThread("ServiceStartArguments",
                Process.THREAD_PRIORITY_BACKGROUND);
        this.mHandlerThread.start();

        this.mGoogleApiClient = new GoogleApiClient.Builder(c)
                .addApi(Wearable.API)
                .build();
        mGoogleApiClient.connect();

        mServiceLooper = this.mHandlerThread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);

        Log.d(debugTag, "service created");
    }

    public void send(){
        mServiceHandler.sendEmptyMessage(1);
    }

    public void quit(){
        this.mHandlerThread.quit();
        if (this.mGoogleApiClient.isConnected() || this.mGoogleApiClient.isConnecting())
            this.mGoogleApiClient.disconnect();
    }

}
