/*
 * Copyright 2026 ©MikotoNetwork
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.batterywhitelist.plus.core.system;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class AppListRepository {

    public static class AppItem {
        public final String name;
        public final String lowerName;
        public final String pkg;
        public final String lowerPkg;

        public AppItem(String name, String pkg) {
            this.name = name;
            this.pkg = pkg;
            this.lowerName = name.toLowerCase(Locale.ROOT);
            this.lowerPkg = pkg.toLowerCase(Locale.ROOT);
        }
    }

    public static List<AppItem> loadInstalledApps(Context context, String selfPkg) {
        PackageManager pm = context.getPackageManager();
        List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        final List<AppItem> items = new ArrayList<>(packages.size());

        for (ApplicationInfo info : packages) {
            if (selfPkg.equals(info.packageName) || "android".equals(info.packageName)) continue;
            String name;
            try {
                name = info.loadLabel(pm).toString();
            } catch (Throwable t) {
                name = info.packageName;
            }
            items.add(new AppItem(name, info.packageName));
        }

        try {
            Collections.sort(items, (a, b) -> a.lowerName.compareTo(b.lowerName));
        } catch (Throwable ignored) {}
        
        return items;
    }
}