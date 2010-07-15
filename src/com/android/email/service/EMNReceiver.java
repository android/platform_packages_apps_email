/*
 * Copyright (C) 2008 The Android Open Source Project
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

import com.android.email.provider.EmailContent;
import com.android.email.provider.EmailContent.AccountColumns;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import static android.provider.Telephony.Sms.Intents.WAP_PUSH_RECEIVED_ACTION;

/**
 * Receives WAP broadcast messages to respond to EMN push notifications.
 */

public class EMNReceiver extends BroadcastReceiver {

    private static final String CONTENT_MIME_TYPE_B_EMN = "application/vnd.wap.emn+wbxml";

    private static final String[] ID_PROJECTION = new String[] {
        EmailContent.RECORD_ID
    };

    private static final String ACTION_CHECK_MAIL = "com.android.email.intent.action.MAIL_SERVICE_WAKEUP";

    private static final String EXTRA_CHECK_ACCOUNT = "com.android.email.intent.extra.ACCOUNT";

    @Override
    public void onReceive(Context context, Intent intent) {

        if (intent.getAction().equals(WAP_PUSH_RECEIVED_ACTION)) {
            // get the right mimtype
            String mimeType = intent.getType();
            if (mimeType.equals(CONTENT_MIME_TYPE_B_EMN)) {
                // get the WBXML encoded data
                byte[] wbxml = intent.getByteArrayExtra("data");
                if (wbxml == null) {
                    return;
                }

                // decode wbxml and get the email address
                EmnDecoder emn = new EmnDecoder();
                emn.decode(wbxml);

                if (emn.mEmailAddress.length() > 0) {

                    // find the account associated with this email address
                    ContentResolver cr = context.getContentResolver();
                    Cursor accountCursor = cr.query(
                            com.android.email.provider.EmailContent.Account.CONTENT_URI,
                            ID_PROJECTION, AccountColumns.EMAIL_ADDRESS + "=?", new String[] {
                                emn.mEmailAddress
                            }, null);

                    // if found kick off a sync
                    try {
                        if (accountCursor.moveToFirst()) {
                            long accountId = accountCursor.getLong(0);
                            Intent in = new Intent();
                            in.setClassName("com.android.email",
                                    "com.android.email.service.MailService");
                            in.setAction(ACTION_CHECK_MAIL);
                            in.putExtra(EXTRA_CHECK_ACCOUNT, accountId);
                            context.startService(in);
                        }
                    } finally {
                        accountCursor.close();
                    }
                }
            }
        }
    }

    /**
     * Minimal EMN WAP message decoder application/vnd.wap.emn+wbxml
     */
    private class EmnDecoder {

        // decode EMN WAP encoded message
        // 

        private static final int WBXML_INLINE_STRING = 0x03;

        private static final int WBXML_TOKEN_MASK = 0x3F;

        private static final int WBXML_TOKEN_WITH_ATTRS = 0x80;

        /**
         * Holds the email address contained in the EMN message after its been
         * decoded
         */
        public String mEmailAddress;

        /**
         * Decodes a wbxml EMN message and stores the email address in it into
         * mEmailAddress
         */
        public boolean decode(byte[] wbxmlBody) {

            // the decoded wap in xml looks like
            // <emn mailbox="mailat:bob@hotmail.com"
            // timestamp="2008-06-30T15:17:00z@/>

            // skip 4 to skip over header (version, publicID, charset, string
            // table length)
            int i = 4;

            // expect <emn TAG with attributes
            if (false == ((wbxmlBody[i] & WBXML_TOKEN_MASK) == 0x05)) {
                return false;
            }
            if (false == ((wbxmlBody[i] & WBXML_TOKEN_WITH_ATTRS) == WBXML_TOKEN_WITH_ATTRS)) {
                return false;
            }

            // if its time stamp skip it
            // maybe the timestamp was put before the mailbox
            i++;
            if ((wbxmlBody[i] & WBXML_TOKEN_MASK) == 0x05) {
                i += 8;
            }

            // only accept mailbox="xxxx@xxxx.xxx" or
            // mailbox="mailat:xxxx@xxxx.xxx"
            // URI support is optional and other URI have security risks
            // expect mailbox ="  or mailbox="mailat:
            if (false == ((wbxmlBody[i] & WBXML_TOKEN_MASK) == 0x06 || (wbxmlBody[i] & WBXML_TOKEN_MASK) == 0x07)) {
                return false;
            }

            // expect in line string which is the email address
            if (wbxmlBody[i + 1] != WBXML_INLINE_STRING) {
                return false;
            }
            i += 2;

            // the string is null terminated so count to the null to determine
            // it length
            int len = 0;
            int j = i;
            while (wbxmlBody[j] != 0) {
                j++;
                len++;
            }

            String value = new String(wbxmlBody, i, len);
            mEmailAddress = value;

            return true;
        }
    }
}
