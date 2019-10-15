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
import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

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
    final static String MQTTDELETE_SENSOR_ACTION = "MQTTDELETE_SENSOR_ACTION";
    final static String MQTTMSG_MSG = "com.aseemsethi.esp32_iot.mqttService.MQTTMSG_MSG";
    final String TAG = "ESP32IOT mqttService";
    NotificationManager mNotificationManager;
    Notification notification;
    String CHANNEL_ID = "my_channel";
    MqttHelper mqttHelper;
    String mqtt_token = "";
    static int counter = 0;
    private final static int OPEN_CODE=0;
    private final static int CLOSE_CODE=1;
    private final static int UNKNOWN_CODE=2;
    private final static int SENSOR_COUNT=10;
    private class sensorT {
        public String sensorName;
        public String sensorTag;
        public String notifyOn;
        public int    id;
        Button btn;
        int status_code;  // OPEN or CLOSE CODE
        //Time lastTimeChanged;
        String lastTimeChanged;
    };
    sensorT sensorStruct[];
    String lastMqttTopic="";
    boolean firstTime = true;

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
                //if (firstTime == true) {
                if (lastMqttTopic != null) {
                    Log.d(TAG, "Subscribing to last MQTT Topic:: " + lastMqttTopic);
                    mqttHelper.subscribeToTopic(lastMqttTopic);
                    firstTime = false;
                }
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
                    ///data/user/0/com.aseemsethi.esp32_iot/files/esp32Notifications
                    fos = openFileOutput("esp32Notifications", Context.MODE_APPEND);
                    fos.write(msg.getBytes());
                    fos.close();
                    Log.d(TAG, "Written msg to Notifications: " + msg);
                } catch (FileNotFoundException e) {e.printStackTrace();}
                catch (IOException e) {e.printStackTrace();}
                updateDB(mqttMessage.toString());
            }
            @Override
            public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
            }
        });
    }

    void updateDB(String msg) {
        String[] arrOfStr = msg.split(":", 4);
        int id = parseWithDefault(arrOfStr[0], SENSOR_COUNT);
        if (id == SENSOR_COUNT) {
            Log.d(TAG, "Cannot associate BLE with a Sensor");
            return;
        }
        if (id == 0) {
            Log.d(TAG, "This is a test MQTT Msg...return true");
            // Enter this msg into the Critical File to be read from UI
            FileOutputStream fos;
            try {
                fos = openFileOutput("esp32Critical", Context.MODE_APPEND);
                fos.write(msg.getBytes());
                fos.close();
                Log.d(TAG, "Written msg to Critical: " + msg);
            } catch (FileNotFoundException e) {e.printStackTrace();}
            catch (IOException e) {e.printStackTrace();}
            return;
        }
        for (int i = 0; i < SENSOR_COUNT; i++) {
            if (sensorStruct[i].id == id) {
                Log.d(TAG, "Found sensor in sensorStruct");
                DBHandler db = new DBHandler(getApplicationContext());
                HashMap<String, String> sensorList =
                        db.GetSensorBySensorId(id);
                if (sensorList != null) {
                    Log.d(TAG, "Found sensor in sensorDB: Old Status: " +
                            sensorList.get("status") + id);
                    if ((arrOfStr[2].trim()).equals(sensorList.get("status").trim())) {
                        Log.d(TAG, "No Sensor state change: " + sensorList.get("status") +
                                "-> " + (arrOfStr[2].trim()));
                    } else {
                        //sensorStruct[i].lastTimeChanged =
                        //        new Time(Calendar.getInstance().getTimeInMillis());
                        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy ':' HH:mm:ss");
                        sensorStruct[i].lastTimeChanged = sdf.format(new Date());
                        Log.d(TAG, "Sensor update: " + sensorList.get("status") +
                                "-> " + (arrOfStr[2].trim()) + " : " +
                                sensorStruct[i].lastTimeChanged);
                        int count = db.UpdateSensorTime(sensorStruct[i].lastTimeChanged.toString(),
                                id);
                    }
                    int count = db.UpdateSensorDetails(arrOfStr[2], id);
                    Log.d(TAG, "Updated Sensor SQLite DB");
                    ArrayList<HashMap<String, String>> sensorL = db.GetSensors();
                    Log.d(TAG, sensorL.toString());
                }
            }
        }
    }

    boolean poilcyAllows(String msg) {
        String[] arrOfStr = msg.split(":", 4);
        int id = parseWithDefault(arrOfStr[0], SENSOR_COUNT);
        if (id == SENSOR_COUNT) {
            Log.d(TAG, "Cannot associate BLE with a Sensor");
            return false;
        }
        if (id == 0) {
            Log.d(TAG, "This is a test MQTT Msg...return true");
            return true;
        }
        for (int i = 0; i < SENSOR_COUNT; i++) {
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
        int id;

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
                mqttHelper.subscribeToTopic(mqtt_token); break;
            case MQTTUPDATE_SENSOR_ACTION:
                id = intent.getIntExtra("id", 0);
                String notifyOn = intent.getStringExtra("notifyOn");
                lastMqttTopic = intent.getStringExtra("lastMqttTopic");
                Log.d(TAG, "Recvd Sensor info from Main MQTTUPDATE_SENSOR_ACTION: " +
                        id + ":" + notifyOn +  ":" + lastMqttTopic);
                sensorStruct[id].id = id;
                sensorStruct[id].notifyOn = notifyOn;
                break;
            case MQTTDELETE_SENSOR_ACTION:
                id = intent.getIntExtra("id", 0);
                Log.d(TAG, "Recvd Sensor info from Main MQTTDELETE_SENSOR_ACTION:" + id);
                for (int i = 0; i < 9; i++) {
                    if (sensorStruct[i].id == id) {
                        Log.d(TAG, "Deleting Sensor: " + id);
                        sensorStruct[i] = new sensorT();
                    }
                }
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
