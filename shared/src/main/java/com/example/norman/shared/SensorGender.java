package com.example.norman.shared;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by norman on 27/09/17.
 */

public enum SensorGender {
    RAW(0), STANDARD(1);

    private int typeId;

    private static Map<Integer, SensorGender> map = new HashMap<>();

    static{
        for(SensorGender st : SensorGender.values())
            map.put(st.getId(), st);

    }

    private SensorGender(int type){
        typeId = type;
    }

    public Integer getId(){
        return typeId;
    }

    public static SensorGender getFromId(Integer id){
        return map.get(id);
    }

}

