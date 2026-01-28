package com.example.mqttbattmon;

import android.content.Context;
import android.os.BatteryManager;
import android.util.Log;

import static android.content.Context.BATTERY_SERVICE;

import java.util.Formatter;
import java.util.Locale;

public class BatteryInfo {

    public static BattInfo getBattInfo(Context context){
        int l=0;boolean status=false;
        BatteryManager bm = (BatteryManager)context.getSystemService(BATTERY_SERVICE);
            int percentage = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
            status = bm.isCharging();
            l=percentage;
        Log.d(MainActivity.LOG_TAG, "getLevel="+l+ (status?" charging":" discharging"));
        return new BattInfo(l, status);
    }
    public static String getBattInfoStr(Context context){
        BatteryInfo.BattInfo batteryInfo=BatteryInfo.getBattInfo(context);
        StringBuilder sb=new StringBuilder();
        Formatter formatter = new Formatter(sb, Locale.US);
        // Explicit argument indices may be used to re-order output.
        formatter.format("Level: %1$d, status: %2$s", batteryInfo.level, batteryInfo.charging);
        return  sb.toString();

    }

    public static class BattInfo{
        public int level=0;
        public boolean charging=false;
        public BattInfo(int l, boolean c){
            level=l;
            charging=c;
        }
        public String ToString(){
            return level +"% "+ (charging?"charging":"discharging");
        }
    }
}
