package com.aseemsethi.esp32_iot;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.format.Formatter;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

// mDNS code referred from https://www.dodgycoder.net/2015/02/setting-up-bonjourzeroconfmdnsnsd.html

public class notificationsActivity extends AppCompatActivity {
    TextView ipaddressSelf, ipaddressDev;
    private ProgressBar progress;
    ArrayList<String> subnetList;
    private Handler mHandler = new Handler();
    private HistoryAdapter mAdapter;
    RecyclerView mRecyclerView;
    public String mRPiAddress;
    final String TAG = "ESP32IOT notify";
    private AlphaAnimation buttonClick = new AlphaAnimation(1F, 0.1F);
    boolean mqtt = false;
    boolean ifttt = false;
    boolean ddns = false;
    String mqtt_token = "";
    String ifttt_token = "";
    String ddns_uri = "";
    private static final int REQUEST_WIFI = 1;
    private static final String KEY_RESPONSE_TEXT = "KEY_RESPONSE_TEXT";
    private Handler uiUpdater = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Use the following 2 lines - else use async threads
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        ipaddressSelf = (TextView) findViewById(R.id.notify_self_ipaddress);
        ipaddressSelf.setText(null);
        getIP(getApplicationContext());

        ipaddressDev = (TextView) findViewById(R.id.notify_dev_ipaddress);
        Intent intent = getIntent();
        String message = intent.getStringExtra("address");
        ipaddressDev.setText(message);

        Button search = (Button) findViewById(R.id.pushSettingsB);
        search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                view.startAnimation(buttonClick);
                EditText mqtt_tokenF = findViewById(R.id.mqtt_val);
                mqtt_token = (mqtt_tokenF).getText().toString();
                EditText ddnsT = findViewById(R.id.ddns_val);
                ddns_uri = ddnsT.getText().toString();
                boolean cont = getIP(getApplicationContext());
                if (cont == false) {
                    //mAdapter.add("Please ensure Phone has WiFi IP Address", Color.BLUE);
                    //mRecyclerView.smoothScrollToPosition(mAdapter.getItemCount());
                    Log.d(TAG, "WiFi IP Address of Device missing");
                    //return;
                }
                if (ipaddressDev.getText().toString().isEmpty()) {
                    mAdapter.add("Device IP Address is Null. Select IOT Device : Menu->Discovery", Color.BLUE);
                    return;
                }
                if (mqtt == true) {
                    String uri = "http://" + ipaddressDev.getText() + ":8080/enable?mqtt=1"
                            + "&mqtt_topic=" + mqtt_token;
                    Log.d(TAG, "Sending MQTT Enable to Device: " + uri);
                    mAdapter.add("Sending MQTT Enable to Device: " + uri, Color.BLUE);
                    mRecyclerView.smoothScrollToPosition(mAdapter.getItemCount());
                    startSendHttpRequestThread(uri);
                }
                if (ddns == true) {
                    String uri = "http://" + ipaddressDev.getText() + ":8080/enable?ddns="+ ddns_uri;
                    Log.d(TAG, "Sending DDNS URI Enable to Device: " + uri);
                    mAdapter.add("Sending DDNS URI Enable to Device: " + uri, Color.BLUE);
                    mRecyclerView.smoothScrollToPosition(mAdapter.getItemCount());
                    startSendHttpRequestThread(uri);
                }
            }
        });
        Button mqttbutton = (Button) findViewById(R.id.checkbox_mqtt);
        mqttbutton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (((CheckBox)v).isChecked()) {
                    mqtt = true;
                } else mqtt = false;
            }
        });
        Button iftttbutton = (Button) findViewById(R.id.checkbox_ifttt);
        iftttbutton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (((CheckBox)v).isChecked()) {
                    ifttt = true;
                } else ifttt = false;
            }
        });
        Button ddnsbutton = (Button) findViewById(R.id.checkbox_ddns);
        ddnsbutton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (((CheckBox)v).isChecked()) {
                    ddns = true;
                } else ddns = false;
            }
        });

        mRecyclerView = (RecyclerView) findViewById(R.id.notify_recycler_view);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mAdapter = new HistoryAdapter(new ArrayList<String>());
        mRecyclerView.setAdapter(mAdapter);

        initControls();
    }

    @Override
    public boolean onCreateOptionsMenu (Menu menu){
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    public boolean getIP(Context context) {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        String ipAddress = Formatter.formatIpAddress(wifiManager.getConnectionInfo().getIpAddress());
        ipaddressSelf.setText(ipAddress);
        if (ipAddress.equals("0.0.0.0")) {
            Log.d(TAG, "IP Address is 0.0.0.0");
            return false;
        } else
            return true;
    }

    private ArrayList<String> mDNSSearch(){
        final ArrayList<String> hosts = new ArrayList<String>();
        return hosts;
    }

    @Override
    public boolean onOptionsItemSelected (MenuItem item){
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

    // This method will be invoked when user click android device Back menu at bottom.
    @Override
    public void onBackPressed() {
        EditText mqtt_tokenF = findViewById(R.id.mqtt_val);
        mqtt_token = (mqtt_tokenF).getText().toString();
        EditText ifttt_tokenF = findViewById(R.id.ifttt_val);
        ifttt_token = (ifttt_tokenF).getText().toString();

        if (mqtt == true) {
            Intent intent = new Intent();
            Log.d(TAG, "Send mqtt token: " + mqtt_token);
            intent.putExtra("mqtt_token", mqtt_token);
            setResult(RESULT_OK, intent);
        }
        finish();
    }
    /* Start a thread to send http request to web server use HttpURLConnection object. */
    private void startSendHttpRequestThread(final String reqUrl) {
        Thread sendHttpRequestThread = new Thread(){
            @Override public void run() {
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
                    httpConn.setConnectTimeout(20000);
                    httpConn.setReadTimeout(20000);
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
                        }
                    }
                }
            };
        }
    }
}
