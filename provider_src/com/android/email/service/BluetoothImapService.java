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
import android.os.SystemClock;
import android.text.TextUtils;
import android.text.format.DateUtils;

import com.android.email.DebugUtils;
import com.android.email.LegacyConversions;
import com.android.email.NotificationController;
import com.android.email.NotificationControllerCreatorHolder;
import com.android.email.R;
import com.android.email.mail.Store;
import com.android.email.provider.Utilities;
import com.android.emailcommon.Logging;
import com.android.emailcommon.TrafficFlags;
import com.android.emailcommon.internet.MimeUtility;
import com.android.emailcommon.mail.AuthenticationFailedException;
import com.android.emailcommon.mail.FetchProfile;
import com.android.emailcommon.mail.Flag;
import com.android.emailcommon.mail.Folder;
import com.android.emailcommon.mail.Folder.FolderType;
import com.android.emailcommon.mail.Folder.MessageRetrievalListener;
import com.android.emailcommon.mail.Folder.MessageUpdateCallbacks;
import com.android.emailcommon.mail.Folder.OpenMode;
import com.android.emailcommon.mail.Message;
import com.android.emailcommon.mail.MessagingException;
import com.android.emailcommon.mail.Part;
import com.android.emailcommon.provider.Account;
import com.android.emailcommon.provider.EmailContent;
import com.android.emailcommon.provider.EmailContent.MailboxColumns;
import com.android.emailcommon.provider.EmailContent.MessageColumns;
import com.android.emailcommon.provider.EmailContent.SyncColumns;
import com.android.emailcommon.provider.Mailbox;
import com.android.emailcommon.service.EmailServiceStatus;
import com.android.emailcommon.service.SearchParams;
import com.android.emailcommon.service.SyncWindow;
import com.android.emailcommon.utility.AttachmentUtilities;
import com.android.mail.providers.UIProvider;
import com.android.mail.utils.LogUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class BluetoothImapService extends ImapService {
    private static final String TAG = "BluetoothImapService";
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
    public int onStartCommand(Intent intent, int flags, int startId) {

        final String action = intent.getAction();
        if (Logging.LOGD) {
            LogUtils.d(Logging.LOG_TAG, "Action: ", action);
        }
        final long accountId = intent.getLongExtra(EXTRA_ACCOUNT, -1);
        Context context = getApplicationContext();
        if (ACTION_CHECK_MAIL.equals(action)) {
            final long inboxId = Mailbox.findMailboxOfType(context, accountId,
                    Mailbox.TYPE_INBOX);
            if (Logging.LOGD) {
                LogUtils.d(Logging.LOG_TAG, "accountId is " + accountId);
                LogUtils.d(Logging.LOG_TAG, "inboxId is " + inboxId);
            }
            if (accountId <= -1 || inboxId <= -1 ) {
                return START_NOT_STICKY;
            }
            mBinder.init(context);
            mBinder.requestSync(inboxId,true,0);
        } else if (ACTION_DELETE_MESSAGE.equals(action)) {
            final long messageId = intent.getLongExtra(EXTRA_MESSAGE_ID, -1);
            if (Logging.LOGD) {
                LogUtils.d(Logging.LOG_TAG, "action: Delete Message mail");
                LogUtils.d(Logging.LOG_TAG, "action: delmsg " + messageId);
            }
            if (accountId <= -1 || messageId <= -1 ) {
                return START_NOT_STICKY;
            }
            Store remoteStore = null;
            try {
                remoteStore = Store.getInstance(Account.getAccountForMessageId(context, messageId),
                        context);
                mBinder.init(context);
                mBinder.deleteMessage(messageId);
                synchronizePendingActions(context,
                        Account.getAccountForMessageId(context, messageId), remoteStore, true);
            } catch (Exception e) {
                LogUtils.d(Logging.LOG_TAG, "RemoteException " + e);
            }
        } else if (ACTION_MESSAGE_READ.equals(action)) {
            final long messageId = intent.getLongExtra(EXTRA_MESSAGE_ID, -1);
            final int flagRead = intent.getIntExtra(EXTRA_MESSAGE_INFO, 0);
            if (Logging.LOGD) {
                LogUtils.d(Logging.LOG_TAG, "action: Message Mark Read or UnRead ");
                LogUtils.d(Logging.LOG_TAG, "action: delmsg " + messageId);
            }
            if (accountId <= -1 || messageId <= -1 ) {
                return START_NOT_STICKY;
            }
            Store remoteStore = null;
            try {
                mBinder.init(context);
                mBinder.setMessageRead(messageId, (flagRead == 1)? true:false);
                remoteStore = Store.getInstance(Account.getAccountForMessageId(context, messageId),
                        context);
                synchronizePendingActions(context,
                        Account.getAccountForMessageId(context, messageId), remoteStore, true);
            } catch (Exception e){
                LogUtils.d(Logging.LOG_TAG, "RemoteException " + e);
            }
        } else if (ACTION_MOVE_MESSAGE.equals(action)) {
            final long messageId = intent.getLongExtra(EXTRA_MESSAGE_ID, -1);
            final int  mailboxType = intent.getIntExtra(EXTRA_MESSAGE_INFO, Mailbox.TYPE_INBOX);
            final long mailboxId = Mailbox.findMailboxOfType(context, accountId, mailboxType);
            if (Logging.LOGD) {
                LogUtils.d(Logging.LOG_TAG, "action:  Move Message mail");
                LogUtils.d(Logging.LOG_TAG, "action: movemsg " + messageId +
                        "mailbox: " + mailboxType + "accountId: " + accountId + "mailboxId: "
                        + mailboxId);
            }
            if (accountId <= -1 || messageId <= -1 || mailboxId <= -1){
                return START_NOT_STICKY;
            }
            Store remoteStore = null;
            try {
                mBinder.init(context);
                mBinder.MoveMessages(messageId, mailboxId);
                remoteStore = Store.getInstance(Account.getAccountForMessageId(context, messageId),
                        context);
                synchronizePendingActions(context,
                        Account.getAccountForMessageId(context, messageId),remoteStore, true);
            } catch (Exception e){
                LogUtils.d(Logging.LOG_TAG, "RemoteException " + e);
            }
        } else if (ACTION_SEND_PENDING_MAIL.equals(action)) {
            if (Logging.LOGD) {
                LogUtils.d(Logging.LOG_TAG, "action: Send Pending Mail " + accountId);
            }
            if (accountId <= -1 ) {
                 return START_NOT_STICKY;
            }
            try {
                mBinder.init(context);
                mBinder.sendMail(accountId);
            } catch (Exception e) {
                LogUtils.e(Logging.LOG_TAG, "RemoteException " + e);
            }
        }

        return Service.START_STICKY;
    }

    /*
      Create our EmailService implementation here.
     */
    class BluetoothEmailServiceStub extends EmailServiceStub {
        /**
         * Delete a single message by moving it to the trash, or really delete it if it's already in
         * trash or a draft message.
         *
         * This function has no callback, no result reporting, because the desired outcome
         * is reflected entirely by changes to one or more cursors.
         *
         * @param messageId The id of the message to "delete".
         */
        public void deleteMessage(long messageId) {

            final EmailContent.Message message =
                    EmailContent.Message.restoreMessageWithId(mContext, messageId);
            if (message == null) {
                if (Logging.LOGD) LogUtils.v(Logging.LOG_TAG, "dletMsg message NULL");
                return;
            }
            // 1. Get the message's account
            final Account account = Account.restoreAccountWithId(mContext, message.mAccountKey);
            // 2. Get the message's original mailbox
            final Mailbox mailbox = Mailbox.restoreMailboxWithId(mContext, message.mMailboxKey);
            if (account == null || mailbox == null) {
                if (Logging.LOGD) LogUtils.v(Logging.LOG_TAG, "dletMsg account or mailbox NULL");
                return;
            }
            if(Logging.LOGD)
                LogUtils.d(Logging.LOG_TAG, "AccountKey " + account.mId + "oirigMailbix: "
                        + mailbox.mId);
            // 3. Confirm that there is a trash mailbox available.  If not, create one
            Mailbox trashFolder =  Mailbox.restoreMailboxOfType(mContext, account.mId,
                    Mailbox.TYPE_TRASH);
            if (trashFolder == null) {
                if (Logging.LOGD) LogUtils.v(Logging.LOG_TAG, "dletMsg Trash mailbox NULL");
            } else {
                LogUtils.d(Logging.LOG_TAG, "TrasMailbix: " + trashFolder.mId);
            }
            // 4.  Drop non-essential data for the message (e.g. attachment files)
            AttachmentUtilities.deleteAllAttachmentFiles(mContext, account.mId,
                    messageId);

            Uri uri = ContentUris.withAppendedId(EmailContent.Message.SYNCED_CONTENT_URI,
                    messageId);

            // 5. Perform "delete" as appropriate
            if ((mailbox.mId == trashFolder.mId) || (mailbox.mType == Mailbox.TYPE_DRAFTS)) {
                // 5a. Really delete it
                mContext.getContentResolver().delete(uri, null, null);
            } else {
                // 5b. Move to trash
                ContentValues cv = new ContentValues();
                cv.put(EmailContent.MessageColumns.MAILBOX_KEY, trashFolder.mId);
                mContext.getContentResolver().update(uri, cv, null, null);
            }
        }

        /**
         * Moves messages to a new mailbox.
         * This function has no callback, no result reporting, because the desired outcome
         * is reflected entirely by changes to one or more cursors.
         * Note this method assumes all of the given message and mailbox IDs belong to the same
         * account.
         *
         * @param messageIds IDs of the messages that are to be moved
         * @param newMailboxId ID of the new mailbox that the messages will be moved to
         * @return an asynchronous task that executes the move (for testing only)
         */
         public void MoveMessages(long messageId, long newMailboxId) {
             Account account = Account.getAccountForMessageId(mContext, messageId);
            if (account != null) {
                if (Logging.LOGD) {
                   LogUtils.d(Logging.LOG_TAG, "moveMessage Acct " + account.mId);
                   LogUtils.d(Logging.LOG_TAG, "moveMessage messageId:" + messageId);
                }
               ContentValues cv = new ContentValues();
               cv.put(EmailContent.MessageColumns.MAILBOX_KEY, newMailboxId);
               ContentResolver resolver = mContext.getContentResolver();
               Uri uri = ContentUris.withAppendedId(
                    EmailContent.Message.SYNCED_CONTENT_URI, messageId);
               resolver.update(uri, cv, null, null);
           } else {
               LogUtils.d(Logging.LOG_TAG, "moveMessage Cannot find account");
           }
       }

       /**
        * Set/clear boolean columns of a message
        * @param messageId the message to update
        * @param columnName the column to update
        * @param columnValue the new value for the column
        */
        private void setMessageBoolean(long messageId, String columnName, boolean columnValue) {
           ContentValues cv = new ContentValues();
           cv.put(columnName, columnValue);
           Uri uri = ContentUris.withAppendedId(EmailContent.Message.SYNCED_CONTENT_URI, messageId);
           mContext.getContentResolver().update(uri, cv, null, null);
       }

       /**
        * Set/clear the unread status of a message
        *
        * @param messageId the message to update
        * @param isRead the new value for the isRead flag
        */
        public void setMessageRead(long messageId, boolean isRead) {
           setMessageBoolean(messageId, EmailContent.MessageColumns.FLAG_READ, isRead);
        }

    };

    private final BluetoothEmailServiceStub mBinder = new BluetoothEmailServiceStub ();

       @Override
       public IBinder onBind(Intent intent) {
          mBinder.init(this);
          return mBinder;
      }
    }
