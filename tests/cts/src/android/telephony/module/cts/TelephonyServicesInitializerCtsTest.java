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
import static org.junit.Assert.fail;

import android.content.Context;
import android.platform.test.annotations.RequiresFlagsEnabled;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;
import android.telephony.TelephonyServicesInitializer;

import androidx.test.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Field;

@RunWith(AndroidJUnit4.class)
public class TelephonyServicesInitializerCtsTest {

    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    private Context mContext;

    @Before
    public void setUp() throws Exception {
        mContext = InstrumentationRegistry.getInstrumentation().getContext();
    }

    @Test
    @RequiresFlagsEnabled("com.android.telephony.flags.support_get_phone_number_ts43")
    public void testInitializeWithContext_initializesService() {
        try {
            // 1. Call the new initialize method.
            TelephonyServicesInitializer.initialize(mContext);

            // 2. Use reflection to check if the internal service is initialized.
            //    This approach, while depending on internal implementation, is one
            //    of the effective ways to verify the initialization of a static method.
            Field serviceField = TelephonyServicesInitializer.class.getDeclaredField(
                    "sPhoneNumberManagerService");
            serviceField.setAccessible(true);
            Object serviceInstance = serviceField.get(null);

            // 3. Assert that the service instance is not null to verify successful initialization.
            assertNotNull("PhoneNumberManagerService should be initialized",
                    serviceInstance);

        } catch (NoSuchFieldException e) {
            fail("Could not find the internal service field. The implementation may have changed. "
                    + e);
        } catch (Exception e) {
            fail("An unexpected exception was thrown during the test: " + e);
        }
    }
}
