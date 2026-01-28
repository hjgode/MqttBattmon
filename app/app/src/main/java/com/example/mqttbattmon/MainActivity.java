package com.example.mqttbattmon;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {
    Context myContext;
    WorkManager workManager;
    final static String LOG_TAG="mqttBattery";
    Button btn_Start;
    TextView textview;
    TextView textAkkusstand;
    TextView textStatus;
    // Unique request code for permission request
    private static final int PERMISSION_REQUEST_CODE = 123;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        myContext=getApplicationContext();
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        textview=(TextView)findViewById((R.id.textView));
        textview.setText(DeviceName.get_device_name(myContext));

        textAkkusstand=(TextView)findViewById(R.id.textViewAkkustand);
        textAkkusstand.setText(BatteryInfo.getBattInfoStr(myContext));
        textStatus=(TextView)findViewById(R.id.textViewStatus);

        btn_Start=(Button) findViewById(R.id.btn_Start);
        if (isWorkScheduled()){
            Log.d(LOG_TAG, "Workrequest already done");
        }
        btn_Start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isWorkScheduled();
                startWork();
            }
        });
        Button btnTest=(Button) findViewById(R.id.btn_Test);
        btnTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MyMQTT mqtt=new MyMQTT(myContext);
                boolean bRes = mqtt.doPublish();
            }
        });
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Log.d(LOG_TAG, "requestPermissions()...");
            requestPermissions();
        }else{
            Log.d(LOG_TAG, "requestPermissions() not called");
        }
    }

    private boolean isWorkScheduled() {
        String tag = "saveRequest";
        tag = "com.example.mqttbattmon.UploadWorker";
        WorkManager instance = WorkManager.getInstance(myContext);
        ListenableFuture<List<WorkInfo>> statuses = instance.getWorkInfosByTag(tag);
        try {
            boolean running = false;
            List<WorkInfo> workInfoList = statuses.get();
            for (WorkInfo workInfo : workInfoList) {
                WorkInfo.State state = workInfo.getState();
                running = state == WorkInfo.State.RUNNING | state == WorkInfo.State.ENQUEUED;
                if (state == WorkInfo.State.RUNNING)
                    Log.d(LOG_TAG, "worker running");
                if (state == WorkInfo.State.ENQUEUED)
                    Log.d(LOG_TAG, "worker enqueud");

            }
            if(!running) {
                Log.d(LOG_TAG, "worker not running");
                textStatus.setText("Worker not running");
            }else{
                textStatus.setText("Worker running");
            }
            return running;
        } catch (ExecutionException e) {
            Log.e(LOG_TAG,e.getMessage());
            return false;
        } catch (InterruptedException e) {
            Log.e(LOG_TAG,e.getMessage());
            return false;
        }
    }
    void startWork(){
        /*
        WorkRequest uploadWorkRequest =
                new OneTimeWorkRequest.Builder(UploadWorker.class)
                        .build();
        WorkManager
                .getInstance(myContext)
                .enqueue(uploadWorkRequest);
        */
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest saveRequest =
                new PeriodicWorkRequest.Builder(UploadWorker.class, 15, TimeUnit.MINUTES)
                        // Constraints
                        .setConstraints(constraints)
                        .setInitialDelay(10, TimeUnit.SECONDS)
                        .build();
        workManager=WorkManager.getInstance(myContext);
        workManager
                .enqueueUniquePeriodicWork("saveRequest", ExistingPeriodicWorkPolicy.REPLACE, saveRequest);
    }
    void testWorkerState(){
        // by tag
        workManager.getWorkInfosByTag("saveRequest");

    }
    void cancel(){
        // by tag
//        workManager.cancelAllWorkByTag("saveRequest");
        workManager.cancelUniqueWork("saveRequest");
    }

    @Override
    protected void onPause(){
        super.onPause();
/*    ForegroundInfo getForegroundInfo(){

    }

 */
    }
    // Function to check and request necessary permissions
    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    private void requestPermissions() {

        // List of permissions the app may need
        String[] permissions = {
                Manifest.permission.POST_NOTIFICATIONS
        };

        List<String> permissionsToRequest = new ArrayList<>();

        // Filter out the permissions that are not yet granted
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
            }
        }

        // If there are permissions that need to
        // be requested, ask the user for them
        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(
                    this,
                    permissionsToRequest.toArray(new String[0]), // Convert list to array
                    PERMISSION_REQUEST_CODE // Pass the request code
            );
        } else {
            // All permissions are already granted
            Toast.makeText(this, "All permissions already granted", Toast.LENGTH_SHORT).show();
        }
    }

    // Callback function that handles the
    // result of the permission request
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            List<String> deniedPermissions = new ArrayList<>();

            // Check which permissions were denied
            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    deniedPermissions.add(permissions[i]);
                }
            }

            if (deniedPermissions.isEmpty()) {

                // All permissions granted
                Toast.makeText(this, "All permissions granted", Toast.LENGTH_SHORT).show();
            } else {

                // Some permissions were denied, show them in a Toast
                Toast.makeText(this, "Permissions denied: " + deniedPermissions, Toast.LENGTH_LONG).show();
            }
        }
    }
}