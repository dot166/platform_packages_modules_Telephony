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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.PersistableBundle;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;

import com.android.internal.telephony.Ts43PhoneNumberRetriever;
import com.android.internal.telephony.com.android.libraries.entitlement.ServiceEntitlementException;
import com.android.internal.telephony.com.android.libraries.entitlement.Ts43Authentication;
import com.android.internal.telephony.com.android.libraries.entitlement.Ts43Operation;
import com.android.internal.telephony.com.android.libraries.entitlement.odsa
        .GetPhoneNumberOperation.GetPhoneNumberRequest;
import com.android.internal.telephony.com.android.libraries.entitlement.odsa
        .GetPhoneNumberOperation.GetPhoneNumberResponse;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.net.URL;

@RunWith(MockitoJUnitRunner.class)
public class Ts43PhoneNumberRetrieverTest {

    private static final int FAKE_SUB_ID = 1;
    private static final int FAKE_SLOT_INDEX = 0;
    private static final String FAKE_SERVER_URL = "https://fake.entitlement-server.com";
    private static final String FAKE_PHONE_NUMBER = "+1234567890";
    private static final String FAKE_TOKEN = "fake-auth-token";

    // Mocks for Android framework and external library classes required for the test.
    @Mock
    private Context mContext;
    @Mock
    private SubscriptionManager mSubscriptionManager;
    @Mock
    private CarrierConfigManager mCarrierConfigManager;
    private PersistableBundle mCarrierConfig;
    @Mock
    private SubscriptionInfo mSubscriptionInfo;
    @Mock
    private PackageManager mPackageManager;
    @Mock
    private GetPhoneNumberResponse mGetPhoneNumberResponse;

    @Mock
    private Ts43PhoneNumberRetriever.Ts43Factory mMockTs43Factory;
    @Mock
    private Ts43Authentication mMockTs43Auth;
    @Mock
    private Ts43Operation mMockTs43Operation;
    private Ts43PhoneNumberRetriever mRetriever;

