/*
 * Copyright (C) 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.telephony.testapps.ts43entitlementtestserverapp;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.IOException;

/**
 * Main activity for the TS.43 Test Server application.
 */
public class Ts43TestServerActivity extends Activity {
    private static final String TAG = "Ts43TestServerActivity";
    private static final int REQUEST_CODE_PERMISSIONS = 1;
    private static final long CONFIG_OVERRIDE_DELAY_MS = 1000;

    private Button mServerButton;
    private TextView mServerStatusTextView;
    private TextView mLogTextView;
    private Spinner mResponseCodeSpinner;
    private LinearLayout mRetryAfterLayout;
    private EditText mRetryAfterEditText;
    private CheckBox mBypassEapAkaCheckbox;

    private HttpServer mHttpServer;
    private CarrierConfigManagerHelper mConfigHelper;
    private Handler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ts43_test_server_activity);

        setupEdgeToEdge(this);

        mServerButton = findViewById(R.id.serverButton);
        mServerStatusTextView = findViewById(R.id.serverStatusTextView);
        mLogTextView = findViewById(R.id.logTextView);
        mResponseCodeSpinner = findViewById(R.id.responseCodeSpinner);
        mRetryAfterLayout = findViewById(R.id.retryAfterLayout);
        mRetryAfterEditText = findViewById(R.id.retryAfterEditText);
        mBypassEapAkaCheckbox = findViewById(R.id.bypassEapAkaCheckbox);

        mHttpServer = new HttpServer(this);
        mConfigHelper = new CarrierConfigManagerHelper(this);
        mHandler = new Handler(Looper.getMainLooper());

        setupSpinner();
        setupCheckbox();

        mServerButton.setOnClickListener(v -> {
            if (mHttpServer.isRunning()) {
                mHttpServer.stop();
                mConfigHelper.restoreOriginalConfig();
                mServerButton.setText(R.string.start_server);
                logToServerStatus(getString(R.string.server_down));
            } else {
                try {
                    mHttpServer.start();
                    mServerButton.setText(R.string.stop_server);
                    logToServerStatus("Server running on the port " + HttpServer.SERVER_PORT);
                    mHandler.postDelayed(() -> mConfigHelper.overrideConfigForTest(),
                            CONFIG_OVERRIDE_DELAY_MS);
                } catch (IOException e) {
                    Log.e(TAG, "Failed to start server", e);
                    Toast.makeText(this, "Failed to start server: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (checkSelfPermission(Manifest.permission.READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_PHONE_STATE},
                    REQUEST_CODE_PERMISSIONS);
        }
    }

    /**
     * Given an activity, configure the activity to adjust for edge to edge restrictions.
     * @param activity the activity.
     */
    public static void setupEdgeToEdge(Activity activity) {
        ViewCompat.setOnApplyWindowInsetsListener(activity.findViewById(android.R.id.content),
                (v, windowInsets) -> {
                    Insets insets = windowInsets.getInsets(
                            WindowInsetsCompat.Type.systemBars()
                                    | WindowInsetsCompat.Type.ime());

                    // Apply the insets paddings to the view.
                    v.setPadding(insets.left, insets.top, insets.right, insets.bottom);

                    // Return CONSUMED if you don't want the window insets to keep being
                    // passed down to descendant views.
                    return WindowInsetsCompat.CONSUMED;
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mHttpServer != null && mHttpServer.isRunning()) {
            mHttpServer.stop();
            mConfigHelper.restoreOriginalConfig();
        }
    }

    private void setupSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.response_codes, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mResponseCodeSpinner.setAdapter(adapter);
        mResponseCodeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selected = (String) parent.getItemAtPosition(position);
                mRetryAfterLayout.setVisibility("503".equals(selected) ? View.VISIBLE : View.GONE);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                mRetryAfterLayout.setVisibility(View.GONE);
            }
        });
    }

    private void setupCheckbox() {
        mBypassEapAkaCheckbox.setOnCheckedChangeListener((buttonView,
                isChecked) -> {
            if (isChecked) {
                Toast.makeText(this,
                    "EAP-AKA bypass is ON. Ensure property is set via adb.",
                    Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this,
                    "EAP-AKA bypass is OFF. Ensure property is cleared for real SIM auth.",
                    Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Get Selected ResponseCode
     * @return response code
     */
    public int getSelectedResponseCode() {
        String selected = (String) mResponseCodeSpinner.getSelectedItem();
        return Integer.parseInt(selected);
    }

    /**
     * Get RetryAfter Value
     * @return retry-after value
     */
    public String getRetryAfterValue() {
        return mRetryAfterEditText.getText().toString();
    }

    /**
     * Logs a status to the server status textview.
     * @param message
     */
    public void logToServerStatus(final String message) {
        runOnUiThread(() -> mServerStatusTextView.setText(message));
    }

    /**
     * Logs a message to the log textview.
     * @param message
     */
    public void logToRequestView(final String message) {
        runOnUiThread(() -> {
            String currentLogs = mLogTextView.getText().toString();
            mLogTextView.setText(message + "\n---\n" + currentLogs);
        });
    }
}

