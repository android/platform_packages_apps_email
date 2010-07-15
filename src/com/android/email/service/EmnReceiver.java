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
import android.util.Log;
import static android.provider.Telephony.Sms.Intents.WAP_PUSH_RECEIVED_ACTION;

/**
 * Receives WAP broadcast messages to respond to EMN push notifications.
 */

public class EmnReceiver extends BroadcastReceiver {

    private static final String CONTENT_MIME_TYPE_B_EMN =
            "application/vnd.wap.emn+wbxml";

    private static final int WBXML_INLINE_STRING = 0x03;
    private static final int WBXML_TOKEN_MASK = 0x3F;
    private static final int WBXML_TOKEN_WITH_ATTRS = 0x80;

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

                if (emailAddress.length()>0) {

                    if (Email.DEBUG) {
                        Log.d(Email.LOG_TAG, "CONTENT_MIME_TYPE_B_EMN received for email " + emailAddress);
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
            // timestamp="2008-06-30T15:17:00z@/>

            // skip 4 to skip over header (version, publicID, charset,
            // string table length)
            int i = 4;

            // expect <emn TAG with attributes
            if (false == ((wbxmlBody[i] & WBXML_TOKEN_MASK) == 0x05)) {
                return "";
            }
            if (false == ((wbxmlBody[i] &
                    WBXML_TOKEN_WITH_ATTRS) == WBXML_TOKEN_WITH_ATTRS)) {
                return "";
            }

            // if its time stamp skip it
            // maybe the time stamp was put before the mailbox
            i++;
            if ((wbxmlBody[i] & WBXML_TOKEN_MASK) == 0x05) {
                i += 8;
            }

            // only accept mailbox="xxxx@xxxx.xxx" or
            // mailbox="mailat:xxxx@xxxx.xxx"
            // URI support is optional and other URI have security risks
            // expect mailbox ="  or mailbox="mailat:
            if (false == ((wbxmlBody[i] & WBXML_TOKEN_MASK) == 0x06 ||
                    (wbxmlBody[i] & WBXML_TOKEN_MASK) == 0x07)) {
                return "";
            }

            // expect in line string which is the email address
            if (wbxmlBody[i + 1] != WBXML_INLINE_STRING) {
                return "";
            }
            i += 2;

            // the string is null terminated so count to the null to
            // determine it length
            int j = i;
            while (wbxmlBody[j] != 0) {
                j++;
            }

            String value = new String(wbxmlBody, i, j-i);

            return value;

        } catch (ArrayIndexOutOfBoundsException e) {
            if (Email.DEBUG) {
                Log.d(Email.LOG_TAG, "ArrayIndexOutOfBoundsException while handling CONTENT_MIME_TYPE_B_EMN");
            }
        } catch (Exception e) {
            if (Email.DEBUG) {
                Log.d(Email.LOG_TAG, "Exception while handling CONTENT_MIME_TYPE_B_EMN " + e);
            }
        }

        return "";
    }

}
