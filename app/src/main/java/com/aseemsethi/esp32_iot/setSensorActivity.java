package com.aseemsethi.esp32_iot;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.wifi.WifiManager;
import android.os.Bundle;
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
import java.io.InputStreamReader;
import java.util.ArrayList;

// mDNS code referred from https://www.dodgycoder.net/2015/02/setting-up-bonjourzeroconfmdnsnsd.html

public class setSensorActivity extends AppCompatActivity {
    TextView ipaddressSelf;
    final String TAG = "ESP32IOT sensor";
    private AlphaAnimation buttonClick = new AlphaAnimation(1F, 0.1F);
    String mqtt_token = "";
    private HistoryAdapter mAdapter;
    RecyclerView mRecyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_sensor);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

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

        // Read config
        try {
            BufferedReader inputReader = new BufferedReader(new InputStreamReader(
                    openFileInput("esp32configTags")));
            String inputString;
            while ((inputString = inputReader.readLine()) != null) {
                Log.d(TAG, "Reading Sensor from file");
                mAdapter.add(inputString, Color.BLUE);
                mRecyclerView.smoothScrollToPosition(mAdapter.getItemCount());
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
}
