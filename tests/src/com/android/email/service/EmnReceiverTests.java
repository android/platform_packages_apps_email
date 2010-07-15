/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.email;

import com.android.email.service.EmnReceiver;
import junit.framework.TestCase;
import java.util.Vector;

public class EmnReceiverTests extends TestCase {

    // test data
    protected static final String EMAIL_FIRST =
        "030D6A00850703656D61696C40616464726573732E636F6D0005C30620100804084401";
    protected static final String TIMESTAMP_FIRST =
        "030d6a008505c3062010080409560703656d61696c40616464726573732e636f6d0001";
    protected static final String EMAIL_FIRST_DOT_COM_ENCODED =
        "030D6A00850703656D61696C4061646472657373008505C30620100804084401";
    protected static final String INVALID_EMN_TAG =
        "030D6A00360703656D61696C40616464726573732E636F6D0005C30620100804084401";
    protected static final String MISSING_INLINE_STRING_TAG =
        "030D6A00850785656D61696C40616464726573732E636F6D0005C30620100804084401";
    protected static final String TRUNCATED_AFTER_INLINE_STRING =
        "030D6A00850703";
    protected static final String MISSING_NULL =
        "030D6A00850703656D61696C40616464726573732E636F6D05C30620100804084401";

    public void testDecode() {

        EmnReceiver emn = new EmnReceiver();

        assertEquals("email@address.com", emn.decode(fromHexString(EMAIL_FIRST)));
        assertEquals("email@address.com", emn.decode(fromHexString(TIMESTAMP_FIRST)));
        assertEquals("email@address.com", emn.decode(fromHexString(EMAIL_FIRST_DOT_COM_ENCODED)));
        assertEquals(null, emn.decode(fromHexString(INVALID_EMN_TAG)));
        assertEquals(null, emn.decode(fromHexString(MISSING_INLINE_STRING_TAG)));
        assertEquals(null, emn.decode(fromHexString(TRUNCATED_AFTER_INLINE_STRING)));
        assertEquals(null, emn.decode(fromHexString(MISSING_NULL)));

    }


    private static byte[] fromHexString(final String encoded) {

        final byte result[] = new byte[encoded.length()/2];
        final char enc[] = encoded.toCharArray();

        for (int i = 0; i < enc.length; i += 2) {
            StringBuilder curr = new StringBuilder(2);
            curr.append(enc[i]).append(enc[i + 1]);
            result[i/2] = (byte) Integer.parseInt(curr.toString(), 16);
        }
        return result;
    }
}