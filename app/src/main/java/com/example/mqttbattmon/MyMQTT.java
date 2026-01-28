package com.example.mqttbattmon;

import static android.content.Context.BATTERY_SERVICE;

import static androidx.core.content.ContextCompat.getSystemService;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.icu.number.CompactNotation;
import android.os.BatteryManager;
import android.provider.Settings;
import android.text.PrecomputedText;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.*;
import org.greenrobot.eventbus.EventBus;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Formatter;
import java.util.Locale;

public class MyMQTT {
    String mqtt_server="tcp://192.168.0.40:1883";
    String mqtt_clientid="android";
    Context context;
    String LOG_TAG="### mqtt battmon MyMqtt";

    String mqtt_topic ="android/battery";
    private MqttClient mqttClient;
    private MqttConnectOptions options;
    String notificationChannelIdD ="MQTT";

    public MyMQTT(Context context){
        this.context=context;
        String deviceStr =  Settings.Global.getString(context.getContentResolver(), "device_name");
        deviceStr=deviceStr.replace(" ", "_");
        deviceStr=deviceStr.replace("(","");
        deviceStr=deviceStr.replace(")","");
        mqtt_topic=mqtt_topic+"/"+deviceStr;
        Log.d(LOG_TAG,"mqtt_topic: "+mqtt_topic);
        try {
            mqttClient = new MqttClient(mqtt_server, mqtt_clientid, null);
            options = new MqttConnectOptions();
            options.setCleanSession(true);
            mqttClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    Log.e(LOG_TAG, "Connection lost", cause);
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    Log.d(LOG_TAG, "Message arrived: " + new String(message.getPayload()));
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    Log.d(LOG_TAG, "Message delivered: " + token.getMessageId());
                }
            });
        } catch (MqttException e) {
            Log.e(LOG_TAG, "Error initializing MQTT client", e);
        }

    }

    private String getMessageJson(int level, String charging, String timestamp){
        StringBuilder sb=new StringBuilder();
        // {
        //  "level": 55,
        //  "status": "discharging",
        //  "datetime": "17.07.2024 19:38"
        //}

        Formatter formatter = new Formatter(sb, Locale.US);
        // Explicit argument indices may be used to re-order output.
        formatter.format("{\"level\": %1$d,\"status\":\"%2$s\",\"datetime\":\"%3$s\"}", level, charging, timestamp);
        return  sb.toString();
    }
    public boolean doPublish(){
        boolean bRes=true;
        String clientId = this.mqtt_clientid;
        String topic="";
        Date date=new Date();
        try {
            SimpleDateFormat FORMATTER = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.GERMAN);
            String formattedDate=FORMATTER.format(date);
            //MqttAndroidClient client = new MqttAndroidClient(this.context, this.mqtt_server, clientId);
            mqttClient.connect();
            if (mqttClient.isConnected()) {
                BatteryInfo.BattInfo battInfo=BatteryInfo.getBattInfo(context);
                int level=battInfo.level;
                boolean charging= battInfo.charging;
                String chargingTxt;
                if (charging)
                    chargingTxt="charging";
                else
                    chargingTxt="discharging";
                String json=getMessageJson(level, chargingTxt, formattedDate);
                MqttMessage message = new MqttMessage(json.getBytes());

                //MqttMessage message = new MqttMessage(String.valueOf(level).getBytes());
                message.setRetained(true);
                topic=mqtt_topic;//+"/battery";
                mqttClient.publish(topic, message);
//                Log.d(LOG_TAG, "published: "+topic+":"+String.valueOf(level));
                Log.d(LOG_TAG, "published: "+topic+": "+json);
/*
                message = new MqttMessage(chargingTxt.getBytes());
                message.setRetained(true);
                topic=mqtt_topic+"/charging";
                mqttClient.publish(topic, message);
                Log.d(LOG_TAG, "published: "+topic+":"+chargingTxt);

                topic=mqtt_topic+"/timestamp";
                mqttClient.publish(topic, new MqttMessage(formattedDate.getBytes()));
                message.setRetained(true);
                Log.d(LOG_TAG, "published: "+topic+":"+formattedDate);
*/
                EventBus.getDefault().post(new MessageEvent("doPublish OK"));

                mqttClient.disconnect();
            }
        }catch (Exception e){
            Log.d(LOG_TAG, e.getMessage());
            bRes=false;
            EventBus.getDefault().post(new MessageEvent("doPublish failed"));

        }
        return bRes;
    }

    private Notification createNotification(){
        createChannel();
        Intent mainActivityIntent=new Intent(context, MainActivity.class);
        int pendingIntentFlag=0;
        pendingIntentFlag = PendingIntent.FLAG_IMMUTABLE;
        PendingIntent mainActivityPendingIntent=PendingIntent.getActivity(
                context,
                0,
                mainActivityIntent,
                pendingIntentFlag);
        Notification notification= new NotificationCompat.Builder(context,notificationChannelIdD)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(context.getString(R.string.app_name))
                .setContentInfo("MQTT publish done")
                .setContentIntent(mainActivityPendingIntent)
                .setAutoCancel(true)
                .build();

        return notification;
    }
    private void createChannel() {
        // Create a Notification channel
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is not in the Support Library.
        CharSequence name = "MQTT channel";
        String description = "MQTT notifications";
        int importance = NotificationManager.IMPORTANCE_DEFAULT;
        NotificationChannel channel = new NotificationChannel(notificationChannelIdD,
                "DoWork Worker",
                NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription(description);
        // Register the channel with the system. You can't change the importance
        // or other notification behaviors after this.
        NotificationManager notificationManager = getSystemService(context,NotificationManager.class);
        if(notificationManager != null)
            notificationManager.createNotificationChannel(channel);
        else{
            Log.d(LOG_TAG, "notificationManager ist NULL");
        }
    }

}
