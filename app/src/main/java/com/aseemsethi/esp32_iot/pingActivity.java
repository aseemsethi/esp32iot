package com.aseemsethi.esp32_iot;

import android.content.Context;
import android.graphics.Color;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.format.Formatter;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.os.Handler;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;

public class pingActivity extends AppCompatActivity {
    TextView ipaddressF, hostsF;
    private ProgressBar progress;
    String selfIP = "0.0.0.0";
    String subnetScan = "192.168.1.";
    ArrayList<String> subnetList;
    private Handler mHandler = new Handler();
    private HistoryAdapter mAdapter;
    RecyclerView mRecyclerView;
    String temp = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ping);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Use the following 2 lines - else use async threads
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        ipaddressF = (TextView) findViewById(R.id.ipaddress);
        hostsF = (TextView) findViewById(R.id.hosts);
        ipaddressF.setText(null);
        progress = (ProgressBar) findViewById(R.id.progressBar1);
        //make the progress bar visible
        progress.setVisibility(View.VISIBLE);

        // Initialize the self-ip and subnet scan to relevant ip addresses.
        getIP(getApplicationContext());
        EditText inputTxt = (EditText) findViewById(R.id.subnetScanUI);
        inputTxt.setText(selfIP.substring(0, selfIP.lastIndexOf('.')+1));
        System.out.println("Aseem:" + selfIP.substring(0, selfIP.lastIndexOf('.')+1));
        subnetScan = selfIP.substring(0, selfIP.lastIndexOf('.')+1);

        Button search = (Button) findViewById(R.id.searchStartB);
        search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                InputMethodManager inputManager = (InputMethodManager)
                        getSystemService(Context.INPUT_METHOD_SERVICE);
                inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),
                        InputMethodManager.HIDE_NOT_ALWAYS);

                EditText inputTxt = (EditText) findViewById(R.id.subnetScanUI);
                String typedText = inputTxt.getText().toString();
                subnetScan = typedText;
                EditText inputTxt1 = (EditText) findViewById(R.id.subnetRangeUI);
                final int subnetRange = Integer.parseInt( inputTxt1.getText().toString() );

                boolean cont = getIP(getApplicationContext());
                if (cont == false) return;
                Runnable runnable = new Runnable() {
                    @Override public void run() {
                        subnetList = scanSubNet(subnetScan, subnetRange);
                    }
                };
                new Thread(runnable).start();
            }
        });
        // Wait till thread above completes
        mRecyclerView = (RecyclerView) findViewById(R.id.iot_recycler_view);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mAdapter = new HistoryAdapter(new ArrayList<String>());
        mRecyclerView.setAdapter(mAdapter);
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
        return true;
    }

    private static boolean isReachable(String addr, int openPort, int timeOutMillis) {
        // Any Open port on other machine
        // openPort =  22 - ssh, 80 or 443 - webserver, 25 - mailserver etc.
        try {
            Socket soc = new Socket();
            soc.connect(new InetSocketAddress(addr, openPort), timeOutMillis);
            System.out.println("Reachable addr: " + addr);
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    private ArrayList<String> scanSubNet(String subnet, int subnetRange){
        final ArrayList<String> hosts = new ArrayList<String>();
        InetAddress inetAddress = null;
        progress.setMax(subnetRange);
        for(int i=1; i<=subnetRange; i++) {
            final int val = i;
            System.out.println("Trying: " + subnet + String.valueOf(i));
            mHandler.post(new Runnable() {  // or progress.post
                @Override
                public void run() {
                    System.out.println("val: " + val);
                    progress.setProgress(val);
                    /* for (String s : hosts) { */
                    if (temp != null) {
                        mAdapter.add(temp, Color.BLUE); temp = null;
                        mRecyclerView.smoothScrollToPosition(mAdapter.getItemCount());
                    }
                }
            });
            try {
                inetAddress = InetAddress.getByName(subnet + String.valueOf(i));
                if (inetAddress.isReachable(1000)) {
                    hosts.add(subnet + String.valueOf(i));
                    temp = ("Ping: " + subnet + String.valueOf(i));
                    System.out.println("Found Device using Ping: " + inetAddress.getHostName());
                } else if (isReachable(subnet + String.valueOf(i), 80, 1000)) {
                    hosts.add(subnet + String.valueOf(i));
                    temp = ("TCP:80: " + subnet + String.valueOf(i));
                    System.out.println("Found Device using Telnet:80: " + inetAddress.getHostName());
                } else if (isReachable(subnet + String.valueOf(i), 22, 1000)) {
                    hosts.add("SSH:22: " + subnet + String.valueOf(i));
                    temp = ("SSH:22: " + subnet + String.valueOf(i));
                    System.out.println("Found Device using SSH:80: " + inetAddress.getHostName());
                } else
                    temp = null;
            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
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
}
