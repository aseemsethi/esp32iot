package com.aseemsethi.esp32_iot;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
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
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

// mDNS code referred from https://www.dodgycoder.net/2015/02/setting-up-bonjourzeroconfmdnsnsd.html

public class setMqttActivity extends AppCompatActivity {
    TextView ipaddressSelf;
    final String TAG = "ESP32IOT mqtt";
    private AlphaAnimation buttonClick = new AlphaAnimation(1F, 0.1F);
    String mqtt_token = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_mqtt);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ipaddressSelf = (TextView) findViewById(R.id.mqtt_self_ipaddress);
        ipaddressSelf.setText(null);
        getIP(getApplicationContext());

        Button search = (Button) findViewById(R.id.setMqttB);
        search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                view.startAnimation(buttonClick);
                EditText mqtt_tokenF = findViewById(R.id.mqttServer_val);
                mqtt_token = (mqtt_tokenF).getText().toString();
                boolean cont = getIP(getApplicationContext());
                if (cont == false) {
                    Log.d(TAG, "No WiFi IP Address");
                    CharSequence text = "Connect to WiFi first";
                    Toast toast = Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT);
                    toast.show();
                    return;
                }
                Intent intent = new Intent();
                Log.d(TAG, "Send mqtt token: " + mqtt_token);
                intent.putExtra("mqtt_token", mqtt_token);
                setResult(RESULT_OK, intent);
                finish();
            }
        });
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
        EditText mqtt_tokenF = findViewById(R.id.mqttServer_val);
        mqtt_token = (mqtt_tokenF).getText().toString();

        Intent intent = new Intent();
        Log.d(TAG, "Send mqtt token: " + mqtt_token);
        intent.putExtra("mqtt_token", mqtt_token);
        setResult(RESULT_OK, intent);
        finish();
    }
}
