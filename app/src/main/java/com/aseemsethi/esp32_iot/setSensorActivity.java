package com.aseemsethi.esp32_iot;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
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
import android.widget.EditText;
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

// mDNS code referred from https://www.dodgycoder.net/2015/02/setting-up-bonjourzeroconfmdnsnsd.html

public class setSensorActivity extends AppCompatActivity {
    TextView ipaddressSelf;
    final String TAG = "ESP32IOT sensor";
    private AlphaAnimation buttonClick = new AlphaAnimation(1F, 0.1F);
    String mqtt_token = "";
    private HistoryAdapter mAdapter;
    RecyclerView mRecyclerView;
    private static final int REQUEST_WIFI = 1;
    private static final String KEY_RESPONSE_TEXT = "KEY_RESPONSE_TEXT";
    private Handler uiUpdater = null;
    String ipaddressDevice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_sensor);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        TextView ipaddressDev = (TextView) findViewById(R.id.notify_dev_ipaddress);
        Intent intent = getIntent();
        String message = intent.getStringExtra("address");
        ipaddressDevice = message;
        Log.d(TAG, "SetSendor called with Device Address: " + ipaddressDevice);

        Button save = (Button) findViewById(R.id.saveSensorT);
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                view.startAnimation(buttonClick);
                EditText sensorName = findViewById(R.id.sensorNameT);
                String sensorNameS = (sensorName).getText().toString();
                EditText sensorTag = findViewById(R.id.sensorTagT);
                String sensorTagS = (sensorTag).getText().toString();

                // Store Sensor in File
                FileOutputStream fos;
                try {
                    fos = openFileOutput("esp32configTags", Context.MODE_APPEND);
                    //default mode is PRIVATE, can be APPEND etc.
                    fos.write(sensorNameS.getBytes());
                    fos.write(":".getBytes());
                    fos.write(sensorTagS.getBytes());
                    fos.write("\n".getBytes());
                    Log.d(TAG, "Saving Sensor to file" + sensorNameS + ":" + sensorTagS);
                    fos.close();
                    mAdapter.add(sensorNameS + ":" + sensorTagS, Color.BLUE);
                    mRecyclerView.smoothScrollToPosition(mAdapter.getItemCount());
                    String uri = "http://" + ipaddressDevice + ":8080/enable?ble="+
                            sensorNameS + ":" + sensorTagS;
                    Log.d(TAG, "Sending BLE URI Enable to Device: " + uri);
                    startSendHttpRequestThread(uri);
                } catch (FileNotFoundException e) {e.printStackTrace();}
                catch (IOException e) {e.printStackTrace();}

                /* Intent intent = new Intent();
                Log.d(TAG, "Send Sensor info: " + sensorNameS + " : " + sensorTagS);
                intent.putExtra("sensorName", sensorNameS);
                intent.putExtra("sensorTag", sensorTagS);
                setResult(RESULT_OK, intent);
                finish(); */
            }
        });
        mRecyclerView = (RecyclerView) findViewById(R.id.sensor_recycler_view);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mAdapter = new HistoryAdapter(new ArrayList<String>());
        mRecyclerView.setAdapter(mAdapter);
        initControls();

        // Read config
        try {
            BufferedReader inputReader = new BufferedReader(new InputStreamReader(
                    openFileInput("esp32configTags")));
            String inputString;
            while ((inputString = inputReader.readLine()) != null) {
                Log.d(TAG, "Reading Sensor from file");
                mAdapter.add(inputString, Color.BLUE);
                mRecyclerView.smoothScrollToPosition(mAdapter.getItemCount());
                String uri = "http://" + ipaddressDevice + ":8080/enable?ble="+ inputString;
                Log.d(TAG, "Sending BLE URI Enable to Device: " + uri);
                //startSendHttpRequestThread(uri);
            }
        } catch (IOException e) { e.printStackTrace();}
    }

    @Override
    public boolean onCreateOptionsMenu (Menu menu){
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
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
        Log.d(TAG, "Back button pressed from Sensor Activity");
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
