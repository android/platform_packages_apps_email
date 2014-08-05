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

import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.TrafficStats;
import android.net.Uri;
import android.os.IBinder;
import android.os.RemoteException;

import com.android.email.DebugUtils;
import com.android.email.NotificationController;
import com.android.email.NotificationControllerCreatorHolder;
import com.android.email.mail.Store;
import com.android.email.mail.store.Pop3Store;
import com.android.email.mail.store.Pop3Store.Pop3Folder;
import com.android.email.mail.store.Pop3Store.Pop3Message;
import com.android.email.provider.Utilities;
import com.android.emailcommon.Logging;
import com.android.emailcommon.TrafficFlags;
import com.android.emailcommon.mail.AuthenticationFailedException;
import com.android.emailcommon.mail.Folder.OpenMode;
import com.android.emailcommon.mail.MessagingException;
import com.android.emailcommon.provider.Account;
import com.android.emailcommon.provider.EmailContent;
import com.android.emailcommon.provider.EmailContent.Attachment;
import com.android.emailcommon.provider.EmailContent.AttachmentColumns;
import com.android.emailcommon.provider.EmailContent.Message;
import com.android.emailcommon.provider.EmailContent.MessageColumns;
import com.android.emailcommon.provider.EmailContent.SyncColumns;
import com.android.emailcommon.provider.Mailbox;
import com.android.emailcommon.service.EmailServiceStatus;
import com.android.emailcommon.service.IEmailServiceCallback;
import com.android.emailcommon.utility.AttachmentUtilities;
import com.android.mail.providers.UIProvider;
import com.android.mail.providers.UIProvider.AttachmentState;
import com.android.mail.utils.LogUtils;

import org.apache.james.mime4j.EOLConvertingInputStream;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import android.util.Log;

public class BluetoothPop3Service extends Pop3Service {
    private static final String TAG = "BluetoothPop3Service";
    private static final String ACTION_CHECK_MAIL =
            "com.android.email.intent.action.MAIL_SERVICE_WAKEUP";
    private static final String EXTRA_ACCOUNT = "com.android.email.intent.extra.ACCOUNT";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return Service.START_STICKY;

        final String action = intent.getAction();
        Log.d(TAG, "action is " + action);
        Context context = getApplicationContext();
        if (ACTION_CHECK_MAIL.equals(action)) {
            final long accountId = intent.getLongExtra(EXTRA_ACCOUNT, -1);
            final long inboxId = Mailbox.findMailboxOfType(context, accountId, Mailbox.TYPE_INBOX);
            Log.d(TAG, "accountId is " + accountId + ", inboxId is " + inboxId);
            mBinder.init(context);
            mBinder.requestSync(inboxId, true, 0);
        }
        return Service.START_STICKY;
    }

    /**
     * Create our EmailService implementation here.
     */
    private final EmailServiceStub mBinder = new EmailServiceStub() {
    };

    @Override
    public IBinder onBind(Intent intent) {
        mBinder.init(this);
        return mBinder;
    }

    }
