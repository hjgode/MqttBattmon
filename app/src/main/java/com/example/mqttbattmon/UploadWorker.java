package com.example.mqttbattmon;

import static androidx.core.content.ContextCompat.getAttributionTag;
import static androidx.core.content.ContextCompat.getSystemService;

import android.Manifest;
import android.app.Application;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.ForegroundInfo;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.security.Permission;


public class UploadWorker extends Worker {
    int notificationId=99;
    String notificationChannelIdD ="MQTT";
    final String LOG_TAG="## MQTT dWorker";
    Context workContext;
    MyMQTT mqtt;
    public UploadWorker(
            @NonNull Context context,
            @NonNull WorkerParameters params) {
        super(context, params);
        workContext=context;
        mqtt=new MyMQTT(workContext);
    }

    @NonNull
    @Override
    public Result doWork() {

        setForegroundAsync(createForegroundInfo("started"));
        Log.d(LOG_TAG, "doWork() ");
        // Do the work here--in this case, upload the images.
        mqtt.doPublish();

        if(ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED){
            NotificationManagerCompat notificationManagerCompat;
            notificationManagerCompat= NotificationManagerCompat.from(getApplicationContext());
            notificationManagerCompat.notify(0, createNotification());
            Log.d(LOG_TAG,"Notification done");
        }
        else{
            Log.d(LOG_TAG,"POST_Notification not granted");
        }
        // Indicate whether the work finished successfully with the Result
        return Result.success();

        /*
        01-02 20:02:54.591  9127  9127 I WM-SystemFgDispatcher: Started foreground service Intent { act=ACTION_START_FOREGROUND cmp=com.example.mqttbattmon/androidx.work.impl.foreground.SystemForegroundService (has extras) }
        01-02 20:02:54.602  9127  9151 I WM-WorkerWrapper: Worker result SUCCESS for Work [ id=beaeca17-fbdd-451c-aca3-7563d7f6c20f, tags={ com.example.mqttbattmon.UploadWorker } ]
        01-02 20:02:54.707  2081  2081 E NotificationService: Muting recently noisy 0|com.example.mqttbattmon|99|null|10484
         */
    }

    private Notification createNotification(){
        createChannel();
        Intent mainActivityIntent=new Intent(getApplicationContext(), MainActivity.class);
        int pendingIntentFlag=0;
        pendingIntentFlag = PendingIntent.FLAG_IMMUTABLE;
        PendingIntent mainActivityPendingIntent=PendingIntent.getActivity(
                getApplicationContext(),
                0,
                mainActivityIntent,
                pendingIntentFlag);
        Notification notification= new NotificationCompat.Builder(getApplicationContext(),notificationChannelIdD)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(getApplicationContext().getString(R.string.app_name))
            .setContentInfo("MQTT publish done")
            .setContentIntent(mainActivityPendingIntent)
            .setAutoCancel(true)
//                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setSilent(true)
            .build();

        return notification;
    }


    @Override
    @NonNull
    public ForegroundInfo getForegroundInfo(){
        return createForegroundInfo("MQTT info");
    }
    private ForegroundInfo createForegroundInfo(@NonNull String progress) {
        // Build a notification using bytesRead and contentLength

        Context context = getApplicationContext();

        String title = context.getString(R.string.notification_title);
        String cancel = context.getString(R.string.cancel_download);
        // This PendingIntent can be used to cancel the worker
        PendingIntent intent = WorkManager.getInstance(context)
                .createCancelPendingIntent(getId());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createChannel();
        }

        Notification notification = createNotification();
                /*
                new NotificationCompat.Builder(context, notificationChannelIdD)
                .setContentTitle(title)
                .setTicker(progress)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setOngoing(true)
                .setPriority(NotificationManager.IMPORTANCE_DEFAULT)
                // Add the cancel action to the notification which can
                // be used to cancel the worker
                .addAction(android.R.drawable.ic_delete, cancel, intent)
                .build();
                */
        ForegroundInfo foregroundInfo;
        foregroundInfo = new ForegroundInfo(
                notificationId,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        );
        return foregroundInfo;
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
                importance);
        channel.setDescription(description);
//        channel.setSound(null,null);
        // Register the channel with the system. You can't change the importance
        // or other notification behaviors after this.
        NotificationManager notificationManager = getSystemService(getApplicationContext(),NotificationManager.class);
        if(notificationManager != null) {
//            notificationManager.deleteNotificationChannel(channel.getId());
            notificationManager.createNotificationChannel(channel);
        }
        else{
            Log.d(LOG_TAG, "notificationManager ist NULL");
        }
    }
}