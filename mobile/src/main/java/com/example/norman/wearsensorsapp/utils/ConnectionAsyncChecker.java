package com.example.norman.wearsensorsapp.utils;

import android.os.AsyncTask;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by norman on 24/10/17.
 */

public class ConnectionAsyncChecker extends AsyncTask {

    private URL url;
    private boolean checked;
    private boolean verified;


    public ConnectionAsyncChecker(){ }


    @Override
    protected Object doInBackground(Object[] objects) {
        //URL TEST
        int responseCode = -1;
        try {
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("HEAD");
            responseCode = conn.getResponseCode();
        }catch (IOException e){
            return false;
        }

        checked = true;
        verified = responseCode == 200;
        return responseCode == 200;
    }

    public void setUrl(URL url){
        this.url = url;
    }

    public boolean isChecked(){
        return checked;
    }

    public boolean isVerified(){
        return verified;
    }

    public void verify(){
        this.execute("");
    }
}
