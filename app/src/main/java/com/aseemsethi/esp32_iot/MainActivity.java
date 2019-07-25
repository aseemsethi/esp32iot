package com.aseemsethi.esp32_iot;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.provider.Settings;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
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
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {
    final String TAG = "ESP32IOT MainActivity";
    private final static int REQUEST_CODE_1 = 1; // for mDNS
    private final static int REQUEST_CODE_2 = 2; // for push notifications
    private final static int REQUEST_CODE_3 = 3; // for mqtt topic
    private final static int REQUEST_CODE_4 = 4; // for sensors

    private static final int REQUEST_WIFI = 1;
    private static final String KEY_RESPONSE_TEXT = "KEY_RESPONSE_TEXT";
    private Handler uiUpdater = null;
    private HistoryAdapter mAdapter;
    RecyclerView mRecyclerView;
    String deviceAddress = "";
    private AlphaAnimation buttonClick = new AlphaAnimation(1F, 0.1F);
    MqttHelper mqttHelper;
    String mqtt_token = "";
    MyReceiver myReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        //System.setProperty("http.keepAlive", "false");
        //System.setProperty("http.maxConnections", "5");

        IntentFilter filter1 = new IntentFilter("RestartMqtt");
        registerReceiver(myReceiverMqtt, filter1);
        //turnOffDozeMode(getApplicationContext());

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        navigationView.setNavigationItemSelectedListener(this);
        final Button wifiB = findViewById(R.id.wifi_b);
        wifiB.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(deviceAddress.isEmpty()) {
                    mAdapter.add("Select an IOT Node first", Color.BLUE);
                    mRecyclerView.smoothScrollToPosition(mAdapter.getItemCount());
                    return;
                }
                v.startAnimation(buttonClick);
                mAdapter.add("Requesting WiFi Status..", Color.BLUE);
                mRecyclerView.smoothScrollToPosition(mAdapter.getItemCount());
                String uri = "http://" + deviceAddress + ":8080/check?wifi=1";
                startSendHttpRequestThread(uri);
            }
        });
        final Button httpB = findViewById(R.id.http_b);
        httpB.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(deviceAddress.isEmpty()) {
                    mAdapter.add("Select an IOT Node first", Color.BLUE);
                    mRecyclerView.smoothScrollToPosition(mAdapter.getItemCount());
                    return;
                }
                v.startAnimation(buttonClick);
                mAdapter.add("Requesting HTTP Status..", Color.BLUE);
                mRecyclerView.smoothScrollToPosition(mAdapter.getItemCount());
                String uri = "http://" + deviceAddress + ":8080/check?http=1";
                startSendHttpRequestThread(uri);
            }
        });
        final Button mqttb = findViewById(R.id.mqtt_b);
        mqttb.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(deviceAddress.isEmpty()) {
                    mAdapter.add("Select an IOT Node first", Color.BLUE);
                    mRecyclerView.smoothScrollToPosition(mAdapter.getItemCount());
                    return;
                }
                v.startAnimation(buttonClick);
                mAdapter.add("Requesting MQTT Status..", Color.BLUE);
                mRecyclerView.smoothScrollToPosition(mAdapter.getItemCount());
                String uri = "http://" + deviceAddress + ":8080/check?mqtt=1";
                startSendHttpRequestThread(uri);
            }
        });
        final Button tempb = findViewById(R.id.temp_b);
        tempb.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(deviceAddress.isEmpty()) {
                    mAdapter.add("Select an IOT Node first", Color.BLUE);
                    mRecyclerView.smoothScrollToPosition(mAdapter.getItemCount());
                    return;
                }
                v.startAnimation(buttonClick);
                mAdapter.add("Requesting Temperature Status..", Color.BLUE);
                mRecyclerView.smoothScrollToPosition(mAdapter.getItemCount());
                String uri = "http://" + deviceAddress + ":8080/check?temp=1";
                startSendHttpRequestThread(uri);
            }
        });
        final Button cfgb = findViewById(R.id.config_b);
        cfgb.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(deviceAddress.isEmpty()) {
                    mAdapter.add("Select an IOT Node first", Color.BLUE);
                    mRecyclerView.smoothScrollToPosition(mAdapter.getItemCount());
                    return;
                }
                v.startAnimation(buttonClick);
                mAdapter.add("Requesting Config..", Color.BLUE);
                mRecyclerView.smoothScrollToPosition(mAdapter.getItemCount());
                String uri = "http://" + deviceAddress + ":8080/check?config=1";
                startSendHttpRequestThread(uri);
            }
        });
        final Button clear = findViewById(R.id.clear_b);
        clear.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.startAnimation(buttonClick);
                mAdapter.clear();
            }
        });

        mRecyclerView = (RecyclerView) findViewById(R.id.main_recycler_view);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mAdapter = new HistoryAdapter(new ArrayList<String>());
        mRecyclerView.setAdapter(mAdapter);

        mAdapter.add("To Start Monitoring:", Color.BLUE);
        mAdapter.add("Enable Menu -> Device Discovery and Menu -> Set Notifications", Color.BLUE);
        mRecyclerView.smoothScrollToPosition(mAdapter.getItemCount());

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

        initControls();
        //Register BroadcastReceiver
        //to receive event from our service
        myReceiver = new MyReceiver();

        Context context = getApplicationContext();
        Intent serviceIntent = new Intent(context, mqttService.class);
        serviceIntent.setAction(mqttService.MQTTMSG_ACTION);
        startForegroundService(serviceIntent);
    }

    //The BroadcastReceiver that listens for bluetooth broadcasts
    private final BroadcastReceiver myReceiverMqtt = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "MqttService BroadcastReceiver: attempting to start mqttService");
            Intent serviceIntent = new Intent(context, mqttService.class);
            serviceIntent.setAction(mqttService.MQTTMSG_ACTION);
            context.startForegroundService(serviceIntent);
            }
    };

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
                            mAdapter.add(responseText, Color.BLUE);
                            mRecyclerView.smoothScrollToPosition(mAdapter.getItemCount());
                            updateView(responseText);
                        }
                    }
                }
            };
        }
    }
    public void updateView(String responseText) {
        Log.d(TAG, "Updating view");
        if (responseText.contains("Config")) {
        } else if (responseText.contains("WiFi")) {
            TextView wifiVal = findViewById(R.id.wifi_val);
            wifiVal.setTypeface(null, Typeface.BOLD_ITALIC);
            wifiVal.setText(responseText);
        } else if (responseText.contains("HTTP")) {
            TextView httpVal = findViewById(R.id.http_val);
            httpVal.setText(responseText);
            httpVal.setTypeface(null, Typeface.BOLD_ITALIC);
        } else if (responseText.contains("MQTT")) {
            TextView mqttVal = findViewById(R.id.mqtt_val);
            mqttVal.setText(responseText);
            mqttVal.setTypeface(null, Typeface.BOLD_ITALIC);
        } else if (responseText.contains("Temp")) {
            TextView mqttVal = findViewById(R.id.temp_val);
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
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_mdns) {
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
        } else if (id == R.id.nav_send) {

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
                TextView textView1 = (TextView)findViewById(R.id.mdns_val);
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
                    String service = dataIntent.getStringExtra("gServiceName");
                    if (service == null || service.isEmpty()) {
                        Log.d(TAG, "No Service Name set");
                        return;
                    }
                    textView1.setText(service);
                    textView1.setTypeface(null, Typeface.BOLD_ITALIC);
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
                    if (sensorName.isEmpty() || sensorTag.isEmpty()) {
                        Log.d(TAG, "Sensor Name or Tag is empty");
                        return;
                    }
                    Log.d(TAG, "Recvd Sensor info: " + sensorName + " : " + sensorTag);
                    mAdapter.add("Sensor Added: " + sensorName + " : " + sensorTag, Color.BLUE);
                    mRecyclerView.smoothScrollToPosition(mAdapter.getItemCount());
                }
                break;
        }
    }

    private class MyReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context arg0, Intent arg1) {
            String msg = arg1.getStringExtra("MQTTRCV");
            Log.d(TAG, "Recvd from Service: " + msg);
            Toast.makeText(getApplicationContext(),
           "Broadcast Rcv!\n" + msg, Toast.LENGTH_LONG).show();
            mAdapter.add(msg, Color.BLUE);
            mRecyclerView.smoothScrollToPosition(mAdapter.getItemCount());
            updateView(msg);
        }

    }
}
