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

package com.batterywhitelist.plus.core.config;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class BatteryCoreConfig {
    public static final String PREFS_NAME = "battery_whitelist_prefs";
    public static final String KEY_LIST = "protected_packages";
    public static final String KEY_AGREED = "user_agreed_agreement";
    public static final String KEY_AGREED_HASH = "user_agreed_eula_hash";
    public static final String KEY_LAST_UPDATE_CHECK = "last_update_check";

    public static final String GUARD_SCRIPT_PATH = "/data/adb/service.d/battery_guard.sh";
    public static final String GUARD_SCRIPT_DIR = "/data/adb/service.d";
    public static final String LAUNCHER_ALIAS_NAME = "com.batterywhitelist.plus.LauncherAlias";

    // 🌟 系统核心组件黑名单
    public static final Set<String> DANGEROUS_PACKAGES = new HashSet<>(Arrays.asList(
            "android",
            "com.android.systemui",
            "com.android.settings",
            "com.android.phone",
            "com.android.server.telecom",
            "com.android.bluetooth",
            "com.android.providers.settings",
            "com.android.permissioncontroller"
    ));

    public static boolean isDangerous(String pkg) {
        return DANGEROUS_PACKAGES.contains(pkg);
    }
}