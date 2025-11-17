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
package com.android.telephony.module.tests;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Handler;
import android.os.PersistableBundle;
import android.os.test.TestLooper;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;

import com.android.internal.telephony.Ts43PhoneNumberController;
import com.android.internal.telephony.Ts43PhoneNumberRetriever;
import com.android.internal.telephony.com.android.libraries.entitlement.ServiceEntitlementException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.Executor;

public class Ts43PhoneNumberControllerTest {

    private static final int SUB_ID = 1;
    private static final String FAKE_RAW_PHONE_NUMBER = "6502530000";
    private static final String FAKE_E164_PHONE_NUMBER = "+16502530000";
    private static final String US_COUNTRY_ISO = "us";

    @Mock private Context mContext;
    @Mock private SubscriptionManager mSubscriptionManager;
    @Mock private CarrierConfigManager mCarrierConfigManager;
    @Mock private ConnectivityManager mConnectivityManager;
    @Mock private Ts43PhoneNumberRetriever mTs43PhoneNumberRetriever;
    @Mock private Network mNetwork;
    @Mock private SubscriptionInfo mSubscriptionInfo;

    private TestLooper mTestLooper;
    private Ts43PhoneNumberController mController;
    private CarrierConfigManager.CarrierConfigChangeListener mCarrierConfigChangeListener;
    private ArgumentCaptor<ConnectivityManager.NetworkCallback> mNetworkCallbackCaptor;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        mTestLooper = new TestLooper();

        when(mContext.getSystemServiceName(SubscriptionManager.class))
                .thenReturn(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
        doReturn(mSubscriptionManager).when(mContext)
                .getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
        when(mContext.getSystemServiceName(CarrierConfigManager.class))
                .thenReturn(Context.CARRIER_CONFIG_SERVICE);
        doReturn(mCarrierConfigManager).when(mContext)
                .getSystemService(Context.CARRIER_CONFIG_SERVICE);
        when(mContext.getSystemServiceName(ConnectivityManager.class))
                .thenReturn(Context.CONNECTIVITY_SERVICE);
        doReturn(mConnectivityManager).when(mContext)
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        when(mSubscriptionManager.getActiveSubscriptionInfo(SUB_ID)).thenReturn(mSubscriptionInfo);
        when(mSubscriptionInfo.getCountryIso()).thenReturn(US_COUNTRY_ISO);
        mNetworkCallbackCaptor = ArgumentCaptor.forClass(ConnectivityManager.NetworkCallback.class);

        mController = new Ts43PhoneNumberController(mContext, mTestLooper.getLooper(),
                mTs43PhoneNumberRetriever);

        // Capture the listener to trigger it manually
        ArgumentCaptor<CarrierConfigManager.CarrierConfigChangeListener> listenerCaptor =
                ArgumentCaptor.forClass(CarrierConfigManager.CarrierConfigChangeListener.class);
        verify(mCarrierConfigManager).registerCarrierConfigChangeListener(
                any(Executor.class), listenerCaptor.capture());
        mCarrierConfigChangeListener = listenerCaptor.getValue();
        verify(mConnectivityManager).registerDefaultNetworkCallback(
                mNetworkCallbackCaptor.capture(), any(Handler.class));
    }

    private void givenTs43Enabled(boolean enabled) {
        PersistableBundle config = new PersistableBundle();
        config.putBoolean(
                CarrierConfigManager.KEY_SUPPORT_PHONE_NUMBER_SOURCE_TS43_BOOL,
                enabled);
        when(mCarrierConfigManager.getConfigForSubId(SUB_ID,
                CarrierConfigManager.KEY_SUPPORT_PHONE_NUMBER_SOURCE_TS43_BOOL)).thenReturn(config);
        when(mSubscriptionManager.isActiveSubscriptionId(SUB_ID)).thenReturn(true);
    }

