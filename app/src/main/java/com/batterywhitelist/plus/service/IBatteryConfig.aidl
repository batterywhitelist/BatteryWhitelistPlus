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
// IBatteryConfig.aidl
package com.batterywhitelist.plus.core.service;

interface IBatteryConfig {
    // 告诉底层哪些应用需要保护（UI 发给 Hook）
    void updateWhitelist(String[] packages);
    
    // 获取底层当前的状态（Hook 返回给 UI）
    String[] getCurrentWhitelist();
}