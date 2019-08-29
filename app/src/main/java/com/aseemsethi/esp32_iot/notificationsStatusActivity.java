package com.aseemsethi.esp32_iot;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
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
import android.view.animation.AlphaAnimation;
import android.widget.Button;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

// mDNS code referred from https://www.dodgycoder.net/2015/02/setting-up-bonjourzeroconfmdnsnsd.html

public class notificationsStatusActivity extends AppCompatActivity {
    TextView ipaddressSelf, ipaddressDev;
    private HistoryAdapter mAdapter;
    RecyclerView mRecyclerView;
    public String mRPiAddress;
    final String TAG = "ESP32IOT notify";
    private AlphaAnimation buttonClick = new AlphaAnimation(1F, 0.1F);
    private Handler uiUpdater = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notificationsstatus);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        ipaddressSelf = (TextView) findViewById(R.id.notifys_self_ipaddress);
        ipaddressSelf.setText(null);
        getIP(getApplicationContext());

        ipaddressDev = (TextView) findViewById(R.id.notifys_dev_ipaddress);
        Intent intent = getIntent();
        String message = intent.getStringExtra("address");
        ipaddressDev.setText(message);

        Button search = findViewById(R.id.clearNotificationLogs);
        search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                view.startAnimation(buttonClick);
                mAdapter.clear();
                Log.d(TAG, "Deleting esp32Notifications file");
                deleteFile("esp32Notifications");
                /*
                FileOutputStream fos;
                try {
                    fos = openFileOutput("esp32Notifications", Context.MODE_PRIVATE);
                    fos.close();
                } catch (FileNotFoundException e) {e.printStackTrace();}
                catch (IOException e) {e.printStackTrace();}
                */
                mAdapter.add("Notification Logs..", Color.BLUE);
                mRecyclerView.smoothScrollToPosition(mAdapter.getItemCount());
            }
        });

        mRecyclerView = (RecyclerView) findViewById(R.id.notify_recycler_view);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mAdapter = new HistoryAdapter(new ArrayList<String>());
        mRecyclerView.setAdapter(mAdapter);
        mAdapter.add("Notification Logs..", Color.BLUE);
        mRecyclerView.smoothScrollToPosition(mAdapter.getItemCount());
        try {
            BufferedReader inputReader = new BufferedReader(new InputStreamReader(
                    openFileInput("esp32Notifications")));
            String inputString;
            while ((inputString = inputReader.readLine()) != null) {
                mAdapter.add(inputString, Color.BLUE);
                mRecyclerView.smoothScrollToPosition(mAdapter.getItemCount());
            }
        } catch (IOException e) { e.printStackTrace();}
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
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

    private ArrayList<String> mDNSSearch() {
        final ArrayList<String> hosts = new ArrayList<String>();
        return hosts;
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

    // This method will be invoked when user click android device Back menu at bottom.
    @Override
    public void onBackPressed() {
        finish();
    }
}