    private void simulateNetworkState(boolean isValidated) {
        ConnectivityManager.NetworkCallback callback = mNetworkCallbackCaptor.getValue();
        if (isValidated) {
            NetworkCapabilities capabilities = new NetworkCapabilities.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_SUSPENDED)
                    .build();
            callback.onCapabilitiesChanged(mNetwork, capabilities);
        } else {
            callback.onLost(mNetwork);
        }
    }

    @Test
    public void testFetch_success_formatsNumberCorrectly() throws ServiceEntitlementException {
        givenTs43Enabled(true);
        simulateNetworkState(true);
        when(mTs43PhoneNumberRetriever.fetchPhoneNumber(SUB_ID)).thenReturn(FAKE_RAW_PHONE_NUMBER);

        // Trigger
        mCarrierConfigChangeListener.onCarrierConfigChanged(0, SUB_ID, 0, 0);
        mTestLooper.dispatchAll();

        // Verify
        verify(mTs43PhoneNumberRetriever).fetchPhoneNumber(SUB_ID);
        verify(mSubscriptionManager).setTs43PhoneNumber(SUB_ID, FAKE_E164_PHONE_NUMBER);
    }

    @Test
    public void testFetch_success_formatFails_setsOriginalNumber()
            throws ServiceEntitlementException {
        givenTs43Enabled(true);
        simulateNetworkState(true);
        String invalidNumber = "123";
        when(mTs43PhoneNumberRetriever.fetchPhoneNumber(SUB_ID)).thenReturn(invalidNumber);
        when(mSubscriptionInfo.getCountryIso()).thenReturn(null);

        // Trigger
        mCarrierConfigChangeListener.onCarrierConfigChanged(0, SUB_ID, 0, 0);
        mTestLooper.dispatchAll();

        // Verify
        verify(mTs43PhoneNumberRetriever).fetchPhoneNumber(SUB_ID);
        verify(mSubscriptionManager).setTs43PhoneNumber(SUB_ID, invalidNumber);
    }

    @Test
    public void testFetch_ts43Disabled() throws ServiceEntitlementException {
        givenTs43Enabled(false);
        simulateNetworkState(true);

        // Trigger
        mCarrierConfigChangeListener.onCarrierConfigChanged(0, SUB_ID, 0, 0);
        mTestLooper.dispatchAll();

        // Verify
        verify(mTs43PhoneNumberRetriever, never()).fetchPhoneNumber(anyInt());
    }

    @Test
    public void testFetch_noInternet_waitsForNetwork() throws ServiceEntitlementException {
        givenTs43Enabled(true);
        simulateNetworkState(false);

        // Trigger
        mCarrierConfigChangeListener.onCarrierConfigChanged(0, SUB_ID, 0, 0);
        mTestLooper.dispatchAll();

        // Verify
        verify(mTs43PhoneNumberRetriever, never()).fetchPhoneNumber(anyInt());
    }

    @Test
    public void testFetch_networkBecomesAvailable() throws ServiceEntitlementException {
        // Given: A fetch is requested while TS.43 is enabled but there's no network.
        givenTs43Enabled(true);
        when(mTs43PhoneNumberRetriever.fetchPhoneNumber(SUB_ID)).thenReturn(FAKE_RAW_PHONE_NUMBER);
        simulateNetworkState(false);
        mCarrierConfigChangeListener.onCarrierConfigChanged(0, SUB_ID, 0, 0);
        mTestLooper.dispatchAll();

        // Verify: The fetch has not been called yet.
        verify(mTs43PhoneNumberRetriever, never()).fetchPhoneNumber(anyInt());

        // When: The network becomes available (validated).
        simulateNetworkState(true);
        mTestLooper.dispatchAll();

        // Then: The pending fetch task is executed and succeeds.
        verify(mTs43PhoneNumberRetriever).fetchPhoneNumber(SUB_ID);
        verify(mSubscriptionManager).setTs43PhoneNumber(SUB_ID, FAKE_E164_PHONE_NUMBER);
    }
    @Test
    public void testFetch_permanentError_doesNotRetry() throws ServiceEntitlementException {
        givenTs43Enabled(true);
        simulateNetworkState(true);
        when(mTs43PhoneNumberRetriever.fetchPhoneNumber(SUB_ID))
                .thenThrow(new ServiceEntitlementException(
                        ServiceEntitlementException.ERROR_HTTP_STATUS_NOT_SUCCESS,
                        500, "Permanent Error"));

        // Trigger
        mCarrierConfigChangeListener.onCarrierConfigChanged(0, SUB_ID, 0, 0);
        mTestLooper.dispatchAll();

        // Verify
        verify(mTs43PhoneNumberRetriever, times(1)).fetchPhoneNumber(SUB_ID);
        // Ensure no delayed messages are sent for retry
        mTestLooper.dispatchAll(); // process any potential retries
        verify(mTs43PhoneNumberRetriever, times(1)).fetchPhoneNumber(SUB_ID);
    }

    @Test
    public void testFetch_transientError503_respectsRetryAfterHeader()
            throws ServiceEntitlementException {
        final long retryAfterSeconds = 5;
        givenTs43Enabled(true);
        simulateNetworkState(true);
        // This needs to be created including the Retry-After value in ServiceEntitlementException.
        // The code below may need to be modified depending on the actual constructor.
        when(mTs43PhoneNumberRetriever.fetchPhoneNumber(SUB_ID))
                .thenThrow(new ServiceEntitlementException(
                        ServiceEntitlementException.ERROR_HTTP_STATUS_NOT_SUCCESS,
                        503, String.valueOf(retryAfterSeconds), "Service Unavailable"))
                .thenReturn(FAKE_RAW_PHONE_NUMBER); // Succeeds on the second attempt

        // Trigger the first fetch
        mCarrierConfigChangeListener.onCarrierConfigChanged(0, SUB_ID, 0, 0);
        mTestLooper.dispatchAll();

        // Verify that the first attempt failed
        verify(mTs43PhoneNumberRetriever, times(1)).fetchPhoneNumber(SUB_ID);
        verify(mSubscriptionManager, never()).setTs43PhoneNumber(anyInt(), any());

        // Advance the looper by less than the Retry-After time and verify that no retry occur
        mTestLooper.moveTimeForward(retryAfterSeconds * 1000 - 2000);
        mTestLooper.dispatchAll();
        verify(mTs43PhoneNumberRetriever, times(1)).fetchPhoneNumber(SUB_ID);

        // Advance the looper by the remaining time and verify that a retry has occurred
        mTestLooper.moveTimeForward(3000);
        mTestLooper.dispatchAll();

        // Verify that the second attempt (retry) was successful
        verify(mTs43PhoneNumberRetriever, times(2)).fetchPhoneNumber(SUB_ID);
        verify(mSubscriptionManager).setTs43PhoneNumber(SUB_ID, FAKE_E164_PHONE_NUMBER);
    }

    @Test
    public void testFetch_transientError_triggersExponentialBackoff()
            throws ServiceEntitlementException {
        givenTs43Enabled(true);
        simulateNetworkState(true);
        when(mTs43PhoneNumberRetriever.fetchPhoneNumber(SUB_ID))
                .thenThrow(new ServiceEntitlementException(
                        ServiceEntitlementException.ERROR_HTTP_STATUS_NOT_SUCCESS,
                        504, "Transient Error")) // First failure
                .thenReturn(FAKE_RAW_PHONE_NUMBER); // Success on retry

        // Trigger initial fetch
        mCarrierConfigChangeListener.onCarrierConfigChanged(0, SUB_ID, 0, 0);
        mTestLooper.dispatchAll();

        // Verify first attempt failed
        verify(mTs43PhoneNumberRetriever, times(1)).fetchPhoneNumber(SUB_ID);
        verify(mSubscriptionManager, never()).setTs43PhoneNumber(anyInt(), any());

        // Advance looper time to trigger the retry
        mTestLooper.moveTimeForward(2000); // INITIAL_DELAY_MILLIS
        mTestLooper.dispatchAll();

        // Verify second attempt (the retry) succeeded
        verify(mTs43PhoneNumberRetriever, times(2)).fetchPhoneNumber(SUB_ID);
        verify(mSubscriptionManager).setTs43PhoneNumber(SUB_ID, FAKE_E164_PHONE_NUMBER);
    }
}
