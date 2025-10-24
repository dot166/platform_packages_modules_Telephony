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

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.os.test.TestLooper;

import com.android.internal.telephony.ExponentialBackoffRetry;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class ExponentialBackoffRetryTest {

    @Mock private Runnable mRetryRunnable;
    private TestLooper mTestLooper;

    private ExponentialBackoffRetry mBackoff;

    private static final long INITIAL_DELAY_MS = 1000;
    private static final long MAX_DELAY_MS = 8000;
    private static final int MULTIPLIER = 2;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mTestLooper = new TestLooper();
        mBackoff = new ExponentialBackoffRetry(
                INITIAL_DELAY_MS, MAX_DELAY_MS, MULTIPLIER, mTestLooper.getLooper(),
                mRetryRunnable);
    }

    @Test
    public void start_schedulesFirstAttempt() {
        mBackoff.start();
        // start() schedules the first attempt after INITIAL_DELAY_MS, so we advance the time.
        mTestLooper.moveTimeForward(INITIAL_DELAY_MS);
        mTestLooper.dispatchAll();

        verify(mRetryRunnable, times(1)).run();
    }

    @Test
    public void notifyFailed_increasesDelay() {
        mBackoff.start();
        mTestLooper.moveTimeForward(INITIAL_DELAY_MS);
        mTestLooper.dispatchAll(); // First run
        verify(mRetryRunnable, times(1)).run();

        // First failure notification -> schedules a retry after a calculated delay.
        mBackoff.notifyFailed();
        long firstRetryMaxDelay = (long) (INITIAL_DELAY_MS * Math.pow(MULTIPLIER, 1));
        mTestLooper.moveTimeForward(firstRetryMaxDelay);
        mTestLooper.dispatchAll();
        verify(mRetryRunnable, times(2)).run(); // Second run (first retry)

        // Second failure notification
        mBackoff.notifyFailed();
        long secondRetryMaxDelay = (long) (INITIAL_DELAY_MS * Math.pow(MULTIPLIER, 2));
        mTestLooper.moveTimeForward(secondRetryMaxDelay);
        mTestLooper.dispatchAll();
        verify(mRetryRunnable, times(3)).run(); // Third run (second retry)
    }

    @Test
    public void delay_doesNotExceedMaxDelay() {
        mBackoff.start();
        mTestLooper.moveTimeForward(INITIAL_DELAY_MS);
        mTestLooper.dispatchAll(); // 1st run
        verify(mRetryRunnable, times(1)).run();

        // Subsequent failures and retries
        mBackoff.notifyFailed(); // schedules for ~2s
        mTestLooper.moveTimeForward((long) (INITIAL_DELAY_MS * Math.pow(MULTIPLIER, 1)));
        mTestLooper.dispatchAll();
        verify(mRetryRunnable, times(2)).run();

        mBackoff.notifyFailed(); // schedules for ~4s
        mTestLooper.moveTimeForward((long) (INITIAL_DELAY_MS * Math.pow(MULTIPLIER, 2)));
        mTestLooper.dispatchAll();
        verify(mRetryRunnable, times(3)).run();

        mBackoff.notifyFailed(); // schedules for ~8s (max)
        mTestLooper.moveTimeForward((long) (INITIAL_DELAY_MS * Math.pow(MULTIPLIER, 3)));
        mTestLooper.dispatchAll();
        verify(mRetryRunnable, times(4)).run();

        mBackoff.notifyFailed(); // schedules for ~8s (still max)
        mTestLooper.moveTimeForward(MAX_DELAY_MS);
        mTestLooper.dispatchAll();
        verify(mRetryRunnable, times(5)).run();
    }

    @Test
    public void stop_cancelsPendingRetries() {
        mBackoff.start(); // Schedule a run.

        mBackoff.stop(); // Cancel the scheduled run.

        // Time is advanced, but the runnable should not be executed.
        mTestLooper.moveTimeForward(INITIAL_DELAY_MS * 10);
        mTestLooper.dispatchAll();

        verify(mRetryRunnable, never()).run();
    }
}
