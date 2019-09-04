package com.aseemsethi.esp32_iot;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
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
import android.widget.Toast;

import java.util.ArrayList;

public class CameraActivity extends AppCompatActivity {
    final String TAG = "ESP32IOT camera";
    private AlphaAnimation buttonClick = new AlphaAnimation(1F, 0.1F);
    private HistoryAdapter mAdapter;
    RecyclerView mRecyclerView;
    String ipaddressDevice;
    private Toast toast = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        final Activity thisActivity = this;

        Intent intent = getIntent();
        String message = intent.getStringExtra("address");
        ipaddressDevice = message;

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

                mAdapter.add("Adding Camera:", Color.BLUE);
                mRecyclerView.smoothScrollToPosition(mAdapter.getItemCount());

            }
        });
        mRecyclerView = (RecyclerView) findViewById(R.id.camera_recycler_view);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mAdapter = new HistoryAdapter(new ArrayList<String>());
        mRecyclerView.setAdapter(mAdapter);
    }

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