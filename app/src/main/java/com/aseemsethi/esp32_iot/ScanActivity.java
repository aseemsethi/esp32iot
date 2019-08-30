package com.aseemsethi.esp32_iot;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
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
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import io.evercam.network.EvercamDiscover;
import io.evercam.network.OnvifRunnable;
import io.evercam.network.UpnpRunnable;
import io.evercam.network.discovery.DiscoveredCamera;
import io.evercam.network.discovery.NetworkInfo;
import io.evercam.network.discovery.UpnpDevice;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ScanActivity extends AppCompatActivity {
    TextView ipaddressSelf, ipaddressDev;
    private HistoryAdapter mAdapter;
    RecyclerView mRecyclerView;
    final String TAG = "ESP32IOT Scan";
    private AlphaAnimation buttonClick = new AlphaAnimation(1F, 0.1F);
    private static final int REQUEST_WIFI = 1;
    private static final String KEY_RESPONSE_TEXT = "KEY_RESPONSE_TEXT";
    private Handler uiUpdater = null;
    String deviceAddress = "";
    String ipAddress;
    //private String externalIp = "";
    public ExecutorService pool;
    boolean scanOver = false;
    boolean upnpDone = false;
    private ArrayList<DiscoveredCamera> onvifDeviceList =
            new ArrayList<DiscoveredCamera>();
    public ArrayList<UpnpDevice> upnpDeviceList
            = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.scan_activity);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        //externalIp = NetworkInfo.getExternalIP();
        pool = Executors.newFixedThreadPool(EvercamDiscover.DEFAULT_FIXED_POOL);

        // Use the following 2 lines - else use async threads
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        ipaddressSelf = (TextView) findViewById(R.id.scan_self_ipaddress);
        ipaddressSelf.setText(null);
        getIP(getApplicationContext());

        ipaddressDev = (TextView) findViewById(R.id.scan_dev_ipaddress);
        Intent intent = getIntent();
        String message = intent.getStringExtra("address");
        ipaddressDev.setText(message);
        deviceAddress = message;

        final Button httpB = findViewById(R.id.scan_b);
        httpB.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(ipAddress.isEmpty()) {
                    mAdapter.add("Set WiFi IP first", Color.BLUE);
                    mRecyclerView.smoothScrollToPosition(mAdapter.getItemCount());
                    return;
                }
                scanOver = false;
                upnpDone = false;
                v.startAnimation(buttonClick);
                pool.execute(onvifRunnable);
                pool.execute(upnpRunnable);
                mAdapter.add("Discovering Cameras..", Color.BLUE);
                mRecyclerView.smoothScrollToPosition(mAdapter.getItemCount());
            }
        });
        final Button clrB = findViewById(R.id.clr_b);
        clrB.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.startAnimation(buttonClick);
                mAdapter.clear();
            }
        });
        final Button showB = findViewById(R.id.show_b);
        showB.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.startAnimation(buttonClick);
                if (scanOver == false) {
                    mAdapter.add("Still Discovering onvif..", Color.BLUE);
                    mRecyclerView.smoothScrollToPosition(mAdapter.getItemCount());
                    return;
                }
                if (onvifDeviceList.size() > 0) {
                    for (DiscoveredCamera discoveredCamera : onvifDeviceList) {
                        Log.d(TAG, discoveredCamera.toString() + ":" +
                                discoveredCamera.getIP());
                        mAdapter.add(discoveredCamera.toString() + ":" +
                                discoveredCamera.getIP() +
                                discoveredCamera.getModel(), Color.BLUE);
                        mRecyclerView.smoothScrollToPosition(mAdapter.getItemCount());
                    }
                } else {
                    Log.d(TAG, "No Onvif Cameras Discovered");
                    mAdapter.add("No Onvif Cameras Discovered", Color.BLUE);
                    mRecyclerView.smoothScrollToPosition(mAdapter.getItemCount());
                }
                if (upnpDone == false) {
                    mAdapter.add("Still Discovering upnp..", Color.BLUE);
                    mRecyclerView.smoothScrollToPosition(mAdapter.getItemCount());
                    return;
                }
                if (upnpDeviceList.size() > 0) {
                    for (UpnpDevice discoveredCamera : upnpDeviceList) {
                        Log.d(TAG, discoveredCamera.toString() + ":" +
                                discoveredCamera.getIp());
                        mAdapter.add(discoveredCamera.toString() + ":" +
                                discoveredCamera.getIp() +
                                discoveredCamera.getModel(), Color.BLUE);
                        mRecyclerView.smoothScrollToPosition(mAdapter.getItemCount());
                    }
                } else {
                    Log.d(TAG, "No upnp Cameras Discovered");
                    mAdapter.add("No upnp Cameras Discovered", Color.BLUE);
                    mRecyclerView.smoothScrollToPosition(mAdapter.getItemCount());
                }
            }
        });
        mRecyclerView = (RecyclerView) findViewById(R.id.scan_recycler_view);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mAdapter = new HistoryAdapter(new ArrayList<String>());
        mRecyclerView.setAdapter(mAdapter);
    }
    // This method will be invoked when user click android device Back menu at bottom.
    @Override
    public void onBackPressed() {
        Log.d(TAG, "Back button pressed from Add Camera Activity");
        Intent intent = new Intent();
        Log.d(TAG, "Send Camera info: ");
        intent.putExtra("onvifDevices", onvifDeviceList);
        setResult(RESULT_OK, intent);
        finish();
    }

    private OnvifRunnable onvifRunnable = new OnvifRunnable() {
        @Override
        public void onFinished() {
            Log.d(TAG, "OnvifRunnable: Finished");
            scanOver = true;
        }

        @Override
        public void onDeviceFound(DiscoveredCamera discoveredCamera) {
            //discoveredCamera.setExternalIp(externalIp);
            Log.d(TAG, discoveredCamera.toString() + ":" +
                    discoveredCamera.getIP());
            onvifDeviceList.add(discoveredCamera);
        }
    };

    private UpnpRunnable upnpRunnable = new UpnpRunnable() {
        @Override
        public void onDeviceFound(UpnpDevice upnpDevice) {
            Log.d(TAG, "UPnP device found: " + upnpDevice.toString());
            upnpDeviceList.add(upnpDevice);
            // If IP address matches
            String ipFromUpnp = upnpDevice.getIp();
            Log.d(TAG, upnpDevice.getModel() + ":" +upnpDevice.getIp());
        }

        @Override
        public void onFinished(ArrayList<UpnpDevice> arrayList) {
            Log.d(TAG, "upnpRunnable: Finished");
            upnpDone = true;
        }
    };

    @Override
    public boolean onCreateOptionsMenu (Menu menu){
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    public boolean getIP(Context context) {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        ipAddress = Formatter.formatIpAddress(wifiManager.getConnectionInfo().getIpAddress());
        ipaddressSelf.setText(ipAddress);
        if (ipAddress.equals("0.0.0.0")) {
            Log.d(TAG, "IP Address is 0.0.0.0");
            return false;
        } else
            return true;
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
}
