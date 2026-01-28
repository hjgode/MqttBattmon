package com.example.mqttbattmon;

import android.content.Context;
import android.provider.Settings;

import androidx.annotation.NonNull;

public class DeviceName {
    @NonNull
    public static String get_device_name(@NonNull Context context){
        StringBuilder deviceSB;
        String deviceStr =  Settings.Global.getString(context.getContentResolver(), "device_name");

        deviceStr=deviceStr.replace(" ", "_");
        deviceStr=deviceStr.replace("(","");
        deviceStr=deviceStr.replace(")","");

        return deviceStr;
    }
}
