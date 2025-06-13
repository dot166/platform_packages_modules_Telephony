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
import static org.junit.Assert.fail;

import android.telephony.ParsedPhoneNumber;

import org.junit.Test;

public class ParsedPhoneNumberCtsTest {

    @Test
    public void testSettingCorrectResultReturnsExceptedValues() throws Exception {
        ParsedPhoneNumber result =
                new ParsedPhoneNumber("+41446681801", ParsedPhoneNumber.ERROR_TYPE_NONE, true);

        assertEquals(true, result.isValidPhoneNumber());
        assertEquals("+41446681801", result.getParsedPhoneNumber());
        assertEquals(0, result.getErrorCode());
    }

    @Test
    public void testSettingUnknownResultReturnsExceptedValues() throws Exception {
        ParsedPhoneNumber result =
                new ParsedPhoneNumber("", ParsedPhoneNumber.ERROR_TYPE_UNKNOWN, false);

        assertEquals(false, result.isValidPhoneNumber());
        try {
            result.getParsedPhoneNumber();
            fail();
        } catch (IllegalStateException e) {
            // pass
        }
        assertEquals(-1, result.getErrorCode());
    }

    @Test
    public void testSettingValidationIssueResultReturnsExceptedValues() throws Exception {
        ParsedPhoneNumber result =
                new ParsedPhoneNumber(
                    "", ParsedPhoneNumber.ERROR_TYPE_FAILED_TO_VALIDATE_EXTRACTED_PHONE_NUMER,
                        false);

        assertEquals(false, result.isValidPhoneNumber());
        try {
            result.getParsedPhoneNumber();
            fail();
        } catch (IllegalStateException e) {
            // pass
        }
        assertEquals(1, result.getErrorCode());
    }

    @Test
    public void testSettingNumberParseExceptionResultReturnsExceptedValues() throws Exception {
        ParsedPhoneNumber result =
                new ParsedPhoneNumber("", ParsedPhoneNumber.ERROR_TYPE_NUMBER_PARSE_EXCEPTION,
                        false);

        assertEquals(false, result.isValidPhoneNumber());
        try {
            result.getParsedPhoneNumber();
            fail();
        } catch (IllegalStateException e) {
            // pass
        }
        assertEquals(2, result.getErrorCode());
    }
}
