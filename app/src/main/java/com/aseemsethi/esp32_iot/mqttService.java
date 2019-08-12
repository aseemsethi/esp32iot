package com.aseemsethi.esp32_iot;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.Context;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.IBinder;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Button;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

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
    final static String MQTTMSG_ACTION = "com.aseemsethi.esp32_iot.mqttService.MQTTMSG_ACTION";
    final static String MQTTSUBSCRIBE_ACTION = "MQTTSUBSCRIBE_ACTION";
    final static String MQTTUPDATE_SENSOR_ACTION = "MQTTUPDATE_SENSOR_ACTION";
    final static String MQTTMSG_MSG = "com.aseemsethi.esp32_iot.mqttService.MQTTMSG_MSG";
    final String TAG = "ESP32IOT mqttService";
    NotificationManager mNotificationManager;
    Notification notification;
    String CHANNEL_ID = "my_channel";
    MqttHelper mqttHelper;
    String mqtt_token = "";
    static int counter = 0;
    private class sensorT {
        public String sensorName;
        public String sensorTag;
        public String notifyOn;
        public int    id;
        Button btn;
    };
    sensorT sensorStruct[];

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void runMyWork() {
        mqttHelper = new MqttHelper(getApplicationContext());
        Log.d(TAG, "mqttService thread..");
        mqttHelper.mqttAndroidClient.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean b, String s) {
                Log.w(TAG, "mqttService - Connected");
            }
            @Override
            public void connectionLost(Throwable throwable) {
            }
            @Override
            public void messageArrived(String topic, MqttMessage mqttMessage) throws Exception {
                Log.d(TAG, "MQTT Recvd: " + mqttMessage.toString());
                counter++;
                Intent intent1 = new Intent();
                intent1.setAction(MQTTMSG_MSG);
                intent1.putExtra("MQTTRCV", mqttMessage.toString());
                sendBroadcast(intent1);
                if (poilcyAllows(mqttMessage.toString())) {
                    Log.d(TAG, "Policy allows notification");
                    sendNotification(mqttMessage.toString());
                } else {
                    Log.d(TAG, "Policy does not allow notification");
                }
                FileOutputStream fos;
                try {
                    String msg = mqttMessage.toString() + ":" + counter + "\n";
                    fos = openFileOutput("esp32Notifications", Context.MODE_APPEND);
                    fos.write(msg.getBytes());
                    fos.write(":".getBytes());
                    fos.close();
                } catch (FileNotFoundException e) {e.printStackTrace();}
                catch (IOException e) {e.printStackTrace();}
            }
            @Override
            public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
            }
        });
    }

    boolean poilcyAllows(String msg) {
        String[] arrOfStr = msg.split(":", 4);
        int id = parseWithDefault(arrOfStr[0], 10);
        if (id == 10) {
            Log.d(TAG, "Cannot associate BLE with a Sensor");
            return false;
        }
        if (id == 0) {
            Log.d(TAG, "This is a test MQTT Msg...return true");
            return true;
        }
        for (int i = 0; i < 9; i++) {
            if (sensorStruct[i].id == id) {
                Log.d(TAG, "Comparing: " + sensorStruct[i].notifyOn + ":" + arrOfStr[2]+":");
                if ((sensorStruct[i].notifyOn).equals(arrOfStr[2].trim())) {
                    return true;
                } else
                    return false;
            }
        }
        return false;
    }

    int parseWithDefault(String s, int def) {
        try {
            return Integer.parseInt(s);
        }
        catch (NumberFormatException e) {
            // It's OK to ignore "e" here because returning a default value is the documented behaviour on invalid input.
            return def;
        }
    }
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action;

        Log.d(TAG, "Started mqttService");
        if (intent == null) {
            Log.d(TAG, "Intent is null..possible due to system restart");
            action = MQTTMSG_ACTION;
        } else
            action = intent.getAction();
        Log.d(TAG,"ACTION: "+action);
        switch (action) {
            case MQTTMSG_ACTION:
                Log.d(TAG, "Starting mqttService first time !!");
                sensorStruct = new sensorT[10];
                for (int i = 0; i < 9; i++) {
                    sensorStruct[i] = new sensorT();
                }
                mNotificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
                NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID,
                        "my_channel",
                        NotificationManager.IMPORTANCE_HIGH);
                mChannel.enableLights(true);
                mChannel.setLightColor(Color.RED);
                mNotificationManager.createNotificationChannel(mChannel);
                enableNotification();
                runMyWork();
                break;
            case MQTTSUBSCRIBE_ACTION:
                mqtt_token = intent.getStringExtra("topic");
                Log.d(TAG, "Recvd MQTT Token to subscribe: " + mqtt_token);
                mqttHelper.subscribeToTopic(mqtt_token);
            case MQTTUPDATE_SENSOR_ACTION:
                int id = intent.getIntExtra("id", 0);
                String notifyOn = intent.getStringExtra("notifyOn");
                Log.d(TAG, "Recvd Sensor info from Main: " + id + ":" + notifyOn);
                sensorStruct[id].id = id;
                sensorStruct[id].notifyOn = notifyOn;
        }
        return START_STICKY;
    }
    @Override public void onTaskRemoved(Intent rootIntent) {
        Log.d(TAG, "Task Removed mqttService");
        super.onTaskRemoved(rootIntent);
        //mqttHelper.unsubscribeToTopic(mqtt_token);

        Log.d(TAG, "onTaskRemoved...attempting to Start mqttService..");
        sendBroadcast(new Intent("RestartMqtt"));
    }
    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy...attempting to restart mqttService");
        //
        // mqttHelper.unsubscribeToTopic(mqtt_token);
        super.onDestroy();
        sendBroadcast(new Intent("RestartMqtt"));
    }

    private void enableNotification() {
        Log.d(TAG, "Send Notification...");

        // Create an explicit intent for an Activity in your app
        Intent intent = new Intent(this, mqttService.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        Uri defaultSoundUri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_menu_camera)
                .setContentTitle("Security")
                .setContentText("Alerts ON")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                // Set the intent that will fire when the user taps the notification
                .setContentIntent(pendingIntent)
                .setSound(Settings.System.DEFAULT_NOTIFICATION_URI)
                .setSound(defaultSoundUri)
                .setChannelId(CHANNEL_ID)
                .setAutoCancel(true);

        // Build the notification.
        notification = builder.build();
        // Start foreground service.
        startForeground(1, notification);
    }

    private void sendNotification(String msg) {
        Log.d(TAG, "Send Notification...");

        // Create an explicit intent for an Activity in your app
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        Uri defaultSoundUri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_menu_camera)
                .setContentTitle("Security Notification: " + counter)
                //.setContentText(msg)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(msg + counter))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                // Set the intent that will fire when the user taps the notification
                .setContentIntent(pendingIntent)
                .setSound(defaultSoundUri)
                .setChannelId(CHANNEL_ID)
                .setAutoCancel(true);

        // Build the notification.
        notification = builder.build();
        mNotificationManager.notify(0, notification);
    }

}
