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

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import com.batterywhitelist.plus.core.config.BatteryCoreConfig;

public class LauncherManager {
    public static boolean isLauncherIconHidden(Context context) {
        try {
            PackageManager pm = context.getPackageManager();
            ComponentName componentName = new ComponentName(context, BatteryCoreConfig.LAUNCHER_ALIAS_NAME);
            int state = pm.getComponentEnabledSetting(componentName);
            return state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean setLauncherIconState(Context context, boolean enabled) {
        try {
            PackageManager pm = context.getPackageManager();
            ComponentName componentName = new ComponentName(context, BatteryCoreConfig.LAUNCHER_ALIAS_NAME);
            int newState = enabled ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                    : PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
            pm.setComponentEnabledSetting(componentName, newState, PackageManager.DONT_KILL_APP);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}