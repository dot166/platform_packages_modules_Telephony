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
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.platform.test.annotations.RequiresFlagsEnabled;
import android.telephony.ParsedPhoneNumber;
import android.telephony.PhoneNumberManager;

import androidx.test.filters.SmallTest;
import androidx.test.platform.app.InstrumentationRegistry;

import com.android.telephony.flags.Flags;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


@SmallTest
public class PhoneNumberManagerTest {
    private PhoneNumberManager mPhoneNumberManager;
    protected static Context sContext;

    @BeforeClass
    public static void setStaticFixtures() {
        if (sContext != null) {
            return;
        }
        try {
            sContext = InstrumentationRegistry.getInstrumentation().getContext();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Before
    public void setup() throws Exception {
        mPhoneNumberManager = sContext.getSystemService(PhoneNumberManager.class);
        PackageManager packageManager = sContext.getPackageManager();
        assumeTrue(packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY));
        assumeTrue(mPhoneNumberManager != null);
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_PHONE_NUMBER_PARSING_API)
    public void testParsingEmptyParametersExceptionThrownWhenFlagEnabled() throws Exception {
        try {
            mPhoneNumberManager.parsePhoneNumber(new ArrayList<Uri>(), "");
            fail("Expected failure due to an invalid argument provided.");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_PHONE_NUMBER_PARSING_API)
    public void testParsingNullParametersExceptionThrownWhenFlagEnabled() throws Exception {
        try {
            mPhoneNumberManager.parsePhoneNumber(null, null);
            fail("Expected failure due to an invalid argument provided.");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_PHONE_NUMBER_PARSING_API)
    public void testParsingGlobalNumberWhenGlobalIsExpectedReturnSuccessWhenFlagEnabled() {
        ParsedPhoneNumber result =
                mPhoneNumberManager.parsePhoneNumber(
                    new ArrayList<Uri>(
                        Arrays.asList(
                            Uri.parse("tel:+41446681800"),
                            Uri.parse("sip:+41446681800@ims.mnc260.mcc310.3gppnetwork.org"))),
                    "CH");

        assertEquals(true, result.isValidPhoneNumber());
        assertEquals("+41446681800", result.getParsedPhoneNumber());
        assertEquals(0, result.getErrorCode());
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_PHONE_NUMBER_PARSING_API)
    public void testParsingNationalNumberWhenNationalReturnSuccessWhenFlagEnabled() {
        ParsedPhoneNumber result =
                mPhoneNumberManager.parsePhoneNumber(
                    new ArrayList<Uri>(
                        Arrays.asList(
                            Uri.parse("tel:0446681801"),
                            Uri.parse("sip:0446681801@ims.mnc260.mcc310.3gppnetwork.org"))),
                    "CH");

        assertEquals(true, result.isValidPhoneNumber());
        assertEquals("+41446681801", result.getParsedPhoneNumber());
        assertEquals(0, result.getErrorCode());
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_PHONE_NUMBER_PARSING_API)
    public void testParsingNumberWhenCountryIsoIsInvalidReturnErrorWhenFlagEnabled() {
        ParsedPhoneNumber result =
                mPhoneNumberManager.parsePhoneNumber(
                    new ArrayList<Uri>(
                        Arrays.asList(
                            // Guatemalan national format number
                            Uri.parse("tel:555-1234"),
                            Uri.parse("sip:555-1234@ims.mnc260.mcc310.3gppnetwork.org"))),
                    "CH");

        assertEquals(false, result.isValidPhoneNumber());
        try {
            result.getParsedPhoneNumber();
            fail();
        } catch (IllegalStateException expected) {
            // expected
        }
        assertEquals(1, result.getErrorCode());
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_PHONE_NUMBER_PARSING_API)
    public void testParsingNumberWithInvalidFormatReturnErrorWhenFlagEnabled() {
        ParsedPhoneNumber result =
                mPhoneNumberManager.parsePhoneNumber(
                    new ArrayList<Uri>(
                        Arrays.asList(
                            // Invalid number format
                            Uri.parse("tel:123456789"),
                            Uri.parse("sip:123456789@ims.mnc260.mcc310.3gppnetwork.org"))),
                    "CH");

        assertEquals(false, result.isValidPhoneNumber());
        try {
            result.getParsedPhoneNumber();
            fail();
        } catch (IllegalStateException expected) {
            // expected
        }
        assertEquals(1, result.getErrorCode());
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_PHONE_NUMBER_PARSING_API)
    public void testParsingPhoneDifferentImsNumberFormatReturnSuccessWhenFlagEnabled() {
        ParsedPhoneNumber result =
                mPhoneNumberManager.parsePhoneNumber(
                    new ArrayList<Uri>(
                        Arrays.asList(
                            Uri.parse("tel:0446681801"),
                            Uri.parse("sip:+41446681801@ims.mnc260.mcc310.3gppnetwork.org"))),
                    "CH");

        assertEquals(true, result.isValidPhoneNumber());
        assertEquals("+41446681801", result.getParsedPhoneNumber());
        assertEquals(0, result.getErrorCode());
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_PHONE_NUMBER_PARSING_API)
    public void testParsingNumberWithPlusAndNoCountryCodeReturnErrorWhenFlagEnabled() {
        ParsedPhoneNumber result =
                mPhoneNumberManager.parsePhoneNumber(
                    new ArrayList<Uri>(
                        Arrays.asList(
                            Uri.parse("tel:+446681801"),
                            Uri.parse("sip:+446681801@ims.mnc260.mcc310.3gppnetwork.org"))),
                    "CH");

        assertEquals(false, result.isValidPhoneNumber());
        try {
            result.getParsedPhoneNumber();
            fail();
        } catch (IllegalStateException expected) {
            // expected
        }
        assertEquals(1, result.getErrorCode());
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_PHONE_NUMBER_PARSING_API)
    public void testParsingNumberWhenCountryHasDoubleLeadingZerosReturnSuccessWhenFlagEnabled() {
        ParsedPhoneNumber result =
                mPhoneNumberManager.parsePhoneNumber(
                    new ArrayList<Uri>(
                        Arrays.asList(
                            Uri.parse("tel:0041446681801"),
                            Uri.parse("sip:0041446681801@ims.mnc260.mcc310.3gppnetwork.org"))),
                    "CH");

        assertEquals(true, result.isValidPhoneNumber());
        assertEquals("+41446681801", result.getParsedPhoneNumber());
        assertEquals(0, result.getErrorCode());
    }


    @Test
    @RequiresFlagsEnabled(Flags.FLAG_PHONE_NUMBER_PARSING_API)
    public void testParsingApiReturnsErrorWhenFlagDisabled() {
        ParsedPhoneNumber result =
                mPhoneNumberManager.parsePhoneNumber(
                    new ArrayList<Uri>(Arrays.asList(Uri.parse("tel:1234567890"))),
                    "US");

        assertEquals(false, result.isValidPhoneNumber());
        try {
            result.getParsedPhoneNumber();
            fail();
        } catch (IllegalStateException expected) {
            // expected
        }
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_PHONE_NUMBER_PARSING_API)
    public void testParsingNumberWhenCountryHasDifferentNumbersSelectsFirst() {
        ParsedPhoneNumber result =
                mPhoneNumberManager.parsePhoneNumber(
                    new ArrayList<Uri>(
                        Arrays.asList(
                            Uri.parse("sip:0041446681802@ims.mnc260.mcc310.3gppnetwork.org"),
                            Uri.parse("tel:0041446681801"))),
                    "CH");

        assertEquals(true, result.isValidPhoneNumber());
        assertEquals("+41446681802", result.getParsedPhoneNumber());
        assertEquals(0, result.getErrorCode());
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_PHONE_NUMBER_PARSING_API)
    public void testParsingNumberInDifferentFormats() {
        ParsedPhoneNumber result =
                mPhoneNumberManager.parsePhoneNumber(
                    new ArrayList<Uri>(
                        Arrays.asList(
                            Uri.parse("sip:16504958132@msg.pc.t-mobile.com"),
                            Uri.parse("sip:310260317432526@ims.mnc260.mcc310.3gppnetwork.org"),
                            Uri.parse("sip:+16504958132@ims.mnc260.mcc310.3gppnetwork.org"))),
                    "US");

        assertEquals(true, result.isValidPhoneNumber());
        assertEquals("+16504958132", result.getParsedPhoneNumber());
        assertEquals(0, result.getErrorCode());
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_PHONE_NUMBER_PARSING_API)
    public void testParsingNumberWhenCountryIsoLowercase() {
        ParsedPhoneNumber result =
                mPhoneNumberManager.parsePhoneNumber(
                    new ArrayList<Uri>(
                        Arrays.asList(
                            Uri.parse("sip:16504958132@msg.pc.t-mobile.com"),
                            Uri.parse("sip:310260317432526@ims.mnc260.mcc310.3gppnetwork.org"))),
                    "us");

        assertEquals(true, result.isValidPhoneNumber());
        assertEquals("+16504958132", result.getParsedPhoneNumber());
        assertEquals(0, result.getErrorCode());
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_PHONE_NUMBER_PARSING_API)
    public void testParsingWithNullUriInList() {
        List<Uri> uris = new ArrayList<>();
        uris.add(null);
        uris.add(Uri.parse("tel:+12125551234"));

        ParsedPhoneNumber result =
                mPhoneNumberManager.parsePhoneNumber(uris, "US");

        assertTrue(result.isValidPhoneNumber());
        assertEquals("+12125551234", result.getParsedPhoneNumber());
        assertEquals(ParsedPhoneNumber.ERROR_TYPE_NONE, result.getErrorCode());
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_PHONE_NUMBER_PARSING_API)
    public void testParsingUkGlobalNumberSuccess() {
        ParsedPhoneNumber result =
                mPhoneNumberManager.parsePhoneNumber(
                    Collections.singletonList(Uri.parse("tel:+442079460000")),
                    "GB");

        assertTrue(result.isValidPhoneNumber());
        assertEquals("+442079460000", result.getParsedPhoneNumber());
        assertEquals(ParsedPhoneNumber.ERROR_TYPE_NONE, result.getErrorCode());
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_PHONE_NUMBER_PARSING_API)
    public void testParsingUkNationalNumberSuccess() {
        ParsedPhoneNumber result =
                mPhoneNumberManager.parsePhoneNumber(
                    Collections.singletonList(Uri.parse("tel:02079460001")),
                    "GB");

        assertTrue(result.isValidPhoneNumber());
        assertEquals("+442079460001", result.getParsedPhoneNumber());
        assertEquals(ParsedPhoneNumber.ERROR_TYPE_NONE, result.getErrorCode());
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_PHONE_NUMBER_PARSING_API)
    public void testParsingUsNumberSuccess() {
        ParsedPhoneNumber result =
                mPhoneNumberManager.parsePhoneNumber(
                    Collections.singletonList(Uri.parse("tel:+12125550100")),
                    "US");

        assertTrue(result.isValidPhoneNumber());
        assertEquals("+12125550100", result.getParsedPhoneNumber());
        assertEquals(ParsedPhoneNumber.ERROR_TYPE_NONE, result.getErrorCode());
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_PHONE_NUMBER_PARSING_API)
    public void testParsingJapaneseNumberSuccess() {
        ParsedPhoneNumber result =
                mPhoneNumberManager.parsePhoneNumber(
                    Collections.singletonList(Uri.parse("tel:+81312345678")),
                    "JP");

        assertTrue(result.isValidPhoneNumber());
        assertEquals("+81312345678", result.getParsedPhoneNumber());
        assertEquals(ParsedPhoneNumber.ERROR_TYPE_NONE, result.getErrorCode());
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_PHONE_NUMBER_PARSING_API)
    public void testParsingTooShortNumberReturnError() {
        ParsedPhoneNumber result =
                mPhoneNumberManager.parsePhoneNumber(
                    Collections.singletonList(Uri.parse("tel:123")),
                    "US");

        assertEquals(false, result.isValidPhoneNumber());
        assertThrows(IllegalStateException.class, result::getParsedPhoneNumber);
        assertEquals(ParsedPhoneNumber.ERROR_TYPE_FAILED_TO_VALIDATE_EXTRACTED_PHONE_NUMER,
                result.getErrorCode());
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_PHONE_NUMBER_PARSING_API)
    public void testParsingTooLongNumberReturnError() {
        ParsedPhoneNumber result =
                mPhoneNumberManager.parsePhoneNumber(
                    // Too long for US
                    Collections.singletonList(Uri.parse("tel:+1212555010099999")),
                    "US");

        assertEquals(false, result.isValidPhoneNumber());
        assertThrows(IllegalStateException.class, result::getParsedPhoneNumber);
        assertEquals(ParsedPhoneNumber.ERROR_TYPE_FAILED_TO_VALIDATE_EXTRACTED_PHONE_NUMER,
                result.getErrorCode());
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_PHONE_NUMBER_PARSING_API)
    public void testParsingNonNumericCharactersReturnError() {
        ParsedPhoneNumber result =
                mPhoneNumberManager.parsePhoneNumber(
                    Collections.singletonList(Uri.parse("tel:abcde")),
                    "US");

        assertEquals(false, result.isValidPhoneNumber());
        assertThrows(IllegalStateException.class, result::getParsedPhoneNumber);
        assertEquals(ParsedPhoneNumber.ERROR_TYPE_NUMBER_PARSE_EXCEPTION, result.getErrorCode());
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_PHONE_NUMBER_PARSING_API)
    public void testParsingListWithInvalidThenValidUri() {
        List<Uri> uris = Arrays.asList(
                Uri.parse("invalid-scheme:123"), // Invalid URI scheme
                Uri.parse("tel:short"),          // Invalid number format
                Uri.parse("tel:+41446681800")    // Valid CH number
        );

        ParsedPhoneNumber result = mPhoneNumberManager.parsePhoneNumber(uris, "CH");

        assertTrue(result.isValidPhoneNumber());
        assertEquals("+41446681800", result.getParsedPhoneNumber());
        assertEquals(ParsedPhoneNumber.ERROR_TYPE_NONE, result.getErrorCode());
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_PHONE_NUMBER_PARSING_API)
    public void testParsingListWithValidThenInvalidUri() {
        List<Uri> uris = Arrays.asList(
                Uri.parse("tel:+41446681800"),   // Valid CH number
                Uri.parse("tel:short"),          // Invalid number format
                Uri.parse("invalid-scheme:123")  // Invalid URI scheme
        );

        ParsedPhoneNumber result = mPhoneNumberManager.parsePhoneNumber(uris, "CH");

        assertTrue(result.isValidPhoneNumber());
        assertEquals("+41446681800", result.getParsedPhoneNumber());
        assertEquals(ParsedPhoneNumber.ERROR_TYPE_NONE, result.getErrorCode());
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_PHONE_NUMBER_PARSING_API)
    public void testParsingListWithNoValidPhoneNumber() {
        List<Uri> uris = Arrays.asList(
                Uri.parse("tel:123"),
                Uri.parse("sip:invalid@domain.com"),
                Uri.parse("tel:verylongnumberthatisntvalidforcH1234567890"),
                Uri.parse("http://example.com") // Non-opaque URI
        );

        ParsedPhoneNumber result = mPhoneNumberManager.parsePhoneNumber(uris, "CH");

        assertEquals(false, result.isValidPhoneNumber());
        assertThrows(IllegalStateException.class, result::getParsedPhoneNumber);
        assertEquals(ParsedPhoneNumber.ERROR_TYPE_FAILED_TO_VALIDATE_EXTRACTED_PHONE_NUMER,
                result.getErrorCode());
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_PHONE_NUMBER_PARSING_API)
    public void testParsingTelUri() {
        ParsedPhoneNumber result =
                mPhoneNumberManager.parsePhoneNumber(
                    Collections.singletonList(Uri.parse("tel:+17862668501")),
                    "US");

        assertTrue(result.isValidPhoneNumber());
        assertEquals("+17862668501", result.getParsedPhoneNumber());
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_PHONE_NUMBER_PARSING_API)
    public void testParsingSipUri() {
        ParsedPhoneNumber result =
                mPhoneNumberManager.parsePhoneNumber(
                    Collections.singletonList(Uri.parse("sip:17862668501@example.com")),
                    "US");

        assertTrue(result.isValidPhoneNumber());
        assertEquals("+17862668501", result.getParsedPhoneNumber());
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_PHONE_NUMBER_PARSING_API)
    public void testParsingSipUriWithPlusSignInUserPart() {
        ParsedPhoneNumber result =
                mPhoneNumberManager.parsePhoneNumber(
                    Collections.singletonList(Uri.parse("sip:+12125550100@ims.example.com")),
                    "US");

        assertTrue(result.isValidPhoneNumber());
        assertEquals("+12125550100", result.getParsedPhoneNumber());
    }


    @Test
    @RequiresFlagsEnabled(Flags.FLAG_PHONE_NUMBER_PARSING_API)
    public void testParsingWithAllNullUrisInList() {
        List<Uri> uris = Arrays.asList(null, null, null);
        ParsedPhoneNumber result =
                mPhoneNumberManager.parsePhoneNumber(uris, "US");

        assertEquals(false, result.isValidPhoneNumber());
        assertThrows(IllegalStateException.class, result::getParsedPhoneNumber);
        // TODO(b/434607712) - Update with new value to catch null or non opaque uri
        assertEquals(ParsedPhoneNumber.ERROR_TYPE_NONE, result.getErrorCode());
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_PHONE_NUMBER_PARSING_API)
    public void testParsingNonOpaqueUriReturnsError() {
        // HTTP URI is not opaque and should return an error.
        ParsedPhoneNumber result =
                mPhoneNumberManager.parsePhoneNumber(
                    Collections.singletonList(Uri.parse("http://www.google.com/search?q=12345")),
                    "US");

        assertEquals(false, result.isValidPhoneNumber());
        assertThrows(IllegalStateException.class, result::getParsedPhoneNumber);
        // TODO(b/434607712) - Update with new value to catch null or non opaque uri
        assertEquals(ParsedPhoneNumber.ERROR_TYPE_NONE, result.getErrorCode());
    }
}