    @Before
    public void setUp() throws Exception {
        mCarrierConfig = new PersistableBundle();
        // Setup default behavior for the mock objects.
        when(mContext.getSystemServiceName(SubscriptionManager.class))
                .thenReturn(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
        doReturn(mSubscriptionManager).when(mContext)
                .getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
        when(mContext.getSystemServiceName(CarrierConfigManager.class))
                .thenReturn(Context.CARRIER_CONFIG_SERVICE);
        doReturn(mCarrierConfigManager).when(mContext)
                .getSystemService(Context.CARRIER_CONFIG_SERVICE);
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        when(mContext.getPackageName()).thenReturn("com.android.phone");
        // Default setup for versionName retrieval.
        PackageInfo info = new PackageInfo();
        info.versionName = "1.0";
        when(mPackageManager.getPackageInfo(anyString(), anyInt())).thenReturn(info);

        when(mCarrierConfigManager.getConfigForSubId(FAKE_SUB_ID,
                CarrierConfigManager.ImsServiceEntitlement.KEY_ENTITLEMENT_SERVER_URL_STRING))
                .thenReturn(mCarrierConfig);
        when(mSubscriptionManager.getActiveSubscriptionInfo(FAKE_SUB_ID))
                .thenReturn(mSubscriptionInfo);
        when(mSubscriptionInfo.getSimSlotIndex()).thenReturn(FAKE_SLOT_INDEX);

        mRetriever = new Ts43PhoneNumberRetriever(mContext, mMockTs43Factory);

        // Configure the mock factory to return our other mock objects.
        when(mMockTs43Factory.createTs43Authentication(any(Context.class), any(URL.class)))
                .thenReturn(mMockTs43Auth);
        when(mMockTs43Factory.createTs43Operation(any(Context.class), any(URL.class),
                anyInt(), anyString())).thenReturn(mMockTs43Operation);
    }

    /**
     * Tests the success scenario where the phone number is retrieved successfully
     * through all steps.
     */
    @Test
    public void testFetchPhoneNumber_Success() throws Exception {
        android.util.Log.d("TS43PhoneNumberTest", "ts43Authenticator " + mMockTs43Auth);
        // Given: Server URL is available.
        mCarrierConfig.putString(CarrierConfigManager.ImsServiceEntitlement
                .KEY_ENTITLEMENT_SERVER_URL_STRING, FAKE_SERVER_URL);

        // Given: The authentication mock will return a valid token.
        Ts43Authentication.Ts43AuthToken mockAuthToken =
                mock(Ts43Authentication.Ts43AuthToken.class);
        android.util.Log.d("TS43PhoneNumberTest", "mockAuthToken " + mockAuthToken);
        when(mockAuthToken.token()).thenReturn(FAKE_TOKEN);
        when(mMockTs43Auth.getAuthToken(anyInt(), anyString(),
                any(), any(), anyString())).thenReturn(mockAuthToken);

        // Given: The operation mock will return a valid phone number response.
        when(mGetPhoneNumberResponse.msisdn()).thenReturn(FAKE_PHONE_NUMBER);
        when(mMockTs43Operation.getPhoneNumber(any(GetPhoneNumberRequest.class)))
                .thenReturn(mGetPhoneNumberResponse);

        // When: Call the target method.
        String phoneNumber = mRetriever.fetchPhoneNumber(FAKE_SUB_ID);

        // Then: Assert the correct phone number is returned.
        assertEquals(FAKE_PHONE_NUMBER, phoneNumber);
    }

    /**
     * Tests the failure scenario where the server URL is not available from CarrierConfig.
     */
    @Test
    public void testFetchPhoneNumber_Failure_NoServerUrl() throws Exception {
        // Given: The server URL is not in the config.

        // When
        String phoneNumber = mRetriever.fetchPhoneNumber(FAKE_SUB_ID);

        // Then
        assertNull(phoneNumber);
    }

    /**
     * Tests the failure scenario where obtaining the authentication token fails
     */
    @Test
    public void testFetchPhoneNumber_Failure_NullAuthToken() throws Exception {
        // Given: The server URL is available.
        mCarrierConfig.putString(CarrierConfigManager.ImsServiceEntitlement
                .KEY_ENTITLEMENT_SERVER_URL_STRING, FAKE_SERVER_URL);

        // Given: The authentication step will fail by returning a null token.
        when(mMockTs43Auth.getAuthToken(anyInt(), any(), any(), any(),
                anyString())).thenReturn(null);

        // When: Call the target method.
        String phoneNumber = mRetriever.fetchPhoneNumber(FAKE_SUB_ID);

        // Then: The method should return null.
        assertNull(phoneNumber);
    }

    /**
     * Tests the failure scenario where an exception is thrown during phone number retrieval.
     */
    @Test(expected = ServiceEntitlementException.class)
    public void testFetchPhoneNumber_Failure_GetPhoneNumberThrowsException() throws Exception {
        // Given: The server URL is available.
        mCarrierConfig.putString(CarrierConfigManager.ImsServiceEntitlement
                .KEY_ENTITLEMENT_SERVER_URL_STRING, FAKE_SERVER_URL);

        // Given: The authentication step succeeds.
        Ts43Authentication.Ts43AuthToken mockAuthToken =
                mock(Ts43Authentication.Ts43AuthToken.class);
        when(mockAuthToken.token()).thenReturn(FAKE_TOKEN);
        when(mMockTs43Auth.getAuthToken(anyInt(), any(), any(),
                any(), anyString())).thenReturn(mockAuthToken);

        // Given: The final phone number retrieval step throws an exception.
        when(mMockTs43Operation.getPhoneNumber(any(GetPhoneNumberRequest.class)))
                .thenThrow(new ServiceEntitlementException(ServiceEntitlementException
                        .ERROR_UNKNOWN, "Network Error"));

        // When: Call the target method.
        String phoneNumber = mRetriever.fetchPhoneNumber(FAKE_SUB_ID);

        // Then: The exception should be caught internally, and the method should return null.
        assertNull(phoneNumber);
    }
}
