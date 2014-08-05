/*
 * Copyright (C) 2017 The Android Open Source Project
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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.android.email.Preferences;
import com.android.email.R;
import com.android.email.SecurityPolicy;
import com.android.email.provider.AccountReconciler;
import com.android.email.provider.EmailProvider;
import com.android.emailcommon.Logging;
import com.android.emailcommon.VendorPolicyLoader;
import com.android.emailcommon.provider.Account;
import com.android.emailcommon.provider.EmailContent;
import com.android.emailcommon.provider.EmailContent.AccountColumns;
import com.android.emailcommon.provider.HostAuth;
import com.android.emailcommon.provider.Mailbox;
import com.android.mail.utils.LogUtils;
import android.util.Log;


/**
 * The broadcast receiver.  The actual job is done in EmailBroadcastProcessor on a worker thread.
 * Extend EmailBroadcastReceiver to handle Bluetooth MAP relevant intents.
 */
public class BluetoothEmailBroadcastReceiver extends EmailBroadcastReceiver {

    private static final String TAG = "BluetoothEmailBroadcastReceiver";
    private static final String ACTION_CHECK_MAIL =
            "com.android.email.intent.action.MAIL_SERVICE_WAKEUP";
    private static final String EXTRA_ACCOUNT = "com.android.email.intent.extra.ACCOUNT";
    private static final String ACTION_DELETE_MESSAGE =
            "com.android.email.intent.action.MAIL_SERVICE_DELETE_MESSAGE";
    private static final String ACTION_MOVE_MESSAGE =
            "com.android.email.intent.action.MAIL_SERVICE_MOVE_MESSAGE";
    private static final String ACTION_MESSAGE_READ =
            "com.android.email.intent.action.MAIL_SERVICE_MESSAGE_READ";
    private static final String ACTION_SEND_PENDING_MAIL =
            "com.android.email.intent.action.MAIL_SERVICE_SEND_PENDING";
    private static final String EXTRA_MESSAGE_ID = "com.android.email.intent.extra.MESSAGE_ID";
    private static final String EXTRA_MESSAGE_INFO =
            "com.android.email.intent.extra.MESSAGE_INFO";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG,"Received " + action);
        if (ACTION_CHECK_MAIL.equals(action)) {
            Intent i;
            final long accountId = intent.getLongExtra(EXTRA_ACCOUNT, -1);
            Log.d(TAG, "accountId is " + accountId);
            final long inboxId = Mailbox.findMailboxOfType(context, accountId,
                    Mailbox.TYPE_INBOX);
            Log.d(TAG, "inboxId is " + inboxId);
            Mailbox mailbox = Mailbox.restoreMailboxWithId(context, inboxId);
            if (mailbox == null) {
                return;
            }
            Account account = Account.restoreAccountWithId(context, mailbox.mAccountKey);
            String protocol = account.getProtocol(context);
            Log.d(TAG, "protocol is " + protocol);
            String legacyImapProtocol = context.getString(R.string.protocol_legacy_imap);
            if (protocol.equals(legacyImapProtocol)) {
                i = new Intent(context, BluetoothImapService.class);
            } else {
                i = new Intent(context, BluetoothPop3Service.class);
            }
            i.setAction(intent.getAction());
            i.putExtra(EXTRA_ACCOUNT,
                    intent.getLongExtra(EXTRA_ACCOUNT, -1));
            context.startService(i);
        } else if (ACTION_DELETE_MESSAGE.equals(action)) {
            Intent i;
            final long messageId = intent.getLongExtra(EXTRA_MESSAGE_ID, -1);
            Log.d(TAG, "messageId is " + messageId);
            Account account = Account.getAccountForMessageId(context, messageId);
            if (account == null ) {
                return;
            }
            String protocol = account.getProtocol(context);
            Log.d(TAG, "protocol is " + protocol + " ActId: " + account.getId());
            String legacyImapProtocol = context.getString(R.string.protocol_legacy_imap);
            if (protocol.equals(legacyImapProtocol)) {
                i = new Intent(context, BluetoothImapService.class);
                i.setAction(intent.getAction());
                i.putExtra(EXTRA_ACCOUNT,
                        intent.getLongExtra(EXTRA_ACCOUNT, -1));
                i.putExtra(EXTRA_MESSAGE_ID,
                        intent.getLongExtra(EXTRA_MESSAGE_ID, -1));
                context.startService(i);
            } else {
               Log.i(TAG, "DELETE MESSAGE POP3 NOT Implemented");
            }
        } else if (ACTION_MESSAGE_READ.equals(action)) {
            Intent i;
            final long messageId = intent.getLongExtra(EXTRA_MESSAGE_ID, -1);
            Log.d(TAG, "messageId is " + messageId);
            Account account = Account.getAccountForMessageId(context, messageId);
            if (account == null ) {
                return;
            }
            String protocol = account.getProtocol(context);
            Log.d(TAG, "protocol is " + protocol + " ActId: " + account.getId());
            String legacyImapProtocol = context.getString(R.string.protocol_legacy_imap);
            if (protocol.equals(legacyImapProtocol)) {
                i = new Intent(context, BluetoothImapService.class);
                i.setAction(intent.getAction());
                i.putExtra(EXTRA_ACCOUNT,
                        intent.getLongExtra(EXTRA_ACCOUNT, -1));
                i.putExtra(EXTRA_MESSAGE_ID,
                        intent.getLongExtra(EXTRA_MESSAGE_ID, -1));
                i.putExtra(EXTRA_MESSAGE_INFO,
                        intent.getIntExtra(EXTRA_MESSAGE_INFO, 0));
                context.startService(i);
            } else {
                Log.i(TAG, "READ MESSAGE POP3 NOT Implemented");
            }
        } else if (ACTION_MOVE_MESSAGE.equals(action)) {
            Intent i;
            final long messageId = intent.getLongExtra(EXTRA_MESSAGE_ID, -1);
            Log.d(TAG, "messageId is " + messageId);
            Account account = Account.getAccountForMessageId(context, messageId);
            if (account == null ) {
                return;
            }
            String protocol = account.getProtocol(context);
            Log.d(TAG, "protocol is " + protocol + " ActId: " + account.getId());
            String legacyImapProtocol = context.getString(R.string.protocol_legacy_imap);
            if (protocol.equals(legacyImapProtocol)) {
                i = new Intent(context, BluetoothImapService.class);
                i.setAction(intent.getAction());
                i.putExtra(EXTRA_ACCOUNT,
                        intent.getLongExtra(EXTRA_ACCOUNT, -1));
                i.putExtra(EXTRA_MESSAGE_ID,
                        intent.getLongExtra(EXTRA_MESSAGE_ID, -1));
                i.putExtra(EXTRA_MESSAGE_INFO,
                        intent.getIntExtra(EXTRA_MESSAGE_INFO, 0));
                context.startService(i);
            } else {
                Log.i(TAG, "READ MESSAGE POP3 NOT Implemented");
            }
        } else if (ACTION_SEND_PENDING_MAIL.equals(action)) {
            Intent i;
            final long accountId = intent.getLongExtra(EXTRA_ACCOUNT, -1);
            Log.d(TAG, "accountId is " + accountId);
            Account account = Account.restoreAccountWithId(context, accountId);
            if (account == null ) {
                return;
            }
            String protocol = account.getProtocol(context);
            Log.d(TAG, "protocol is " + protocol);
            String legacyImapProtocol = context.getString(R.string.protocol_legacy_imap);
            if (protocol.equals(legacyImapProtocol)) {
                i = new Intent(context, BluetoothImapService.class);
                i.setAction(intent.getAction());
                i.putExtra(EXTRA_ACCOUNT,
                        intent.getLongExtra(EXTRA_ACCOUNT, -1));
                context.startService(i);
            } else {
                Log.i(TAG, "SEND MESSAGE POP3 NOT Implemented");
            }
        } else {
            EmailBroadcastProcessorService.processBroadcastIntent(context, intent);
        }
    }
}
