/*
 * Copyright (c) 2016, The Linux Foundation. All rights reserved.
 * Not a Contribution.
 *
 * Copyright (C) 2015 The Android Open Source Project
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
package com.android.email.activity;

import android.Manifest.permission;
import android.app.Activity;
import com.android.email.R;

/**
 * Activity that requests permissions needed for activities exported from Contacts.
 */
public class RequestPermissionsActivity extends RequestPermissionsActivityBase {

    private static final String[] REQUIRED_PERMISSIONS = new String[]{
            permission.READ_CONTACTS, // Contacts group
            permission.READ_EXTERNAL_STORAGE,
            permission.WRITE_EXTERNAL_STORAGE,
            permission.READ_CALENDAR, // Calendar group
            permission.WRITE_CALENDAR, // Calendar group
            permission.GET_ACCOUNTS,
            permission.READ_PHONE_STATE
    };

    @Override
    protected String[] getRequiredPermissions() {
        return REQUIRED_PERMISSIONS;
    }

    @Override
    protected String[] getDesiredPermissions() {
        return new String[]{
                permission.READ_CONTACTS, // Contacts group
                permission.READ_EXTERNAL_STORAGE,
                permission.WRITE_EXTERNAL_STORAGE,
                permission.READ_CALENDAR, // Calendar group
                permission.WRITE_CALENDAR, // Calendar group
                permission.GET_ACCOUNTS,
                permission.READ_PHONE_STATE
       };
    }

    public static boolean startPermissionActivity(Activity activity) {
        return startPermissionActivity(activity,
                REQUIRED_PERMISSIONS,
                RequestPermissionsActivity.class);
    }
}
