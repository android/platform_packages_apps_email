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

package com.android.email.service;

import com.android.email.Email;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;
import static android.provider.Telephony.Sms.Intents.WAP_PUSH_RECEIVED_ACTION;

/**
 * Receives WAP broadcast messages to respond to EMN push notifications.
 */

public class EmnReceiver extends BroadcastReceiver {

    private static final String CONTENT_MIME_TYPE_B_EMN =
            "application/vnd.wap.emn+wbxml";

    // for emn spec see http://www.openmobilealliance.org/technical/release_program/
    //                  docs/emailnot/v1_0-20021031-c/oma-push-emn-v1_0-20020830-c.pdf
    private static final byte WBXML_INLINE_STRING = 0x03;
    private static final byte WBXML_TOKEN_MASK = 0x3F;
    private static final byte WBXML_TOKEN_WITH_ATTRS = (byte) 0x80;
    private static final byte WBXML_TAG_EMN = 0x05;
    private static final byte WBXML_ATTR_TIMESTAMP = 0x05;
    private static final byte WBXML_ATTR_MAILBOX = 0x06;
    private static final byte WBXML_ATTR_MAILBOX_AT = 0x07;
    private static final byte WBXML_ATTR_OPAQUE_DATA = (byte) 0xC3;
    private static final byte WBXML_ATTR_COM = (byte) 0x85;
    private static final byte WBXML_ATTR_EDU = (byte) 0x86;
    private static final byte WBXML_ATTR_NET = (byte) 0x87;
    private static final byte WBXML_ATTR_ORG = (byte) 0x88;
    private static final byte WBXML_ATTR_NULL = 0x00;

    @Override
    public void onReceive(Context context, Intent intent) {

        if (intent.getAction().equals(WAP_PUSH_RECEIVED_ACTION)) {
            // get the right mime type
            String mimeType = intent.getType();
            if (mimeType.equals(CONTENT_MIME_TYPE_B_EMN)) {
                // get the WBXML encoded data
                byte[] wbxml = intent.getByteArrayExtra("data");
                if (wbxml == null) {
                    return;
                }

                // decode wbxml and get the email address
                String emailAddress = decode(wbxml);

                if (!TextUtils.isEmpty(emailAddress)) {

                    if (Email.DEBUG) {
                        Log.d(Email.LOG_TAG, "CONTENT_MIME_TYPE_B_EMN " +
                                "received for email " + emailAddress);
                    }

                    MailService.actionCheckMail(context, emailAddress);
                }
            }
        }
    }

    /**
     * Decodes a wbxml EMN message and returns the email address
     */
    public String decode(byte[] wbxmlBody) {

        try {

            // the decoded wap in xml looks like
            // <emn mailbox="mailat:bob@hotmail.com"
            // timestamp="2008-06-30T15:17:00Z"/>

            // skip 4 to skip over header (version, publicID, charset,
            // string table length)
            int i = 4;

            // expect <emn TAG with attributes
            if ((wbxmlBody[i] & WBXML_TOKEN_MASK) != WBXML_TAG_EMN) {
                return null;
            }
            if ((wbxmlBody[i] & WBXML_TOKEN_WITH_ATTRS) != WBXML_TOKEN_WITH_ATTRS) {
                return null;
            }

            // if its time stamp skip it
            // maybe the time stamp was put before the mailbox
            i++;
            if ((wbxmlBody[i] & WBXML_TOKEN_MASK) == WBXML_ATTR_TIMESTAMP) {
                i++;
                // expect opaque data
                if (wbxmlBody[i]  == WBXML_ATTR_OPAQUE_DATA) {
                    i++;
                    // next up is the opaque's data length which we want to skip
                    i += wbxmlBody[i] + 1;
                }
            }

            // only accept mailbox="xxxx@xxxx.xxx" or
            // mailbox="mailat:xxxx@xxxx.xxx"
            // URI support is optional and other URI have security risks
            // expect mailbox ="  or mailbox="mailat:
            if ((wbxmlBody[i] & WBXML_TOKEN_MASK) == WBXML_ATTR_MAILBOX &&
                    (wbxmlBody[i] & WBXML_TOKEN_MASK) == WBXML_ATTR_MAILBOX_AT) {
                return null;
            }

            // expect in line string which is the email address
            if (wbxmlBody[i + 1] != WBXML_INLINE_STRING) {
                return null;
            }
            i += 2;

            // the string is null terminated so count to the null to
            // determine it length
            int j = i;
            while (wbxmlBody[j] != WBXML_ATTR_NULL) {
                j++;
            }

            // if empty string
            if (i == j) return null;

            String emailAddress =  new String(wbxmlBody, i, j-i);

            // after the null terminator there can be a extension encoded bit
            // if the extension was not included in the inline string. Check
            // to see if the next bit is one of the common encodings
            i = j + 1;
            String extension = decodeExtension(wbxmlBody[i]);
            if (extension != null) {
                emailAddress += extension;
            }

            return emailAddress;
        } catch (ArrayIndexOutOfBoundsException e) {
            if (Email.DEBUG) {
                Log.d(Email.LOG_TAG, "ArrayIndexOutOfBoundsException while handling " +
                        "CONTENT_MIME_TYPE_B_EMN");
            }
        } catch (Exception e) {
            if (Email.DEBUG) {
                Log.d(Email.LOG_TAG, "Exception while handling CONTENT_MIME_TYPE_B_EMN " + e);
            }
        }

        return null;
    }

    private String decodeExtension(byte encodedByte) {
        if (encodedByte == WBXML_ATTR_COM) {
            return ".com";
        }
        else if (encodedByte == WBXML_ATTR_EDU) {
            return ".edu";
        }
        else if (encodedByte == WBXML_ATTR_NET) {
            return ".net";
        }
        else if (encodedByte == WBXML_ATTR_ORG) {
            return ".org";
        }

        return null;
    }

}
