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

import com.batterywhitelist.plus.core.utils.SecurityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class ConfigJsonHelper {
    private static final Pattern PKG_PATTERN = Pattern.compile("^[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z][a-zA-Z0-9_]*)+$");

    public static boolean isValidPkg(String p) {
        return p != null && p.length() <= 255 && PKG_PATTERN.matcher(p).matches();
    }

    public static void exportToStream(OutputStream os, String modulePkg, Set<String> selectedPackages) throws Exception {
        JSONObject root = new JSONObject();
        root.put("schemaVersion", 1);
        root.put("module", modulePkg);
        root.put("timestamp", System.currentTimeMillis());

        List<String> sorted = new ArrayList<>(selectedPackages);
        Collections.sort(sorted);

        JSONArray arr = new JSONArray();
        for (String p : sorted) arr.put(p);
        root.put("protected_packages", arr);

        os.write(root.toString(2).getBytes(StandardCharsets.UTF_8));
        os.flush();
    }

    public static class ImportResult {
        public Set<String> importedPackages = new HashSet<>();
        public int skipped;
        public int blocked;
        public String error;
        public boolean success;
    }

    public static ImportResult parseImportStream(InputStream is, String currentModulePkg) {
        ImportResult result = new ImportResult();
        try {
            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = is.read(buf)) > 0) bos.write(buf, 0, n);
            String text = bos.toString("UTF-8");

            JSONObject root = new JSONObject(text);
            String module = root.optString("module", "");
            if (!module.isEmpty() && !currentModulePkg.equals(module)) {
                result.error = "这不是本模块的备份文件";
                return result;
            }

            int schema = root.optInt("schemaVersion", 1);
            if (schema > 1) {
                result.error = "备份版本过高，请升级模块";
                return result;
            }

            JSONArray arr = root.optJSONArray("protected_packages");
            if (arr == null) {
                result.error = "备份文件缺少 protected_packages 字段";
                return result;
            }

            for (int i = 0; i < arr.length(); i++) {
                String p = arr.optString(i, null);
                if (isValidPkg(p)) {
                    if (BatteryCoreConfig.isDangerous(p)) {
                        result.blocked++;
                    } else {
                        result.importedPackages.add(p);
                    }
                } else {
                    result.skipped++;
                }
            }
            result.success = true;
            return result;
        } catch (Throwable t) {
            result.error = t.getMessage();
            return result;
        }
    }
}