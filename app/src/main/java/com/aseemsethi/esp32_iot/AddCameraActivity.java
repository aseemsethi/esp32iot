package com.aseemsethi.esp32_iot;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import com.rvirin.onvif.onvifcamera.OnvifDevice;
import com.rvirin.onvif.onvifcamera.OnvifListener;
import com.rvirin.onvif.onvifcamera.OnvifRequest;
import com.rvirin.onvif.onvifcamera.OnvifResponse;
import static com.rvirin.onvif.onvifcamera.OnvifDeviceKt.currentDevice;

// https://github.com/vardang/onvif
public class AddCameraActivity extends AppCompatActivity implements OnvifListener{
    final String TAG = "ESP32IOT camera";
    private AlphaAnimation buttonClick = new AlphaAnimation(1F, 0.1F);
    private HistoryAdapter mAdapter;
    RecyclerView mRecyclerView;
    String ipaddressDevice;
    private Toast toast = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_camera);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        final Activity thisActivity = this;

        Intent intent = getIntent();
        String message = intent.getStringExtra("address");
        ipaddressDevice = message;
        Log.d(TAG, "Add Camera called with Device Address: " + ipaddressDevice);

        Button save = (Button) findViewById(R.id.saveCameraT);
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                view.startAnimation(buttonClick);
                EditText userIdT = findViewById(R.id.userId);
                String userIdS = (userIdT).getText().toString();
                EditText pwdT = findViewById(R.id.pwd);
                String pwdS = (pwdT).getText().toString();
                EditText ipaddT = findViewById(R.id.ipadd);
                String ipaddS = (ipaddT).getText().toString();

                mAdapter.add("Camera Added:", Color.BLUE);
                mRecyclerView.smoothScrollToPosition(mAdapter.getItemCount());
                // Create ONVIF device with user inputs and retrieve camera informations
                Log.d(TAG, "Logging to camera with: " + userIdS + " : " + pwdS);
                currentDevice = new OnvifDevice(ipaddS, userIdS, pwdS);
                OnvifListener onvifListener = (OnvifListener) thisActivity;
                currentDevice.setListener(onvifListener);
                currentDevice.getServices();
                if (currentDevice.isConnected()) {
                    String uri = currentDevice.getRtspURI();
                    if (uri == null) {
                        Log.d(TAG, "Current Device connected, No RTSP URI");
                        return;
                    }
                    Log.d(TAG, "Current Device connected, RTSP URI: " + uri);
                }
            }
        });
        mRecyclerView = (RecyclerView) findViewById(R.id.camera_recycler_view);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mAdapter = new HistoryAdapter(new ArrayList<String>());
        mRecyclerView.setAdapter(mAdapter);
    }

    @Override
    public void requestPerformed(OnvifResponse onvifResponse) {
        Log.d(TAG, onvifResponse.getParsingUIMessage());
        cancelToast();

        if (!onvifResponse.getSuccess()) {
            Log.e(TAG, "request failed: " + onvifResponse.getRequest().getType() +
                    "\n Response: " + onvifResponse.getError());
            toast = Toast.makeText(this,
                    "⛔ " + onvifResponse.getRequest().getType() + " : " + "Failed",
                    Toast.LENGTH_SHORT);
            if (toast != null) {
                toast.show();
            }
            mAdapter.add("Request Failed: " + "\n" + onvifResponse.getRequest().getType()
                    + "\n" + onvifResponse.getError(),
                    Color.BLUE);
            mRecyclerView.smoothScrollToPosition(mAdapter.getItemCount());
            return;
        } else {
            Log.d(TAG,"Request " + onvifResponse.getRequest().getType() +
                    " performed.");
            Log.d(TAG,"Succeeded: " + onvifResponse.getSuccess() +
                    "\nmessage:" + onvifResponse.getParsingUIMessage());
            mAdapter.add("Request: " + " : " + onvifResponse.getRequest().getType()
                            + " : " + onvifResponse.getSuccess(),
                    Color.BLUE);
            mRecyclerView.smoothScrollToPosition(mAdapter.getItemCount());
        }
        // if GetServices have been completed, we request the device information
        if (onvifResponse.getRequest().getType() == OnvifRequest.Type.GetServices) {
            currentDevice.getDeviceInformation();
            Log.d(TAG, "Get Services: ");
            mAdapter.add("GetServices: " + onvifResponse.getParsingUIMessage(), Color.BLUE);
            mRecyclerView.smoothScrollToPosition(mAdapter.getItemCount());
        }
        // if GetDeviceInformation have been completed, we request the profiles
        else if (onvifResponse.getRequest().getType() == OnvifRequest.Type.GetDeviceInformation) {
            //TextView textView = findViewById(R.id.explanationTextView);
            //textView.setText(onvifResponse.getParsingUIMessage());
            Log.d(TAG, "Device Info: " + onvifResponse.getParsingUIMessage());
            toast = Toast.makeText(this, "Device information retrieved 👍",
                    Toast.LENGTH_SHORT);
            showToast();
            currentDevice.getProfiles();
            mAdapter.add(onvifResponse.getParsingUIMessage(), Color.BLUE);
            mRecyclerView.smoothScrollToPosition(mAdapter.getItemCount());
        }
        // if GetProfiles have been completed, we request the Stream URI
        else if (onvifResponse.getRequest().getType() == OnvifRequest.Type.GetProfiles) {
            int profilesCount = currentDevice.getMediaProfiles().size();
            Log.d(TAG, "Profiles: " + onvifResponse.getParsingUIMessage());
            toast = Toast.makeText(this, profilesCount + " profiles retrieved 😎",
                    Toast.LENGTH_SHORT);
            mAdapter.add(profilesCount + " Profiles: " +
                            currentDevice.getMediaProfiles().toString(), Color.BLUE);
            mRecyclerView.smoothScrollToPosition(mAdapter.getItemCount());
            showToast();
            currentDevice.getStreamURI();
        }
        // if GetStreamURI have been completed, we're ready to play the video
        else if (onvifResponse.getRequest().getType() == OnvifRequest.Type.GetStreamURI) {
            //Button button = findViewById(R.id.button);
            //button.setText(getString(R.string.Play));
            Log.d("ONVIF", "Stream URI retrieved: " + currentDevice.getRtspURI());
            toast = Toast.makeText(this,"Stream URI retrieved", Toast.LENGTH_SHORT);
            showToast();
            mAdapter.add("Profiles:" + onvifResponse.getParsingUIMessage(), Color.BLUE);
            mRecyclerView.smoothScrollToPosition(mAdapter.getItemCount());
        }
    }

    private void showToast() {if (toast != null) {toast.show();}}
    private void cancelToast() {if (toast != null) {toast.cancel();}}
    public void returnInfo() {
        Intent intent = new Intent();
        setResult(RESULT_OK, intent);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
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
        Log.d(TAG, "Back button pressed from Sensor Activity");
        returnInfo();
        finish();
    }
}