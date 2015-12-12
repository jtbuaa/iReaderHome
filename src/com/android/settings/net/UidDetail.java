/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.settings.net;

import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;

public class UidDetail {
    // must get at first, from package manager or db. will use it to sort.
    public String pinyin;

    // below can get at first, which have no influence on performance
    public String packageName;
    public String className;
    public String sourceDir;
    public boolean isSystem;
    public int hashCode;

    // below is time consuming, need get in async task
    public Drawable icon;
    public String label;
    public String versionName;

    // may be null if the uidDetail get from db
    public ResolveInfo info;

    public boolean found = false;
}
