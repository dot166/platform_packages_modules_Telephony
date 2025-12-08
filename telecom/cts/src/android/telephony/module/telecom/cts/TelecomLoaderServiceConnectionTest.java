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

package android.telephony.module.telecom.cts;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import android.content.ComponentName;
import android.os.Binder;
import android.os.IBinder;
import android.telecom.TelecomLoaderServiceConnection;

import androidx.annotation.NonNull;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class TelecomLoaderServiceConnectionTest {

    private static final IBinder TEST_BINDER = new Binder();

    public static class TestConnection extends TelecomLoaderServiceConnection {
        public TelecomLoader connectedLoader = null;

        @Override
        public void onConnected(@NonNull TelecomLoader connection) {
            connectedLoader = connection;
        }
        @Override
        public void onDisconnected() {
            connectedLoader = null;
        }
    }

    public static class TestLoader implements TelecomLoaderServiceConnection.TelecomLoader {
        @Override
        public IBinder createTelecomService(@NonNull String sysUiPackageName) {
            return TEST_BINDER;
        }
    }

    private final TestLoader mTestLoader = new TestLoader();
    private final TestConnection mTestConnection = new TestConnection();

    @Test
    public void testNullOperation() {
        mTestConnection.onServiceConnected(new ComponentName("com.android.cts", ".Test"), null);
        assertNull(mTestConnection.connectedLoader);
    }

    @Test
    public void testOperation() {
        mTestConnection.onConnected(mTestLoader);
        assertEquals(mTestLoader, mTestConnection.connectedLoader);
        mTestConnection.onDisconnected();
        assertNull(mTestConnection.connectedLoader);
    }
}
