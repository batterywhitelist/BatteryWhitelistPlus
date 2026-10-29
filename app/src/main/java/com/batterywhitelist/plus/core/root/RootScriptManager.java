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

package com.batterywhitelist.plus.core.root;

import com.batterywhitelist.plus.core.config.BatteryCoreConfig;
import com.batterywhitelist.plus.core.utils.SecurityUtils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class RootScriptManager {
    private static final long SU_TIMEOUT_SEC = 10L;

    public static boolean deployGuardScript(Set<String> selectedPackages, Set<String> lastDeployed) {
        Set<String> snapshot = new HashSet<>(selectedPackages);
        // 强制剔除黑名单
        snapshot.removeAll(BatteryCoreConfig.DANGEROUS_PACKAGES);

        if (snapshot.equals(lastDeployed)) return false; // 无变化

        if (snapshot.isEmpty()) {
            String existing = runSuCommandForOutput("test -f " + BatteryCoreConfig.GUARD_SCRIPT_PATH + " && echo yes || echo no\n", SU_TIMEOUT_SEC);
            if ("yes".equals(existing)) {
                runSuCommand("rm -f " + BatteryCoreConfig.GUARD_SCRIPT_PATH + "\n", SU_TIMEOUT_SEC);
            }
            lastDeployed.clear();
            return true;
        }

        String newScript = buildGuardScript(snapshot);
        String newMd5 = SecurityUtils.md5Hex(newScript);
        if (newMd5 == null) return false;

        String currentMd5 = runSuCommandForOutput(
                "test -f " + BatteryCoreConfig.GUARD_SCRIPT_PATH + " && md5sum " + BatteryCoreConfig.GUARD_SCRIPT_PATH
                        + " | cut -d' ' -f1 || echo none\n", SU_TIMEOUT_SEC);

        if (newMd5.equals(currentMd5)) {
            lastDeployed.clear();
            lastDeployed.addAll(snapshot);
            return false;
        }

        String cmd = "mkdir -p " + BatteryCoreConfig.GUARD_SCRIPT_DIR + "\n"
                + "cat > " + BatteryCoreConfig.GUARD_SCRIPT_PATH + " << 'BATTERY_GUARD_EOF'\n"
                + newScript
                + "BATTERY_GUARD_EOF\n"
                + "chmod 755 " + BatteryCoreConfig.GUARD_SCRIPT_PATH + "\n";

        if (runSuCommand(cmd, SU_TIMEOUT_SEC)) {
            lastDeployed.clear();
            lastDeployed.addAll(snapshot);
            return true;
        }
        return false;
    }

    private static String buildGuardScript(Set<String> packages) {
        StringBuilder guardCmd = new StringBuilder();
        for (String p : packages) {
            guardCmd.append("  cmd deviceidle whitelist +").append(p).append(" 2>/dev/null\n")
                    .append("  appops set ").append(p).append(" RUN_IN_BACKGROUND allow 2>/dev/null\n")
                    .append("  appops set ").append(p).append(" RUN_ANY_IN_BACKGROUND allow 2>/dev/null\n");
        }

        return "#!/system/bin/sh\n"
                + "until [ \"$(getprop sys.boot_completed)\" = \"1\" ]; do sleep 2; done\n"
                + "sleep 15\n"
                + "while true; do\n"
                + guardCmd
                + "  sleep 60\n"
                + "done\n";
    }

    public static boolean runSuCommand(String commands, long timeoutSec) {
        Process p = null;
        try {
            p = new ProcessBuilder("su").redirectErrorStream(true).start();
            try (OutputStream os = p.getOutputStream()) {
                os.write(commands.getBytes(StandardCharsets.UTF_8));
                os.flush();
            }
            drainStream(p.getInputStream());
            if (!p.waitFor(timeoutSec, TimeUnit.SECONDS)) return false;
            return p.exitValue() == 0;
        } catch (Throwable t) {
            return false;
        } finally {
            if (p != null) { try { p.destroy(); } catch (Throwable ignored) {} }
        }
    }

    public static String runSuCommandForOutput(String commands, long timeoutSec) {
        Process p = null;
        try {
            p = new ProcessBuilder("su").redirectErrorStream(true).start();
            try (OutputStream os = p.getOutputStream()) {
                os.write(commands.getBytes(StandardCharsets.UTF_8));
                os.flush();
            }
            StringBuilder sb = new StringBuilder();
            try (BufferedReader r = new BufferedReader(
                    new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = r.readLine()) != null) sb.append(line).append('\n');
            }
            if (p.waitFor(timeoutSec, TimeUnit.SECONDS)) return sb.toString().trim();
            return null;
        } catch (Throwable t) {
            return null;
        } finally {
            if (p != null) { try { p.destroy(); } catch (Throwable ignored) {} }
        }
    }

    private static void drainStream(InputStream is) {
        if (is == null) return;
        try (InputStream in = is) {
            in.transferTo(OutputStream.nullOutputStream());
        } catch (Throwable ignored) {}
    }
}