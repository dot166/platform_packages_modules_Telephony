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
package android.telephony;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import android.os.Parcel;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ParsedPhoneNumberTest {

    // Test data
    private static final String VALID_PHONE_NUMBER = "+1-555-123-4567";
    private static final String ANOTHER_VALID_PHONE_NUMBER = "+44-20-7946-0958";
    private static final String INVALID_PHONE_NUMBER_EMPTY = "";

    @Test
    public void constructorAndGetters_validNumber() {
        ParsedPhoneNumber parsedPhoneNumber =
                new ParsedPhoneNumber(VALID_PHONE_NUMBER, ParsedPhoneNumber.ERROR_TYPE_NONE, true);

        assertEquals(VALID_PHONE_NUMBER, parsedPhoneNumber.getParsedPhoneNumber());
        assertTrue(parsedPhoneNumber.isValidPhoneNumber());
        assertEquals(ParsedPhoneNumber.ERROR_TYPE_NONE, parsedPhoneNumber.getErrorCode());
    }

    @Test
    public void constructorAndGetters_invalidNumber_unknownError() {
        ParsedPhoneNumber parsedPhoneNumber =
                new ParsedPhoneNumber(INVALID_PHONE_NUMBER_EMPTY,
                    ParsedPhoneNumber.ERROR_TYPE_UNKNOWN, false);

        assertFalse(parsedPhoneNumber.isValidPhoneNumber());
        assertEquals(ParsedPhoneNumber.ERROR_TYPE_UNKNOWN, parsedPhoneNumber.getErrorCode());
        assertThrows(IllegalStateException.class, () -> parsedPhoneNumber.getParsedPhoneNumber());
    }

    @Test
    public void constructorAndGetters_invalidNumber_failedToValidateError() {
        ParsedPhoneNumber parsedPhoneNumber =
                new ParsedPhoneNumber(INVALID_PHONE_NUMBER_EMPTY,
                    ParsedPhoneNumber.ERROR_TYPE_FAILED_TO_VALIDATE_EXTRACTED_PHONE_NUMER, false);

        assertFalse(parsedPhoneNumber.isValidPhoneNumber());
        assertEquals(ParsedPhoneNumber.ERROR_TYPE_FAILED_TO_VALIDATE_EXTRACTED_PHONE_NUMER,
                parsedPhoneNumber.getErrorCode());
        assertThrows(IllegalStateException.class, () -> parsedPhoneNumber.getParsedPhoneNumber());
    }

    @Test
    public void constructorAndGetters_invalidNumber_parseExceptionError() {
        ParsedPhoneNumber parsedPhoneNumber =
                new ParsedPhoneNumber(INVALID_PHONE_NUMBER_EMPTY,
                    ParsedPhoneNumber.ERROR_TYPE_NUMBER_PARSE_EXCEPTION, false);

        assertFalse(parsedPhoneNumber.isValidPhoneNumber());
        assertEquals(ParsedPhoneNumber.ERROR_TYPE_NUMBER_PARSE_EXCEPTION,
                parsedPhoneNumber.getErrorCode());
        assertThrows(IllegalStateException.class, () -> parsedPhoneNumber.getParsedPhoneNumber());
    }

    @Test
    public void equals_sameObject_returnsTrue() {
        ParsedPhoneNumber p1 = new ParsedPhoneNumber(VALID_PHONE_NUMBER,
                ParsedPhoneNumber.ERROR_TYPE_NONE, true);

        assertTrue(p1.equals(p1));
    }

    @Test
    public void equals_identicalObjects_returnsTrue() {
        ParsedPhoneNumber p1 = new ParsedPhoneNumber(VALID_PHONE_NUMBER,
                ParsedPhoneNumber.ERROR_TYPE_NONE, true);
        ParsedPhoneNumber p2 = new ParsedPhoneNumber(VALID_PHONE_NUMBER,
                ParsedPhoneNumber.ERROR_TYPE_NONE, true);

        assertTrue(p1.equals(p2));
        assertEquals(p1.hashCode(), p2.hashCode());
    }

    @Test
    public void equals_differentPhoneNumber_returnsFalse() {
        ParsedPhoneNumber p1 = new ParsedPhoneNumber(VALID_PHONE_NUMBER,
                ParsedPhoneNumber.ERROR_TYPE_NONE, true);
        ParsedPhoneNumber p2 = new ParsedPhoneNumber(ANOTHER_VALID_PHONE_NUMBER,
                ParsedPhoneNumber.ERROR_TYPE_NONE, true);

        assertFalse(p1.equals(p2));
        assertNotEquals(p1.hashCode(), p2.hashCode());
    }

    @Test
    public void equals_differentErrorCode_returnsFalse() {
        ParsedPhoneNumber p1 = new ParsedPhoneNumber(VALID_PHONE_NUMBER,
                ParsedPhoneNumber.ERROR_TYPE_NONE, true);
        ParsedPhoneNumber p2 = new ParsedPhoneNumber(VALID_PHONE_NUMBER,
                ParsedPhoneNumber.ERROR_TYPE_UNKNOWN, true);

        assertFalse(p1.equals(p2));
        assertNotEquals(p1.hashCode(), p2.hashCode());
    }

    @Test
    public void equals_differentValidity_returnsFalse() {
        ParsedPhoneNumber p1 = new ParsedPhoneNumber(VALID_PHONE_NUMBER,
                ParsedPhoneNumber.ERROR_TYPE_NONE, true);
        ParsedPhoneNumber p2 = new ParsedPhoneNumber(VALID_PHONE_NUMBER,
                ParsedPhoneNumber.ERROR_TYPE_NONE, false);

        assertFalse(p1.equals(p2));
        assertNotEquals(p1.hashCode(), p2.hashCode());
    }

    @Test
    public void equals_differentClass_returnsFalse() {
        ParsedPhoneNumber p1 = new ParsedPhoneNumber(VALID_PHONE_NUMBER,
                ParsedPhoneNumber.ERROR_TYPE_NONE, true);

        assertFalse(p1.equals(new Object()));
    }

    @Test
    public void equals_nullObject_returnsFalse() {
        ParsedPhoneNumber p1 = new ParsedPhoneNumber(VALID_PHONE_NUMBER,
                ParsedPhoneNumber.ERROR_TYPE_NONE, true);

        assertFalse(p1.equals(null));
    }

    @Test
    public void toString_validNumber() {
        ParsedPhoneNumber parsedPhoneNumber =
                new ParsedPhoneNumber(VALID_PHONE_NUMBER, ParsedPhoneNumber.ERROR_TYPE_NONE, true);
        String expectedAnonymized = "+X-XXX-XXX-XXXX";
        String expectedToString = "ParsedPhoneNumber{"
                + "mPhoneNumber=" + expectedAnonymized
                + ", mErrorCode=" + ParsedPhoneNumber.ERROR_TYPE_NONE
                + ", mIsValidNumber=" + true
                + '}';

        assertEquals(expectedToString, parsedPhoneNumber.toString());
    }

    @Test
    public void toString_invalidNumber_empty() {
        ParsedPhoneNumber parsedPhoneNumber =
                new ParsedPhoneNumber(INVALID_PHONE_NUMBER_EMPTY,
                    ParsedPhoneNumber.ERROR_TYPE_UNKNOWN, false);
        String expectedAnonymized = "";
        String expectedToString = "ParsedPhoneNumber{"
                + "mPhoneNumber=" + expectedAnonymized
                + ", mErrorCode=" + ParsedPhoneNumber.ERROR_TYPE_UNKNOWN
                + ", mIsValidNumber=" + false
                + '}';

        assertEquals(expectedToString, parsedPhoneNumber.toString());
    }

    @Test
    public void toString_invalidNumber_withSomeDigits() {
        ParsedPhoneNumber parsedPhoneNumber =
                new ParsedPhoneNumber("abc123def", ParsedPhoneNumber.ERROR_TYPE_UNKNOWN, false);
        String expectedAnonymized = "abcXXXdef";
        String expectedToString = "ParsedPhoneNumber{"
                + "mPhoneNumber=" + expectedAnonymized
                + ", mErrorCode=" + ParsedPhoneNumber.ERROR_TYPE_UNKNOWN
                + ", mIsValidNumber=" + false
                + '}';

        assertEquals(expectedToString, parsedPhoneNumber.toString());
    }

    @Test
    public void parcelable_validNumber() {
        ParsedPhoneNumber original =
                new ParsedPhoneNumber(VALID_PHONE_NUMBER, ParsedPhoneNumber.ERROR_TYPE_NONE, true);
        Parcel parcel = Parcel.obtain();
        original.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        ParsedPhoneNumber parceled = ParsedPhoneNumber.CREATOR.createFromParcel(parcel);

        assertNotNull(parceled);
        assertEquals(original, parceled);
        assertEquals(original.getParsedPhoneNumber(), parceled.getParsedPhoneNumber());
        assertEquals(original.isValidPhoneNumber(), parceled.isValidPhoneNumber());
        assertEquals(original.getErrorCode(), parceled.getErrorCode());
        parcel.recycle();
    }

    @Test
    public void parcelable_invalidNumber_withError() {
        ParsedPhoneNumber original =
                new ParsedPhoneNumber(INVALID_PHONE_NUMBER_EMPTY,
                    ParsedPhoneNumber.ERROR_TYPE_NUMBER_PARSE_EXCEPTION, false);
        Parcel parcel = Parcel.obtain();
        original.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        ParsedPhoneNumber parceled = ParsedPhoneNumber.CREATOR.createFromParcel(parcel);

        assertNotNull(parceled);
        assertEquals(original, parceled);
        assertFalse(parceled.isValidPhoneNumber());
        assertEquals(ParsedPhoneNumber.ERROR_TYPE_NUMBER_PARSE_EXCEPTION, parceled.getErrorCode());
        assertThrows(IllegalStateException.class, () -> parceled.getParsedPhoneNumber());
        parcel.recycle();
    }

    @Test
    public void describeContents_returnsZero() {
        ParsedPhoneNumber parsedPhoneNumber =
                new ParsedPhoneNumber(VALID_PHONE_NUMBER, ParsedPhoneNumber.ERROR_TYPE_NONE, true);

        assertEquals(0, parsedPhoneNumber.describeContents());
    }

    @Test
    public void newArray_returnsCorrectSizeArray() {
        ParsedPhoneNumber[] array = ParsedPhoneNumber.CREATOR.newArray(5);

        assertNotNull(array);
        assertEquals(5, array.length);
    }
}
