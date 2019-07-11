package com.aseemsethi.esp32_iot;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.Context;
import android.graphics.Color;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import static android.content.ContentValues.TAG;

/*
In manifest -             android:process=":mqtt-iot"
If the name assigned to this attribute begins with a colon (':'), a new process,
private to the application, is created when it's needed and the service runs in
that process. If the process name begins with a lowercase character, the service
will run in a global process of that name, provided that it has permission to
do so. This allows components in different applications to share a process,
reducing resource usage.
*/
public class mqttService extends Service {
    final static String MQTTMSG_ACTION = "MQTTMSG_ACTION";
    final String TAG = "ESP32IOT mqttService";
    NotificationManager mNotificationManager;
    Notification notification;
    String CHANNEL_ID = "my_channel_01";

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void runMyTask() {

        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    while(true) {
                        Intent intent1 = new Intent();
                        intent1.setAction(MQTTMSG_ACTION);
                        intent1.putExtra("MQTTRCV", "MQTT Msg received");
                        sendBroadcast(intent1);
                        Thread.sleep(5000);
                        sendNotification();
                    }
                }
                catch (InterruptedException e) {}
            }
        };
        thread.start();
    }
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Started mqttService");
        mNotificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID,
                "my_channel_01",
                NotificationManager.IMPORTANCE_LOW);
        mChannel.enableLights(true);
        mChannel.setLightColor(Color.RED);
        mChannel.enableVibration(true);
        mChannel.setVibrationPattern(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400});
        mNotificationManager.createNotificationChannel(mChannel);

        runMyTask();
        return START_STICKY;
    }
    @Override
    public void onDestroy() {
        Log.d(TAG, "Destroyed mqttService");
        super.onDestroy();
        //stopping the player when service is destroyed
    }

    private void sendNotification() {

        Log.d(TAG, "Send Notification...");

        // Create an explicit intent for an Activity in your app
        Intent intent = new Intent(this, mqttService.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_menu_camera)
                .setContentTitle("Security Notification")
                .setContentText("Alert")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                // Set the intent that will fire when the user taps the notification
                .setContentIntent(pendingIntent)
                .setChannelId(CHANNEL_ID)
                .setAutoCancel(true);

        // Build the notification.
        notification = builder.build();
        mNotificationManager.notify(0, notification);

        // Start foreground service.
        startForeground(1, notification);
    }

}
