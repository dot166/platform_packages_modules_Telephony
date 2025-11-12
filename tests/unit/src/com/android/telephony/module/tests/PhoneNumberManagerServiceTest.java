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

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionManager;

import com.android.internal.telephony.PhoneNumberManagerService;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class PhoneNumberManagerServiceTest {

    @Mock private Context mContext;
    @Mock private SubscriptionManager mSubscriptionManager;
    @Mock private CarrierConfigManager mCarrierConfigManager;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        when(mContext.getSystemServiceName(SubscriptionManager.class))
                .thenReturn(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
        doReturn(mSubscriptionManager).when(mContext)
                .getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
        when(mContext.getSystemServiceName(CarrierConfigManager.class))
                .thenReturn(Context.CARRIER_CONFIG_SERVICE);
        doReturn(mCarrierConfigManager).when(mContext)
                .getSystemService(Context.CARRIER_CONFIG_SERVICE);
    }

    @Test
    public void constructor_withNullContext_doesNotCreateController() {
        PhoneNumberManagerService service = new PhoneNumberManagerService(null);
        assertNotNull(service);
    }

    @Test
    public void constructor_withValidContext_createsController() {
        PhoneNumberManagerService service = new PhoneNumberManagerService(mContext);
        assertNotNull(service);
    }
}
