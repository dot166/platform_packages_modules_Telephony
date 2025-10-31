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

import android.content.Context;
import android.os.PersistableBundle;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.util.Log;
import android.widget.Toast;

import java.util.List;

/**
 * A helper class to manage CarrierConfig overrides for testing purposes.
 */
public class CarrierConfigManagerHelper {
    private static final String TAG = "CarrierConfigHelper";
    private static final String LOCAL_ENTITLEMENT_SERVER_URL = "http://127.0.0.1:5555";
    private static final String KEY_ENTITLEMENT_SERVER_URL_STRING =
            "imsserviceentitlement.entitlement_server_url_string";

    private static final String KEY_SUPPORT_PHONE_NUMBER_SOURCE_TS43_BOOL =
            "support_phone_number_source_ts43_bool";

    private final Context mContext;
    private final CarrierConfigManager mCarrierConfigManager;
    private final SubscriptionManager mSubscriptionManager;
    private PersistableBundle mOriginalConfigs;

    public CarrierConfigManagerHelper(Context context) {
        mContext = context;
        mCarrierConfigManager = mContext.getSystemService(CarrierConfigManager.class);
        mSubscriptionManager = mContext.getSystemService(SubscriptionManager.class);
    }

    /**
     * override config for test
     */
    public void overrideConfigForTest() {
        int subId = getActiveSubId();
        if (subId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            Toast.makeText(mContext, "No active subscription found.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (mOriginalConfigs == null) {
            mOriginalConfigs = mCarrierConfigManager.getConfigForSubId(subId,
                    KEY_ENTITLEMENT_SERVER_URL_STRING,
                    KEY_SUPPORT_PHONE_NUMBER_SOURCE_TS43_BOOL);
            Log.d(TAG, "Saved original configs: " + mOriginalConfigs);
        }

        PersistableBundle bundleToModify = new PersistableBundle();
        bundleToModify.putString(KEY_ENTITLEMENT_SERVER_URL_STRING, LOCAL_ENTITLEMENT_SERVER_URL);
        bundleToModify.putBoolean(KEY_SUPPORT_PHONE_NUMBER_SOURCE_TS43_BOOL, true);
        Log.d(TAG, "Overriding carrier config with: " + bundleToModify);
        try {
            mCarrierConfigManager.overrideConfig(subId, bundleToModify, false);
            Toast.makeText(mContext, "CarrierConfig overridden for local test.",
                    Toast.LENGTH_SHORT).show();
        } catch (SecurityException e) {
            Log.e(TAG, "Failed to override CarrierConfig. Ensure app is privileged.", e);
            Toast.makeText(mContext, "Permission denied to override CarrierConfig.",
                    Toast.LENGTH_LONG).show();
        }
    }

    /**
     * restore original config
     */
    public void restoreOriginalConfig() {
        int subId = getActiveSubId();
        if (subId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            return;
        }
        if (mOriginalConfigs != null) {
            Log.d(TAG, "Restoring original configs: " + mOriginalConfigs);
            try {
                mCarrierConfigManager.overrideConfig(subId, mOriginalConfigs, false);
                mOriginalConfigs = null;
                Toast.makeText(mContext, "Restored original CarrierConfig.",
                        Toast.LENGTH_SHORT).show();
            } catch (SecurityException e) {
                Log.e(TAG, "Failed to restore CarrierConfig.", e);
            }
        }
    }

    private int getActiveSubId() {
        List<SubscriptionInfo> subInfoList = mSubscriptionManager.getActiveSubscriptionInfoList();
        if (subInfoList != null && !subInfoList.isEmpty()) {
            return subInfoList.get(0).getSubscriptionId();
        }
        return SubscriptionManager.INVALID_SUBSCRIPTION_ID;
    }
}

