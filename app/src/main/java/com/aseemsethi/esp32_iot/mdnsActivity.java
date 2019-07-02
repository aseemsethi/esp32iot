package com.aseemsethi.esp32_iot;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
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
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;

// mDNS code referred from https://www.dodgycoder.net/2015/02/setting-up-bonjourzeroconfmdnsnsd.html

public class mdnsActivity extends AppCompatActivity {
    TextView ipaddressF;
    private ProgressBar progress;
    String selfIP = "0.0.0.0";
    String subnetScan = "192.168.1.";
    ArrayList<String> subnetList;
    private Handler mHandler = new Handler();
    private HistoryAdapter mAdapter;
    RecyclerView mRecyclerView;
    String temp = null;
    private NsdManager mNsdManager;
    private NsdManager.DiscoveryListener discoveryListener;
    private NsdManager.ResolveListener mResolveListener;
    NsdManager.RegistrationListener mRegistrationListener;
    private NsdServiceInfo mServiceInfo;
    public String mRPiAddress;
    // The NSD service type that the RPi exposes.
    private static final String SERVICE_TYPE = "_http._tcp.";
    public String mServiceName = "ESP32";
    boolean disoveryStarted = false;

    final String TAG = "ESP32IOT mDNS";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mdns);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        boolean disoveryStarted = false;

        // Use the following 2 lines - else use async threads
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        ipaddressF = (TextView) findViewById(R.id.mdns_self_ipaddress);
        ipaddressF.setText(null);

        // Initialize the self-ip and subnet scan to relevant ip addresses.
        getIP(getApplicationContext());

        Button search = (Button) findViewById(R.id.mDNSStartB);
        search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                /*InputMethodManager inputManager = (InputMethodManager)
                        getSystemService(Context.INPUT_METHOD_SERVICE);
                inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),
                        InputMethodManager.HIDE_NOT_ALWAYS);*/

                boolean cont = getIP(getApplicationContext());
                if (cont == false) {
                    mAdapter.add("Please ensure Phone has WiFi IP Address", Color.BLUE);
                    Log.d(TAG, "No WiFi IP Address");
                    mRecyclerView.smoothScrollToPosition(mAdapter.getItemCount());
                    return;
                }
                Runnable runnable = new Runnable() {
                    @Override public void run() {
                        subnetList = mDNSSearch();
                    }
                };
                new Thread(runnable).start();
            }
        });
        // Wait till thread above completes
        // hostsF.setText(subnetList.toString());
        mRecyclerView = (RecyclerView) findViewById(R.id.iot_recycler_view);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mAdapter = new HistoryAdapter(new ArrayList<String>());
        mRecyclerView.setAdapter(mAdapter);

        // mDNS stuff
        mRPiAddress = "";
        mNsdManager = (NsdManager)(getApplicationContext().getSystemService(Context.NSD_SERVICE));
        initializeResolveListener();
        initializeDiscoveryListener();
        initializeRegistrationListener();
        //mNsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener);
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
        ipaddressF.setText(ipAddress);
        if (ipAddress.equals("0.0.0.0")) {
            Log.d(TAG, "IP Address is 0.0.0.0");
            return false;
        } else
            return true;
    }

    private ArrayList<String> mDNSSearch(){
        final ArrayList<String> hosts = new ArrayList<String>();
        if (disoveryStarted == false) {
            disoveryStarted = true;
            mNsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener);
        } else {
            Log.d(TAG, "Not restarting discovery services !!!");
        }
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

    public void initializeDiscoveryListener() {

        // Instantiate a new DiscoveryListener
        discoveryListener = new NsdManager.DiscoveryListener() {

            // Called as soon as service discovery begins.
            @Override
            public void onDiscoveryStarted(String regType) {
                Log.d(TAG, "Service discovery started");
            }

            @Override
            public void onServiceFound(NsdServiceInfo service) {
                // A service was found! Do something with it.
                Log.d(TAG, "Service discovery success" + service);
                if (!service.getServiceType().equals(SERVICE_TYPE)) {
                    // Service type is the string containing the protocol and
                    // transport layer for this service.
                    Log.d(TAG, "Unknown Service Type: " + service.getServiceType());
                } else if (service.getServiceName().contains("ESP32")){
                    //mNsdManager.resolveService(service, mResolveListener);
                } else {
                    //mNsdManager.resolveService(service, mResolveListener);
                }
                mNsdManager.resolveService(service, new NsdManager.ResolveListener() {
                    @Override
                    public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                        Log.e(TAG, "Resolve Failed: ..." + serviceInfo);
                    }
                    @Override
                    public void onServiceResolved(final NsdServiceInfo serviceInfo) {
                        Log.i(TAG, "Found Service Resolved: " + serviceInfo);
                        mServiceInfo = serviceInfo;
                        final int port = mServiceInfo.getPort();
                        InetAddress host = mServiceInfo.getHost();
                        final String address = host.getHostAddress();
                        Log.d(TAG, "Resolved address : " + address + " : " + port);
                        mRPiAddress = address;
                        mHandler.post(new Runnable() {  // or progress.post
                            @Override
                            public void run() {
                                mAdapter.add(serviceInfo.getServiceName() + ", "
                                                + serviceInfo.getServiceType() + ", "
                                                + address + ", " + port
                                        , Color.BLUE);
                                mRecyclerView.smoothScrollToPosition(mAdapter.getItemCount());
                            }
                        });
                    }
                });
            }

            @Override
            public void onServiceLost(NsdServiceInfo service) {
                // When the network service is no longer available.
                // Internal bookkeeping code goes here.
                Log.e(TAG, "service lost: " + service);
            }

            @Override
            public void onDiscoveryStopped(String serviceType) {
                Log.i(TAG, "Discovery stopped: " + serviceType);
                disoveryStarted = false;
            }

            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Start Discovery failed: Error code:" + errorCode);
                disoveryStarted = false;
            }

            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Stop Discovery failed: Error code:" + errorCode);
                //mNsdManager.stopServiceDiscovery(this);
                disoveryStarted = false;
            }
        };
    }

    // This does not work - we need to create a new ResolverListener for each device found
    // https://stackoverflow.com/questions/25815162/listener-already-in-use-service-discovery
    public void initializeResolveListener() {
        mResolveListener = new NsdManager.ResolveListener() {

            @Override
            public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                // Called when the resolve fails. Use the error code to debug.
                Log.e(TAG, "Resolve failed: " + errorCode);
            }

            @Override
            public void onServiceResolved(final NsdServiceInfo serviceInfo) {
                Log.e(TAG, "Resolve Succeeded. " + serviceInfo);

                mServiceInfo = serviceInfo;
                final int port = mServiceInfo.getPort();
                InetAddress host = mServiceInfo.getHost();
                final String address = host.getHostAddress();
                Log.d(TAG, "Resolved address = " + address + " : " + port);
                mRPiAddress = address;
                mHandler.post(new Runnable() {  // or progress.post
                    @Override
                    public void run() {
                        mAdapter.add(serviceInfo.getServiceName() + ", "
                                + serviceInfo.getServiceType() + ", "
                                + address + ", " + port
                                , Color.BLUE);
                        mRecyclerView.smoothScrollToPosition(mAdapter.getItemCount());
                    }
                });
            }
        };
    }

    public void initializeRegistrationListener() {
        mRegistrationListener = new NsdManager.RegistrationListener() {
            @Override
            public void onServiceRegistered(NsdServiceInfo NsdServiceInfo) {
                mServiceName = NsdServiceInfo.getServiceName();
            }

            @Override
            public void onRegistrationFailed(NsdServiceInfo arg0, int arg1) {
            }
            @Override
            public void onServiceUnregistered(NsdServiceInfo arg0) {
            }

            @Override
            public void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
            }

        };
    }
    public void registerService(int port) {
        NsdServiceInfo serviceInfo  = new NsdServiceInfo();
        serviceInfo.setPort(port);
        serviceInfo.setServiceName(mServiceName);
        serviceInfo.setServiceType(SERVICE_TYPE);

        mNsdManager.registerService(
                serviceInfo, NsdManager.PROTOCOL_DNS_SD, mRegistrationListener);

    }
    public void discoverServices() {
        mNsdManager.discoverServices(
                SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener);
    }

    public void stopListening() {
        if (disoveryStarted)
            mNsdManager.stopServiceDiscovery(discoveryListener);
        disoveryStarted = false;
    }
    // This method will be invoked when user click android device Back menu at bottom.
    @Override
    public void onBackPressed() {
        stopListening();
        Intent intent = new Intent();
        intent.putExtra("Address", mRPiAddress);
        setResult(RESULT_OK, intent);
        finish();
    }

}
