package com.aseemsethi.esp32_iot;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.support.v4.view.GravityCompat;
import android.support.v7.app.ActionBarDrawerToggle;
import android.view.MenuItem;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;

import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.rvirin.onvif.onvifcamera.OnvifDevice;
import com.rvirin.onvif.onvifcamera.OnvifListener;
import com.rvirin.onvif.onvifcamera.OnvifRequest;
import com.rvirin.onvif.onvifcamera.OnvifResponse;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Time;
import java.util.ArrayList;
import java.util.HashMap;

import io.evercam.network.discovery.DiscoveredCamera;

import static com.rvirin.onvif.onvifcamera.OnvifDeviceKt.currentDevice;

import com.pedro.vlc.VlcListener;
import com.pedro.vlc.VlcVideoLibrary;

// How to import esptouch module.
// File --> New --> Import Module - point this to the esptouch directory locally downloaded
// from https://github.com/EspressifApp/EsptouchForAndroid
// In gradle file add a line - implementation project(':esptouch')
// In settings.gradle - modify to : include ':app', ':esptouch'
//
// ONVIF - https://github.com/vardang/onvif/blob/master/app/build.gradle
// Use implements OnvifListener, and add
// implementation 'com.rvirin.onvif:onvifcamera:1.1.8' to gradle app
// and import the Onvif statements.
// Add in build.gradle - classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlin_version"
//
// Goto https://github.com/evercam/evercam-android/tree/master/evercamPlay/src
// Copy the libs/jar files (3 jar files) into our lib directory
// Now we can use evercam lib for searching cameras
//
// For ONVIF, goto https://github.com/rvi/ONVIFCameraAndroid
// Download the onvifcamera directory from this package into a local directory
// File --> New --> Import Module the above onvifcamera directory
// In gradle file add a line - implementation project(':onvifcamera')
// In settings.gradle - modify to : include ':app', ':onvifcamera'
//
// Web Services Dynamic Discovery (WS-Discovery) is a technical specification that
// defines a multicast discovery protocol to locate services on a local network.
// It operates over TCP and UDP port 3702 and uses IP multicast address
// 239.255.255.250. Communication between nodes is done using SOAP-over-UDP.
// UDP ports 139, 445, 1124, 3702 TCP ports 139, 445, 3702, 49179, 5357,5358
//
public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, OnvifListener, VlcListener {

    final String TAG = "ESP32IOT MainActivity";
    private final static int REQUEST_CODE_1 = 1; // for mDNS
    private final static int REQUEST_CODE_2 = 2; // for push notifications
    private final static int REQUEST_CODE_3 = 3; // for mqtt topic
    private final static int REQUEST_CODE_4 = 4; // for sensors
    private final static int REQUEST_CODE_5 = 5; // for notificationStatus
    private final static int REQUEST_CODE_6 = 6; // for esptouch
    private final static int REQUEST_CODE_7 = 7; // for logs
    private final static int REQUEST_CODE_8 = 8; // for mrt
    private final static int REQUEST_CODE_9 = 9; // for addCamera
    private final static int REQUEST_CODE_10 = 10; // for scanCamera

    private static final int REQUEST_WIFI = 1;
    private static final String KEY_RESPONSE_TEXT = "KEY_RESPONSE_TEXT";
    private Handler uiUpdater = null;
    private HistoryAdapter mAdapter;
    RecyclerView mRecyclerView;
    String deviceAddress = "";
    String lastMqttTopic ="";
    private AlphaAnimation buttonClick = new AlphaAnimation(1F, 0.1F);
    MqttHelper mqttHelper;
    String mqtt_token = "";
    //MyReceiver myReceiver;
    BroadcastReceiver myReceiverMqtt = null;
    BroadcastReceiver myReceiverMqttMsg = null;
    private final static int OPEN_CODE=0;
    private final static int CLOSE_CODE=1;
    private final static int UNKNOWN_CODE=2;
    private final static int SENSOR_COUNT=10;
    private final static int CAMERA_COUNT=10;

    private class sensorT {
        public String sensorName;
        public String sensorTag;
        public String notifyOn;
        public int    id;
        Button        btn;
        TableRow      tr;
        TextView      tv;
        int status_code;  // OPEN or CLOSE CODE
        Time lastTimeChanged;
    };
    sensorT sensorStruct[];

    private class cameraT {
        String      ipaddress;
        OnvifDevice currentDevice;
        TextView    tv;
        SurfaceView iv;
        int         id;
        String      rtspStream;
    };
    cameraT cameraStruct[];
    int cameraID = 1;
    Activity thisActivity;
    boolean cameraClick = false;
    int currentID = 0;
    String whereToSave;
    VlcVideoLibrary vlcVideoLibrary = null;

    final static String MQTTMSG_MSG = "com.aseemsethi.esp32_iot.mqttService.MQTTMSG_MSG";
    private long lastTouchTime = 0;
    private long currentTouchTime = 0;
    private ArrayList<DiscoveredCamera> onvifDeviceList =
            new ArrayList<DiscoveredCamera>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        thisActivity = this;

        sensorStruct = new sensorT[SENSOR_COUNT];
        for (int i = 0; i < SENSOR_COUNT; i++) {
            sensorStruct[i] = new sensorT();
            sensorStruct[i].status_code = UNKNOWN_CODE;
        }
        cameraStruct = new cameraT[CAMERA_COUNT];
        for (int i = 0; i < CAMERA_COUNT; i++) {
            cameraStruct[i] = new cameraT();
        }
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        navigationView.setNavigationItemSelectedListener(this);

        navigationView.setNavigationItemSelectedListener(this);
        final Button nodeB = findViewById(R.id.node);
        nodeB.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.startAnimation(buttonClick);
                Context context = getApplicationContext();
                Intent intent = new Intent(context, mdnsActivity.class);
                startActivityForResult(intent, REQUEST_CODE_1);
            }
        });
        final Button wifiB = findViewById(R.id.wifi_b);
        wifiB.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(deviceAddress.isEmpty()) {
                    //mAdapter.add("Select an IOT Node first", Color.BLUE);
                    //mRecyclerView.smoothScrollToPosition(mAdapter.getItemCount());
                    return;
                }
                wifiB.setText("WIFI");
                v.startAnimation(buttonClick);
                //mAdapter.add("Requesting WiFi Status..", Color.BLUE);
                //mRecyclerView.smoothScrollToPosition(mAdapter.getItemCount());
                String uri = "http://" + deviceAddress + ":8080/check?wifi=1";
                startSendHttpRequestThread(uri);
            }
        });
        final Button tempb = findViewById(R.id.temp_b);
        tempb.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(deviceAddress.isEmpty()) {
                    //mAdapter.add("Select an IOT Node first", Color.BLUE);
                    //mRecyclerView.smoothScrollToPosition(mAdapter.getItemCount());
                    return;
                }
                tempb.setText("TEMP");
                v.startAnimation(buttonClick);
                //mAdapter.add("Requesting Temperature Status..", Color.BLUE);
                //mRecyclerView.smoothScrollToPosition(mAdapter.getItemCount());
                String uri = "http://" + deviceAddress + ":8080/check?temp=1";
                startSendHttpRequestThread(uri);
            }
        });
        /*
        mRecyclerView = (RecyclerView) findViewById(R.id.main_recycler_view);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mAdapter = new HistoryAdapter(new ArrayList<String>());
        mRecyclerView.setAdapter(mAdapter);

        mAdapter.add("To Start Monitoring:", Color.BLUE);
        mAdapter.add("Enable Menu -> Device Discovery and Menu -> Set Notifications", Color.BLUE);
        mRecyclerView.smoothScrollToPosition(mAdapter.getItemCount());
        */

        // Read Device from file
        try {
            BufferedReader inputReader = new BufferedReader(new InputStreamReader(
                    openFileInput("esp32configNode")));
            String inputString;
            while ((inputString = inputReader.readLine()) != null) {
                deviceAddress = inputString;
                Log.d(TAG, "Read Device Node from file: " + deviceAddress);
                TextView textView = (TextView)findViewById(R.id.node);
                textView.setText(deviceAddress);
            }
        } catch (IOException e) { e.printStackTrace();}

        if (isMyServiceRunning()) {
            Log.d(TAG, "MainActivity onCreate: service is already running");
        } else {
            Context context = getApplicationContext();
            Intent serviceIntent = new Intent(context, mqttService.class);
            serviceIntent.setAction(mqttService.MQTTMSG_ACTION);
            Log.d(TAG, "Starting mqttService");
            startForegroundService(serviceIntent);
        }

        DBHandler dbHandler = new DBHandler(MainActivity.this);
        Context context = getApplicationContext();

        // Let MQTTService know to register for the last known MQTT Topic
        try {
            BufferedReader inputReader = new BufferedReader(new InputStreamReader(
                    openFileInput("esp32mqttTopic")));
            String inputString;
            while ((inputString = inputReader.readLine()) != null) {
                lastMqttTopic = inputString;
                Log.d(TAG, "Read MQTT Topic from file: " + lastMqttTopic);
                break;
            }
        } catch (IOException e) { e.printStackTrace();}

        // Read Sensors from file
        try {
            BufferedReader inputReader = new BufferedReader(new InputStreamReader(
                    openFileInput("esp32SensorNode")));
            String inputString;
            while ((inputString = inputReader.readLine()) != null) {
                String str = inputString;
                String[] arrOfStr = str.split(":", 4);
                // Room:fddc1a:3:Close
                Log.d(TAG, "Read Sensor Node from file: " + str);
                if (arrOfStr[0] != null) {
                    addButton(arrOfStr[0], arrOfStr[1], Integer.parseInt(arrOfStr[2]),
                            arrOfStr[3]);
                    dbHandler.insertSensorDetails(Integer.parseInt(arrOfStr[2]),
                            arrOfStr[0], "Close", "");
                    // Update the MQTT service for its policies on notification
                    Intent serviceIntent = new Intent(context, mqttService.class);
                    serviceIntent.setAction(mqttService.MQTTUPDATE_SENSOR_ACTION);
                    serviceIntent.putExtra("lastMqttTopic", lastMqttTopic);
                    serviceIntent.putExtra("id", Integer.parseInt(arrOfStr[2]));
                    serviceIntent.putExtra("notifyOn", arrOfStr[3]);
                    startService(serviceIntent);
                }
            }
            inputReader.close();
            updateSensorStatus();
        } catch (IOException e) { e.printStackTrace();}

        // Read Cameras from the file
        //deleteFile("esp32Cameras");
        try {
            BufferedReader inputReader = new BufferedReader(new InputStreamReader(
                    openFileInput("esp32Cameras")));
            String inputString;
            while ((inputString = inputReader.readLine()) != null) {
                String str = inputString;
                String[] arrOfStr = str.split(":", 2);
                // CameraIP:CameraID
                Log.d(TAG, "Read Camera Node from file: " + str);
                if (arrOfStr[0] != null) {
                    addCamera(arrOfStr[0]);
                }
            }
            inputReader.close();
        } catch (IOException e) { e.printStackTrace();}

        initControls();
    }

    void updateSensorStatus() {
        DBHandler db = new DBHandler(this);
        Log.d(TAG, "Update: Print out the Sensor SQLite DB");
        ArrayList<HashMap<String, String>> sensorList = db.GetSensors();
        Log.d(TAG, sensorList.toString());

        for (int i = 1; i < SENSOR_COUNT; i++) {
            if (sensorStruct[i].id == i) {
                Log.d(TAG, "OnCreate: Found sensor in sensorStruct:" + i);
                HashMap<String, String> sensorT =
                        db.GetSensorBySensorId(i);
                if (sensorT != null) {
                    Log.d(TAG, "OnCreate: Found sensor in sensorDB: Status: " +
                            sensorT.get("status"));
                    sensorStruct[i].btn.setText("\n\n" + sensorT.get("status"));
                    sensorStruct[i].tv.setText(sensorStruct[i].sensorName + ":" +
                            i + "\n" + "Current Status since: " +
                            sensorT.get("time"));
                }
                String st = sensorT.get("status");
                if (st.trim().equals("Open")) {
                    sensorStruct[i].btn.setBackgroundResource(R.drawable.sensor_open);
                } else if (st.trim().equals("Close")) {
                    sensorStruct[i].btn.setBackgroundResource(R.drawable.sensor);
                } else {
                    sensorStruct[i].btn.setBackgroundResource(R.drawable.sensor);
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
            Log.v(TAG,"Permission: "+permissions[0]+ "was "+ grantResults[0]);
            //resume tasks needing this permission
        }
    }

    public  boolean isStorageReadPermissionGranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                Log.v(TAG,"Read Permission is granted");
                return true;
            } else {
                Log.v(TAG,"Read Permission is revoked");
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
                return false;
            }
        }
        else { //permission is automatically granted on sdk<23 upon installation
            Log.v(TAG,"Read Permission is granted");
            return true;
        }
    }
    public  boolean isStorageWritePermissionGranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                Log.v(TAG,"Write Permission is granted");
                return true;
            } else {
                Log.v(TAG,"Write Permission is revoked");
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
                return false;
            }
        }
        else { //permission is automatically granted on sdk<23 upon installation
            Log.v(TAG,"Write Permission is granted");
            return true;
        }
    }
    /* Start a thread to send http request to web server use HttpURLConnection object. */
    private void startSendHttpRequestThread(final String reqUrl) {
        boolean ret = isStorageReadPermissionGranted();
        if (ret == false) {
            Log.d(TAG, "No permission granted for Read Ext Strage, needed by HTTP");
            return;
        }
        ret = isStorageWritePermissionGranted();
        if (ret == false) {
            Log.d(TAG, "No permission granted for Write Ext Strage, needed by HTTP");
            return;
        }
        Thread sendHttpRequestThread = new Thread(){
            @Override public void run() {
                Log.d(TAG, "Connecting to URI: " +  reqUrl);
                // Maintain http url connection.
                HttpURLConnection httpConn = null;
                // Read text input stream.
                InputStreamReader isReader = null;
                // Read text into buffer.
                BufferedReader bufReader = null;
                // Save server response text.
                StringBuffer readTextBuf = new StringBuffer();
                try {
                    // Create a URL object use page url.
                    URL url = new URL(reqUrl);
                    // Open http connection to web server.
                    httpConn = (HttpURLConnection)url.openConnection();
                    // Set http request method to get.
                    httpConn.setRequestMethod("GET");
                    // Set connection timeout and read timeout value.
                    httpConn.setConnectTimeout(10000);
                    httpConn.setReadTimeout(10000);
                    // Get input stream from web url connection.
                    InputStream inputStream = httpConn.getInputStream();
                    // Create input stream reader based on url connection input stream.
                    isReader = new InputStreamReader(inputStream);
                    // Create buffered reader.
                    bufReader = new BufferedReader(isReader);
                    // Read line of text from server response.
                    String line = bufReader.readLine();
                    // Loop while return line is not null.
                    while(line != null){
                        // Append the text to string buffer.
                        readTextBuf.append(line);
                        // Continue to read text line.
                        line = bufReader.readLine();
                    }
                    // Send message to main thread to update response text in TextView after read all.
                    Message message = new Message();
                    // Set message type.
                    message.what = REQUEST_WIFI;
                    // Create a bundle object.
                    Bundle bundle = new Bundle();
                    // Put response text in the bundle with the special key.
                    bundle.putString(KEY_RESPONSE_TEXT, readTextBuf.toString());
                    // Set bundle data in message.
                    message.setData(bundle);
                    Log.d(TAG, "Recvd HTTP Msg: " + readTextBuf.toString());
                    // Send message to main thread Handler to process.
                    uiUpdater.sendMessage(message);
                } catch(MalformedURLException ex) {
                    Log.e(TAG, ex.getMessage(), ex);
                } catch(IOException ex) {
                    Log.e(TAG, ex.getMessage(), ex);
                } finally {
                    try {
                        if (bufReader != null) {
                            bufReader.close();
                            bufReader = null;
                        }
                        if (isReader != null) {
                            isReader.close();
                            isReader = null;
                        }
                        if (httpConn != null) {
                            httpConn.disconnect();
                            httpConn = null;
                        }
                    } catch (IOException ex) {
                        Log.e(TAG, ex.getMessage(), ex);
                    }
                }
            }
        };
        // Start the child thread to request web page.
        sendHttpRequestThread.start();
    }

    private void initControls() {
        // This handler is used to wait for child thread message to update server
        // response text in TextView.
        if (uiUpdater == null) {
            uiUpdater = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    if (msg.what == REQUEST_WIFI) {
                        Bundle bundle = msg.getData();
                        if (bundle != null) {
                            String responseText = bundle.getString(KEY_RESPONSE_TEXT);
                            //mAdapter.add(responseText, Color.BLUE);
                            //mRecyclerView.smoothScrollToPosition(mAdapter.getItemCount());
                            updateView(responseText);
                        }
                    }
                }
            };
        }
    }
    public void updateView(String responseText) {
        Log.d(TAG, "Updating view");
        if (responseText.contains("WiFi")) {
            TextView wifiVal = findViewById(R.id.wifi_b);
            wifiVal.setTypeface(null, Typeface.BOLD_ITALIC);
            wifiVal.setText(responseText);
         } else if (responseText.contains("Temp")) {
            TextView mqttVal = findViewById(R.id.temp_b);
            mqttVal.setText(responseText);
            mqttVal.setTypeface(null, Typeface.BOLD_ITALIC);
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            if(deviceAddress.isEmpty()) {
                //mAdapter.add("Select an IOT Node first", Color.BLUE);
                //mRecyclerView.smoothScrollToPosition(mAdapter.getItemCount());
                return true;
            }
            //mAdapter.add("Requesting Config..", Color.BLUE);
            //mRecyclerView.smoothScrollToPosition(mAdapter.getItemCount());
            String uri = "http://" + deviceAddress + ":8080/check?config=1";
            startSendHttpRequestThread(uri);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_config) {
            Intent intent = new Intent(this, espTouchActivity.class);
            startActivityForResult(intent, REQUEST_CODE_6);
        } else if (id == R.id.nav_mdns) {
            Intent intent = new Intent(this, mdnsActivity.class);
            startActivityForResult(intent, REQUEST_CODE_1);
        } else if (id == R.id.nav_notify) {
            Intent intent = new Intent(this, notificationsActivity.class);
            intent.putExtra("address", deviceAddress);
            startActivityForResult(intent, REQUEST_CODE_2);
        } else if (id == R.id.nav_mqtt) {
            Intent intent = new Intent(this, setMqttActivity.class);
            startActivityForResult(intent, REQUEST_CODE_3);
        } else if (id == R.id.nav_sensor) {
            Intent intent = new Intent(this, setSensorActivity.class);
            intent.putExtra("address", deviceAddress);
            startActivityForResult(intent, REQUEST_CODE_4);
        } else if (id == R.id.nav_notifications) {
            Intent intent = new Intent(this, notificationsStatusActivity.class);
            intent.putExtra("address", deviceAddress);
            startActivityForResult(intent, REQUEST_CODE_5);
        } else if (id == R.id.nav_logs) {
            Intent intent = new Intent(this, LogsActivity.class);
            intent.putExtra("address", deviceAddress);
            startActivityForResult(intent, REQUEST_CODE_7);
        } else if (id == R.id.nav_mrt) {
            Intent intent = new Intent(this, MRTActivity.class);
            intent.putExtra("address", deviceAddress);
            startActivityForResult(intent, REQUEST_CODE_8);
        } else if (id == R.id.nav_camera) {
            Intent intent = new Intent(this, AddCameraActivity.class);
            intent.putExtra("address", deviceAddress);
            startActivityForResult(intent, REQUEST_CODE_9);
        } else if (id == R.id.nav_scan_camera) {
            Intent intent = new Intent(this, ScanActivity.class);
            intent.putExtra("address", deviceAddress);
            startActivityForResult(intent, REQUEST_CODE_10);
        }
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent dataIntent) {
        super.onActivityResult(requestCode, resultCode, dataIntent);
        // The returned result data is identified by requestCode.
        // The request code is specified in startActivityForResult(intent, REQUEST_CODE_1); method.
        switch (requestCode) {
            // This request code is set by startActivityForResult(intent, REQUEST_CODE_1) method.
            case REQUEST_CODE_1:
                TextView textView = (TextView)findViewById(R.id.node);
                // User could have selected Search for IOT or entered DuckDNS Domain Name
                if(resultCode == RESULT_OK) {
                    String address = dataIntent.getStringExtra("Address");
                    if (address == null || address.isEmpty()) {
                        Log.d(TAG, "No Device address set");
                        String domain = dataIntent.getStringExtra("DomainAddress");
                        if (domain == null || domain.isEmpty()) {
                            Log.d(TAG, "No Device Domain set");
                        } else {
                                Log.d(TAG, "Device Domain set: " + domain);
                            // This means that the user did not do a search but selected domain
                                textView.setText(domain);
                                // DNS Resolve this address now and set it to deviceAddress
                                deviceAddress = domain;
                        }
                    } else {
                        textView.setText(address);
                        deviceAddress = address;
                    }
                    if (!deviceAddress.isEmpty()) {
                        FileOutputStream fos;
                        try {
                            fos = openFileOutput("esp32configNode", Context.MODE_PRIVATE);
                            //default mode is PRIVATE, can be APPEND etc.
                            fos.write(deviceAddress.getBytes());
                            fos.write("\n".getBytes());
                            Log.d(TAG, "Saving Device Node to file" + ":" + deviceAddress);
                            fos.close();
                        } catch (FileNotFoundException e) {e.printStackTrace();}
                        catch (IOException e) {e.printStackTrace();}
                    }

                    // gServiceName is mDNS ServiceName
                    /*String service = dataIntent.getStringExtra("gServiceName"); */
                }
                break;
            case REQUEST_CODE_2:
            case REQUEST_CODE_3:
                Log.d(TAG, "Push Device Settings returned to main");
                if(resultCode == RESULT_OK) {
                    mqtt_token = dataIntent.getStringExtra("mqtt_token");
                    if (mqtt_token.isEmpty()) {
                        Log.d(TAG, "No MQTT publish token set");
                        return;
                    }
                    // 2:Round:Open : Sat Aug 10 12:14:20 2019:192.168.1.35:
                    Log.d(TAG, "Recvd mqtt token in main : " + mqtt_token);
                    Context context = getApplicationContext();
                    Intent serviceIntent = new Intent(context, mqttService.class);
                    serviceIntent.setAction(mqttService.MQTTSUBSCRIBE_ACTION);
                    serviceIntent.putExtra("topic", mqtt_token);
                    startService(serviceIntent);
                        FileOutputStream fos;
                        try {
                            fos = openFileOutput("esp32mqttTopic", Context.MODE_PRIVATE);
                            //default mode is PRIVATE, can be APPEND etc.
                            fos.write(mqtt_token.getBytes());
                            fos.write("\n".getBytes());
                            Log.d(TAG, "Saving MQTT Topic to file" + ":" + mqtt_token);
                            fos.close();
                        } catch (FileNotFoundException e) {e.printStackTrace();}
                        catch (IOException e) {e.printStackTrace();}
                }
                break;
            case REQUEST_CODE_4:
                Log.d(TAG, "Sensor Settings returned to main");
                if(resultCode == RESULT_OK) {
                    String sensorName = dataIntent.getStringExtra("sensorName");
                    String sensorTag = dataIntent.getStringExtra("sensorTag");
                    String notifyOn = dataIntent.getStringExtra("notifyOn");
                    String startTime = dataIntent.getStringExtra("startTime");
                    String endTime = dataIntent.getStringExtra("endTime");
                    String bleID = dataIntent.getStringExtra("bleID");
                    int id = Integer.parseInt(bleID);

                    if (sensorName.isEmpty() || sensorTag.isEmpty()) {
                        Log.d(TAG, "Sensor Name or Tag is empty");
                        return;
                    }
                    // Update the MQTT service for its policies on notification
                    Context context = getApplicationContext();
                    Intent serviceIntent = new Intent(context, mqttService.class);
                    serviceIntent.setAction(mqttService.MQTTUPDATE_SENSOR_ACTION);
                    serviceIntent.putExtra("id", id);
                    serviceIntent.putExtra("notifyOn", notifyOn);
                    startService(serviceIntent);

                    Log.d(TAG, "Recvd Sensor from SensorActivity: " + sensorName + " : " +
                            sensorTag + ":" + id);
                    if (addButton(sensorName, sensorTag, id, notifyOn) == false) {
                        Log.d(TAG, "Sensor already added by addButton");
                        return;
                    }
                    Log.d(TAG, "Save Sensor from SensorActivity in file..");
                    FileOutputStream fos;
                    try {
                        fos = openFileOutput("esp32SensorNode", Context.MODE_APPEND);
                        String str = sensorName + ":" + sensorTag + ":" + id + ":" + notifyOn;
                        fos.write(str.getBytes());
                        fos.write("\n".getBytes());
                        Log.d(TAG, "Saving Sensor Node to file" + ":" + str);
                        fos.close();
                    } catch (FileNotFoundException e) {e.printStackTrace();}
                    catch (IOException e) {e.printStackTrace();}
                }
                break;
            case REQUEST_CODE_10:
                int id = 0;
                Log.d(TAG, "Cameras returned to main: ");
                if(resultCode == RESULT_OK) {
                    onvifDeviceList = (ArrayList<DiscoveredCamera>)dataIntent.
                            getSerializableExtra("onvifDevices");
                    if (onvifDeviceList.size() == 0) {
                        Log.d(TAG, "Main: No cameras detected"); return;
                    }

                    for (DiscoveredCamera discoveredCamera : onvifDeviceList) {
                        Log.d(TAG, discoveredCamera.toString() + ":" +
                                discoveredCamera.getIP());
                        id = addCamera(discoveredCamera.getIP());
                        if (id == 0) continue;
                    }

                    Log.d(TAG, "Save Cameras in file..");
                    ///data/user/0/com.aseemsethi.esp32_iot/files/esp32Cameras
                    FileOutputStream fos;
                    try {
                        fos = openFileOutput("esp32Cameras", Context.MODE_APPEND);
                        for (DiscoveredCamera discoveredCamera : onvifDeviceList) {
                            String str = discoveredCamera.getIP() + ":" + id;
                            fos.write(str.getBytes());
                            fos.write("\n".getBytes());
                            Log.d(TAG, "Saving Camera to file" + ":" + str);
                        }
                        fos.close();
                    } catch (FileNotFoundException e) {e.printStackTrace();}
                    catch (IOException e) {e.printStackTrace();}
                }
                break;
        }
    }

    int addCamera(final String ipaddress) {
        for (int i = 0; i < 9; i++) {
            if (cameraStruct[i].ipaddress == null) continue;
            if ((cameraStruct[i].ipaddress).contains(ipaddress)) {
                Log.d(TAG, "Camera already added    !!!!");
                return 0;
            }
        }
        Log.d(TAG, "addCamera: add camera buttons");
        TableLayout tl = (TableLayout) findViewById(R.id.table1);
        TableRow tr = new TableRow(this);
        tr.setGravity(Gravity.CENTER);
        // Set new table row layout parameters.
        TableRow.LayoutParams layoutParams = new TableRow.LayoutParams(
                TableRow.LayoutParams.WRAP_CONTENT,
                TableRow.LayoutParams.WRAP_CONTENT, 1.7f);

        final TextView tv = new TextView(this);
        tv.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT,
                TableRow.LayoutParams.WRAP_CONTENT,
                0.7f));
        tv.setWidth(80);
        tv.setText(ipaddress);
        tv.setTypeface(tv.getTypeface(), Typeface.BOLD_ITALIC);
        tv.setTextSize(10);
        tv.setPadding(0,0, 30, 0);
        tr.addView(tv);

        final SurfaceView iv = new SurfaceView(this);
        iv.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT,
                TableRow.LayoutParams.WRAP_CONTENT,
                1.0f));
        //iv.setBackgroundResource(R.drawable.square);
        iv.setPadding(0, 20, 0, 20);
        iv.setId(cameraID);
        iv.getHolder().setFixedSize(400, 400);

        layoutParams.setMargins(5,5,5,5);
        tr.setLayoutParams(layoutParams);
        tr.addView(iv);
        tl.addView(tr);

        TableRow tr1 = new TableRow(this);
        TextView tv1 = new TextView(this);
        tv.setHeight(80);
        tr1.addView(tv1);
        tl.addView(tr1);

        cameraStruct[cameraID].ipaddress = ipaddress;
        cameraStruct[cameraID].tv = tv;
        cameraStruct[cameraID].iv = iv;
        cameraStruct[cameraID].id = cameraID;
        cameraStruct[cameraID].currentDevice = new OnvifDevice(
                ipaddress+":5000",
                "aseemsethi", "pinewood");
        Log.d(TAG, "addCamera: added: " + cameraID + " : " + ipaddress);

        iv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.startAnimation(buttonClick);
                if (cameraClick) {
                    Log.d(TAG, "Currently working with a camera"); return;
                }
                cameraClick = true;
                int id = iv.getId();
                currentID = id;
                String ipadd = cameraStruct[v.getId()].ipaddress;
                Log.d(TAG, "Camera clicked: " + iv.getId() + " : " + ipadd);
                Log.d(TAG, "Connecting to: " + ipadd);
                currentDevice = cameraStruct[id].currentDevice;
                OnvifListener onvifListener = (OnvifListener) thisActivity;
                cameraStruct[id].currentDevice.setListener(onvifListener);
                cameraStruct[id].currentDevice.getServices();
                /*if (cameraStruct[id].currentDevice.isConnected()) {
                    String uri = cameraStruct[id].currentDevice.getRtspURI();
                    if (uri == null) {
                        Log.d(TAG, "Current Device connected, No RTSP URI");
                        return;
                    }
                    Log.d(TAG, "Current Device connected, RTSP URI: " + uri);
                    cameraStruct[id].tv.setText(uri);
                } else {
                    Log.d(TAG, "currentDevice is not connected");
                    cameraStruct[id].tv.setText(ipadd + " : Not connected");
                } */
            }
        });
        cameraID += 1;
        return cameraID-1;
    }

    boolean addButton(String sensorName, String sensorTag, final int id, String notifyOn) {
        for (int i = 0; i < 9; i++) {
            if (sensorStruct[i].sensorName == sensorName ||
                    sensorStruct[i].id == id ||
                    sensorStruct[i].sensorTag == sensorTag) {
                Log.d(TAG, "Sensor already added    !!!!");
                return false;
            }
        }
        Log.d(TAG, "addbutton: add sensor buttons");
        TableLayout tl = (TableLayout) findViewById(R.id.table1);
        TableRow tr = new TableRow(this);
        tr.setGravity(Gravity.CENTER);
        // Set new table row layout parameters.
        TableRow.LayoutParams layoutParams = new TableRow.LayoutParams(
                TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(-5,-5,-5,-5);
        //tr.setLayoutParams(layoutParams);

        final TextView tv = new TextView(this);
        tv.setLayoutParams(new TableRow.LayoutParams(
                TableRow.LayoutParams.WRAP_CONTENT,
                TableRow.LayoutParams.WRAP_CONTENT,0.7f));
        tv.setText(sensorName + ":" + id +
                   "\n" + "Current Status since: ");
        tv.setTypeface(tv.getTypeface(), Typeface.BOLD_ITALIC);
        tv.setTextSize(10);
        tr.addView(tv);

        final Button btn = new Button(this);
        btn.setId(id);
        btn.setBackgroundResource(R.drawable.sensor);
        tr.addView(btn);

        tl.addView(tr, new TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT,
                TableLayout.LayoutParams.WRAP_CONTENT));

        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.startAnimation(buttonClick);
                lastTouchTime = currentTouchTime;
                currentTouchTime = System.currentTimeMillis();

                if (currentTouchTime - lastTouchTime < 250) {
                    Log.d(TAG, "Double Click");
                    lastTouchTime = 0;
                    currentTouchTime = 0;
                } else {
                    Log.d(TAG, "Single Click");
                    return;
                }
                Log.d(TAG, "delete btn clicked: " + btn.getId());
                int id = btn.getId();

                FileOutputStream fos = null;
                try {
                    fos = openFileOutput("tempFileT", Context.MODE_PRIVATE);
                } catch (FileNotFoundException e) {e.printStackTrace();}
                catch (IOException e) {e.printStackTrace();}

                try {
                    BufferedReader inputReader = new BufferedReader(new InputStreamReader(
                            openFileInput("esp32SensorNode")));
                    String inputString;
                    while ((inputString = inputReader.readLine()) != null) {
                        String str = inputString;
                        String[] arrOfStr = str.split(":", 4);
                        Log.d(TAG, "Reading Sensor Node from file: " + str);
                        if (arrOfStr[0] != null) {
                            int idFile = Integer.parseInt(arrOfStr[2]);
                            if (id == idFile) {
                                Log.d(TAG, "Found ID to delete:" + idFile);
                            } else {
                                Log.d(TAG, "Writing: " + inputString);
                                fos.write(inputString.getBytes());
                                fos.write("\n".getBytes());                            }
                        }
                    }
                    fos.close();
                    inputReader.close();
                    File outFile = new File(getFilesDir() + "/tempFileT");
                    File oldFile = new File(getFilesDir() + "/esp32SensorNode");
                    //Directory: /data/user/0/com.aseemsethi.esp32_iot/files
                    Log.d(TAG, "Directory: " + getFilesDir());
                    Log.d(TAG, "Rename Files: " + outFile.getAbsolutePath() +
                            ":" + oldFile.getAbsolutePath());
                    if (outFile.exists()) {
                        boolean ret = outFile.renameTo(oldFile);
                        if (ret)
                            Log.d(TAG, "File successfully renamed");
                        else
                            Log.d(TAG, "File not renamed !!");
                    } else
                        Log.d(TAG, "new sensor file not created !!");
                } catch (IOException e) { e.printStackTrace(); Log.d(TAG, "File error");}

                // Now delete this Sensor from sensorStruct too, so it can be added later to the
                // file by again going to SensorActivity. Else, it will not be written to file
                // if sensorStruct has this data.
                sensorStruct[id] = new sensorT();
                // Send a msg to MqttService to delete this Sensor from its DB too
                // TBD
                Context context = getApplicationContext();
                Intent serviceIntent = new Intent(context, mqttService.class);
                serviceIntent.setAction(mqttService.MQTTDELETE_SENSOR_ACTION);
                serviceIntent.putExtra("id", id);
                startService(serviceIntent);
                //(sensorStruct[id].tr).setVisibility(View.GONE);
                //v.setVisibility(View.GONE);
            }
        });
        // Save this into a structure, that needs to also go into a file.
        sensorStruct[id].id = id;
        sensorStruct[id].btn = btn;
        sensorStruct[id].tr = tr;
        sensorStruct[id].tv = tv;
        sensorStruct[id].sensorName = sensorName;
        sensorStruct[id].sensorTag = sensorTag;
        sensorStruct[id].notifyOn = notifyOn;
        return true;
    }

    @Override
    public void requestPerformed(OnvifResponse onvifResponse) {
        Log.d(TAG, onvifResponse.getParsingUIMessage());

        if (!onvifResponse.getSuccess()) {
            Log.e(TAG, "request failed: " + onvifResponse.getRequest().getType() +
                    "\n Response: " + onvifResponse.getError());
            cameraClick = false;
            return;
        } else {
            Log.d(TAG,"Request " + onvifResponse.getRequest().getType() +
                    " performed.");
            //Log.d(TAG,"Succeeded: " + onvifResponse.getSuccess() +
            //        "message:" + onvifResponse.getParsingUIMessage());
        }
        // if GetServices have been completed, we request the device information
        if (onvifResponse.getRequest().getType() == OnvifRequest.Type.GetServices) {
            currentDevice.getDeviceInformation();
            Log.d(TAG, "Get Services: ");
        }
        // if GetDeviceInformation have been completed, we request the profiles
        else if (onvifResponse.getRequest().getType() == OnvifRequest.Type.GetDeviceInformation) {
            //TextView textView = findViewById(R.id.explanationTextView);
            //textView.setText(onvifResponse.getParsingUIMessage());
            Log.d(TAG, "Get Device Info: " + onvifResponse.getParsingUIMessage());
            currentDevice.getProfiles();
        }
        // if GetProfiles have been completed, we request the Stream URI
        else if (onvifResponse.getRequest().getType() == OnvifRequest.Type.GetProfiles) {
            int profilesCount = currentDevice.getMediaProfiles().size();
            Log.d(TAG, "Get Profiles: " + onvifResponse.getParsingUIMessage());
            currentDevice.getStreamURI();
        }
        // if GetStreamURI have been completed, we're ready to play the video
        else if (onvifResponse.getRequest().getType() == OnvifRequest.Type.GetStreamURI) {
            //Button button = findViewById(R.id.button);
            //button.setText(getString(R.string.Play));
            Log.d(TAG, "Stream URI retrieved: " + currentDevice.getRtspURI());
            cameraStruct[currentID].tv.setText(cameraStruct[currentID].ipaddress + "\n" +
                    currentDevice.getRtspURI());
            cameraStruct[currentID].rtspStream = currentDevice.getRtspURI();
            cameraClick = false;
            if (cameraStruct[currentID].currentDevice.isConnected()) {
                String uri = cameraStruct[currentID].currentDevice.getRtspURI();
                if (uri == null) {
                    Log.d(TAG, "Current Device connected, No RTSP URI");
                    return;
                }
                Log.d(TAG, "Current Device connected, RTSP URI: " + uri);
                getPic(currentID);
            } else {
                Log.d(TAG, "currentDevice is not connected");
            }
            //currentDevice.getSnapshotUri();
        }
    }

    void getPic(int id) {
        // Keep screen on while streaming.
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        ImageView iv;
        Log.d(TAG, "Playing VLC now");
        // Directory where images to be saved
        whereToSave = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES) + "/faces/";
        vlcVideoLibrary = new VlcVideoLibrary(this, this,
                cameraStruct[id].iv);
        vlcVideoLibrary.play(cameraStruct[id].rtspStream);
    }

    @Override
    public void onComplete() {
        Toast.makeText(this, "Video loading...", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onError() {
        Toast.makeText(this, "Error loading video...", Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "Main activity - OnPause");
        super.onPause();
        unregisterServices();
    }
    @Override
    protected void onDestroy() {
        Log.d(TAG, "Main activity - destroy");
        super.onDestroy();
        unregisterServices();
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "Main activity - stop");
        super.onStop();
        unregisterServices();
    }

    void unregisterServices() {
        try {
            Log.d(TAG, "onDestroy unregister myReceiverMqtt");
            unregisterReceiver(myReceiverMqtt);
        } catch (IllegalArgumentException e) {
            if (e.getMessage().contains("Receiver not registered")) {
                // Ignore this exception. This is exactly what is desired
                Log.w(TAG,"Tried to unregister myReceiverMqtt when it's not registered");
            } else {
                // unexpected, re-throw
                throw e;
            }
        }
        try {
            Log.d(TAG, "unregister myReceiverMqttMsg");
            unregisterReceiver(myReceiverMqttMsg);
        } catch (IllegalArgumentException e) {
            if (e.getMessage().contains("Receiver not registered")) {
                // Ignore this exception. This is exactly what is desired
                Log.w(TAG,"Tried to unregister myReceiverMqttMsg when it's not registered");
            } else {
                // unexpected, re-throw
                throw e;
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "OnResume - Register receivers");
        registerServices();
    }


    void registerServices() {
        Log.d(TAG, "Register receivers");

        IntentFilter filter1 = new IntentFilter("RestartMqtt");
        //The BroadcastReceiver that listens for bluetooth broadcasts
        myReceiverMqtt = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (isMyServiceRunning()) {
                    Log.d(TAG, "mqttService BroadcastReceiver: is already running"); return;
                }
                Log.d(TAG, "mqttService BroadcastReceiver: attempting to start mqttService");
                Intent serviceIntent = new Intent(context, mqttService.class);
                serviceIntent.setAction(mqttService.MQTTMSG_ACTION);
                context.startForegroundService(serviceIntent);
            }
        };
        registerReceiver(myReceiverMqtt, filter1);

        IntentFilter filter2 = new IntentFilter(MQTTMSG_MSG);
        //The BroadcastReceiver that listens for bluetooth broadcasts
        myReceiverMqttMsg = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String msg = intent.getStringExtra("MQTTRCV");
                String[] arrOfStr = msg.split(":", 4);
                Log.d(TAG, "Button msg: " + msg);
                int id = parseWithDefault(arrOfStr[0], 0);
                if (id == 0) {
                    Log.d(TAG, "Cannot associate BLE with Button");
                    return;
                }
                // MQTT Recvd: 3:Round:Close : Fri Aug  9 22:09:32 2019:192.168.1.35:
                Log.d(TAG, "MQTT Msg recv in main: " + msg + ",  id:" + id);
                for (int i = 0; i < 9; i++) {
                    if (sensorStruct[i].id == id) {
                        Log.d(TAG, "Found sensor in sensorStruct");
                        DBHandler db = new DBHandler(context);
                        HashMap<String, String> sensorList =
                                db.GetSensorBySensorId(id);
                        if (sensorList != null) {
                            Log.d(TAG, "Found sensor " + i +
                                    " in sensorDB: Old Status: " +
                                    sensorList.get("status") + "LastTimeChanged: " +
                                    sensorList.get("time"));
                            int count = db.UpdateSensorDetails(arrOfStr[2], id);
                            Log.d(TAG, "Print out the Sensor SQLite DB");
                            ArrayList<HashMap<String, String>> sensorL = db.GetSensors();
                            Log.d(TAG, sensorL.toString());
                        }
                        sensorStruct[i].btn.setText("\n\n\n" + arrOfStr[2]);
                        sensorStruct[i].tv.setText(sensorStruct[i].sensorName + ":" +
                                id + "\n" + "Current Status since: " +
                                sensorList.get("time"));
                        if ((arrOfStr[2].trim()).equals("Open")) {
                            sensorStruct[i].status_code = OPEN_CODE;
                            sensorStruct[i].btn.setBackgroundResource(R.drawable.sensor_open);
                        } else {
                            sensorStruct[i].status_code = CLOSE_CODE;
                            sensorStruct[i].btn.setBackgroundResource(R.drawable.sensor);
                        }
                        break;
                    }
                }
            }
        };
        registerReceiver(myReceiverMqttMsg, filter2);
    }

    int parseWithDefault(String s, int def) {
        try {
            return Integer.parseInt(s);
        }
        catch (NumberFormatException e) {
            // It's OK to ignore "e" here because returning a default value is the
            // documented behaviour on invalid input.
            return def;
        }
    }

    private boolean isMyServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (mqttService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}
