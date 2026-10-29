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

package com.batterywhitelist.plus.core.security;

import android.content.Context;
import com.batterywhitelist.plus.core.config.BatteryCoreConfig;
import com.batterywhitelist.plus.core.utils.SecurityUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class EulaManager {
    
    public static String loadEulaFromAssets(Context context) {
        try (InputStream is = context.getAssets().open("EULA.txt")) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = is.read(buf)) > 0) bos.write(buf, 0, n);
            return bos.toString("UTF-8");
        } catch (IOException e) {
            return null;
        }
    }

    public static boolean isAgreed(Context context) {
        String eula = loadEulaFromAssets(context);
        if (eula == null) return false;
        
        String eulaHash = SecurityUtils.sha256Hex(eula);
        String agreedHash = context.getSharedPreferences(BatteryCoreConfig.PREFS_NAME, Context.MODE_PRIVATE)
                .getString(BatteryCoreConfig.KEY_AGREED_HASH, "");
        boolean agreed = context.getSharedPreferences(BatteryCoreConfig.PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(BatteryCoreConfig.KEY_AGREED, false);
                
        return agreed && eulaHash != null && eulaHash.equals(agreedHash);
    }
}