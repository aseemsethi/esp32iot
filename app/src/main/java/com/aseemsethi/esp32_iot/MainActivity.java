package com.aseemsethi.esp32_iot;

import android.Manifest;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.support.v4.view.GravityCompat;
import android.support.v7.app.ActionBarDrawerToggle;
import android.view.MenuItem;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;

import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.animation.AlphaAnimation;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

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


// How to import esptouch module.
// File --> New --> Import Module - point this to the esptouch directory locally downloaded
// from https://github.com/EspressifApp/EsptouchForAndroid
// In gradle file add a line - implementation project(':esptouch')
// In settings.gradle - modify to : include ':app', ':esptouch'
public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {
    final String TAG = "ESP32IOT MainActivity";
    private final static int REQUEST_CODE_1 = 1; // for mDNS
    private final static int REQUEST_CODE_2 = 2; // for push notifications
    private final static int REQUEST_CODE_3 = 3; // for mqtt topic
    private final static int REQUEST_CODE_4 = 4; // for sensors
    private final static int REQUEST_CODE_5 = 5; // for notificationStatus
    private final static int REQUEST_CODE_6 = 6; // for esptouch
    private final static int REQUEST_CODE_7 = 7; // for logs
    private final static int REQUEST_CODE_8 = 8; // for mrt
    //final static String MQTTMSG_ACTION = "com.aseemsethi.esp32_iot.mqttService.MQTTMSG_ACTION";

    private static final int REQUEST_WIFI = 1;
    private static final String KEY_RESPONSE_TEXT = "KEY_RESPONSE_TEXT";
    private Handler uiUpdater = null;
    private HistoryAdapter mAdapter;
    RecyclerView mRecyclerView;
    String deviceAddress = "";
    private AlphaAnimation buttonClick = new AlphaAnimation(1F, 0.1F);
    MqttHelper mqttHelper;
    String mqtt_token = "";
    //MyReceiver myReceiver;
    BroadcastReceiver myReceiverMqtt = null;
    BroadcastReceiver myReceiverMqttMsg = null;
    private class sensorT {
        public String sensorName;
        public String sensorTag;
        public String notifyOn;
        public int    id;
        Button        btn;
    };
    sensorT sensorStruct[];
    final static String MQTTMSG_MSG = "com.aseemsethi.esp32_iot.mqttService.MQTTMSG_MSG";
    private long lastTouchTime = 0;
    private long currentTouchTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        sensorStruct = new sensorT[10];
        for (int i = 0; i < 9; i++) {
            sensorStruct[i] = new sensorT();
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

        // Read Sensors from file
        try {
            BufferedReader inputReader = new BufferedReader(new InputStreamReader(
                    openFileInput("esp32SensorNode")));
            String inputString;
            while ((inputString = inputReader.readLine()) != null) {
                String str = inputString;
                String[] arrOfStr = str.split(":", 4);
                Log.d(TAG, "Read Sensor Node from file: " + str);
                if (arrOfStr[0] != null) {
                    addButton(arrOfStr[0], arrOfStr[1], Integer.parseInt(arrOfStr[2]), arrOfStr[3]);
                    // Update the MQTT service for its policies on notification
                    Context context = getApplicationContext();
                    Intent serviceIntent = new Intent(context, mqttService.class);
                    serviceIntent.setAction(mqttService.MQTTUPDATE_SENSOR_ACTION);
                    serviceIntent.putExtra("id", Integer.parseInt(arrOfStr[2]));
                    serviceIntent.putExtra("notifyOn", arrOfStr[3]);
                    startService(serviceIntent);
                }
            }
            inputReader.close();
            updateSensorStatus();
        } catch (IOException e) { e.printStackTrace();}

        initControls();
    }

    void updateSensorStatus() {
        String inputString;
        String lastLine = "";

        try {
            BufferedReader inputReader = new BufferedReader(new InputStreamReader(
                    openFileInput("esp32Notifications")));
            while ((inputString = inputReader.readLine()) != null) {
                lastLine = inputString;
            }
        } catch (IOException e) { e.printStackTrace();}

        String[] arrOfStr = lastLine.split(":", 4);
        Log.d(TAG, "File msgs, lastLine: " + lastLine);
        int id = parseWithDefault(arrOfStr[0], 0);
        if (id == 0) {
            Log.d(TAG, "Cannot associate BLE with Button");
            return;
        }
        // MQTT Recvd: 3:Round:Closed : Fri Aug  9 22:09:32 2019:192.168.1.35:
        Log.d(TAG, "lastLine id:" + id);
        for (int i = 0; i < 9; i++) {
            if (sensorStruct[i].id == id) {
                Log.d(TAG, "Found the button");
                sensorStruct[i].btn.setText(sensorStruct[i].sensorName + ":" + id
                        + "\n\n" + arrOfStr[2]);
                if ((arrOfStr[2].trim()).equals("Open"))
                    sensorStruct[i].btn.setBackgroundResource(R.drawable.sensor_open);
                else
                    sensorStruct[i].btn.setBackgroundResource(R.drawable.sensor);
                break;
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
                        // Store Sensor in File
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
                    //mqttHelper.subscribeToTopic(mqtt_token);
                    Context context = getApplicationContext();
                    Intent serviceIntent = new Intent(context, mqttService.class);
                    serviceIntent.setAction(mqttService.MQTTSUBSCRIBE_ACTION);
                    serviceIntent.putExtra("topic", mqtt_token);
                    startService(serviceIntent);
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
        }
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
        final Button btn = new Button(this);
        btn.setText(sensorName + ":" + id);
        btn.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 10);
        btn.setId(id);
        btn.setBackgroundResource(R.drawable.sensor);
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
                v.setVisibility(View.GONE);
            }
        });
        TableLayout tl = (TableLayout) findViewById(R.id.table1);
        TableRow tr = new TableRow(this);
        // Set new table row layout parameters.
        TableRow.LayoutParams layoutParams = new TableRow.LayoutParams(
                TableRow.LayoutParams.WRAP_CONTENT);
        tr.setLayoutParams(layoutParams);
        tr.addView(btn, 0);
        tl.addView(tr);
        // Save this into a structure, that needs to also go into a file.
        sensorStruct[id].id = id;
        sensorStruct[id].btn = btn;
        sensorStruct[id].sensorName = sensorName;
        sensorStruct[id].sensorTag = sensorTag;
        sensorStruct[id].notifyOn = notifyOn;
        return true;
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
                // MQTT Recvd: 3:Round:Closed : Fri Aug  9 22:09:32 2019:192.168.1.35:
                Log.d(TAG, "MQTT Msg recv in main: " + msg + ",  id:" + id);
                for (int i = 0; i < 9; i++) {
                    if (sensorStruct[i].id == id) {
                        Log.d(TAG, "Found the button");
                        sensorStruct[i].btn.setText(sensorStruct[i].sensorName + ":"
                                + id
                                + "\n\n" + arrOfStr[2]);
                        if ((arrOfStr[2].trim()).equals("Open"))
                            sensorStruct[i].btn.setBackgroundResource(R.drawable.sensor_open);
                        else
                            sensorStruct[i].btn.setBackgroundResource(R.drawable.sensor);
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
            // It's OK to ignore "e" here because returning a default value is the documented behaviour on invalid input.
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
