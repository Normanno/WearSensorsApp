package com.example.norman.ROSWSA.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.example.norman.ROSWSA.R;
import com.example.norman.ROSWSA.sensing.DeviceSensingManager;

import java.util.ArrayList;

/**
 * Created by norman on 26/09/17.
 */

public class SensorCustomAdapter extends ArrayAdapter<DeviceSensingManager.SensorItem> implements View.OnClickListener{

    private ArrayList<DeviceSensingManager.SensorItem> sensors;
    private ArrayList<Integer> ids;
    private Context context;

    public SensorCustomAdapter(@NonNull Context context, @NonNull ArrayList<DeviceSensingManager.SensorItem> data) {
        super(context, R.layout.sensors_listview_item , data);

        this.sensors = data;
        this.context = context;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        DeviceSensingManager.SensorItem sensor = sensors.get(position);

        View v = convertView;
        if( v == null){
            v = LayoutInflater.from(context).inflate(R.layout.sensors_listview_item, null);
        }

        if( sensor != null){
            TextView sName = (TextView) v.findViewById(R.id.sensor_name);
            TextView sType = (TextView) v.findViewById(R.id.sensor_type);
            TextView sObserved = (TextView) v.findViewById(R.id.sensor_observed);
            TextView sGender = (TextView) v.findViewById(R.id.sensor_gender);
            if(sName != null)
                sName.setText(sensor.name);

            if(sType != null)
                sType.setText(sensor.type);

            if(sObserved != null) {
                String text = (sensor.listening ? this.context.getString(R.string.sensor_status_tv_active):this.context.getString(R.string.sensor_status_tv_not_active));
                int color = this.context.getColor(sensor.listening ? R.color.textActive: R.color.textNotActive);
                sObserved.setText(text);
                sObserved.setTextColor(color);
            }

            if(sGender != null){
                sGender.setText(sensor.gender.toString());
                sGender.setTextColor(this.context.getColor(R.color.textOnWhiteBgEvidence));

            }
        }

        return v;
    }

    @Override
    public void onClick(View view) {

    }
}
