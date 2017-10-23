package com.example.norman.shared;

import android.content.Context;
import android.hardware.Sensor;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by norman on 19/09/17.
 */

public enum SensorType {
    ACCELLEROMETER(Sensor.TYPE_ACCELEROMETER, R.string.sensor_name_accellerometer, false),
    GYROSCOPE(Sensor.TYPE_GYROSCOPE, R.string.sensor_name_gyroscope, false),
    AMBIENT_TEMPERATURE(Sensor.TYPE_AMBIENT_TEMPERATURE, R.string.sensor_name_ambient_temperature, false),
    GRAVITY(Sensor.TYPE_GRAVITY, R.string.sensor_name_gravity, false),
    LIGHT(Sensor.TYPE_LIGHT, R.string.sensor_name_light, false),
    LINEAR_ACCELLERATION(Sensor.TYPE_LINEAR_ACCELERATION, R.string.sensor_name_linear_accelleration, false),
    MAGNETIC_FIELD(Sensor.TYPE_MAGNETIC_FIELD, R.string.sensor_name_magnetic_field, false),
    ORIENTATION(Sensor.TYPE_ORIENTATION, R.string.sensor_name_orientation, true),
    PRESSURE(Sensor.TYPE_PRESSURE, R.string.sensor_name_pressure, false),
    PROXIMITY(Sensor.TYPE_PROXIMITY, R.string.sensor_name_proximity, false),
    RELATIVE_HUMIDITY(Sensor.TYPE_RELATIVE_HUMIDITY, R.string.sensor_name_relative_humidity, false),
    ROTATION_VECTOR(Sensor.TYPE_ROTATION_VECTOR, R.string.sensor_name_rotation_vector, false),
    DEVICE_TEMPERATURE(Sensor.TYPE_TEMPERATURE, R.string.sensor_name_device_temperature, true);

    private int class_id;
    private int name;
    private boolean deprecated;

    private static Map<Integer, SensorType> map = new HashMap<Integer, SensorType>();
    static{
        for(SensorType cs : SensorType.values())
            map.put(cs.getId(), cs);
    }

    public static SensorType getFromId(Integer id){
        return map.get(id);
    }

    private SensorType(int id, int name, boolean deprecated){
        this.class_id = id;
        this.name = name;
        this.deprecated = deprecated;
    }

    public int getId(){
        return class_id;
    }

    public String getName(Context context){
        return context.getString(name);
    }

    public boolean isDeprecated(){
        return deprecated;
    }
}
