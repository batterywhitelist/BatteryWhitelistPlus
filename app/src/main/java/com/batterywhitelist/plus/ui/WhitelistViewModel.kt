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

package com.batterywhitelist.plus.ui

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.batterywhitelist.plus.core.config.BatteryCoreConfig
import com.batterywhitelist.plus.core.root.RootScriptManager
import com.batterywhitelist.plus.core.system.AppListRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WhitelistViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext

    // 核心状态数据
    private val _apps = MutableStateFlow<List<AppListRepository.AppItem>>(emptyList())
    val apps: StateFlow<List<AppListRepository.AppItem>> = _apps.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedPackages = MutableStateFlow<Set<String>>(emptySet())
    val selectedPackages: StateFlow<Set<String>> = _selectedPackages.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // 防抖部署 Job
    private var deployJob: Job? = null
    private val lastDeployed = mutableSetOf<String>()

    init {
        // 1. 连接 LSPosed AIDL 服务
        LsposedBridge.connect()
        
        // 2. 读取 SP 中保存的白名单
        loadSavedPackages()
        
        // 3. 加载应用列表
        loadInstalledApps()
    }

    private fun loadSavedPackages() {
        val prefs = context.getSharedPreferences(BatteryCoreConfig.PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getStringSet(BatteryCoreConfig.KEY_LIST, emptySet()) ?: emptySet()
        // 过滤高危包名
        val safeSaved = saved.filter { !BatteryCoreConfig.isDangerous(it) }.toSet()
        _selectedPackages.value = safeSaved
    }

    private fun loadInstalledApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val items = AppListRepository.loadInstalledApps(context, context.packageName)
            _apps.value = items
            _isLoading.value = false
            
            // 首次加载完成后，同步一次底层
            scheduleDeploy()
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleApp(pkg: String, isChecked: Boolean) {
        val current = _selectedPackages.value.toMutableSet()
        if (isChecked) {
            if (BatteryCoreConfig.isDangerous(pkg)) {
                Log.e("WhitelistViewModel", "拦截危险操作: $pkg")
                return
            }
            current.add(pkg)
        } else {
            current.remove(pkg)
        }
        _selectedPackages.value = current

        // 保存到 SP 兜底
        viewModelScope.launch(Dispatchers.IO) {
            context.getSharedPreferences(BatteryCoreConfig.PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putStringSet(BatteryCoreConfig.KEY_LIST, current).commit()
        }

        // 实时通知 AIDL
        LsposedBridge.updateWhitelist(current)
        
        // 防抖部署 Root 脚本
        scheduleDeploy()
    }

    fun setImportedPackages(newPackages: Set<String>) {
        _selectedPackages.value = newPackages
        // 同步 SP、AIDL、Root
        viewModelScope.launch(Dispatchers.IO) {
            context.getSharedPreferences(BatteryCoreConfig.PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putStringSet(BatteryCoreConfig.KEY_LIST, newPackages).commit()
        }
        LsposedBridge.updateWhitelist(newPackages)
        scheduleDeploy()
    }

    // Root 脚本延迟部署 (Debounce 3秒)
    private fun scheduleDeploy() {
        deployJob?.cancel()
        deployJob = viewModelScope.launch {
            delay(3000L) // DEPLOY_DEBOUNCE_MS
            withContext(Dispatchers.IO) {
                val success = RootScriptManager.deployGuardScript(_selectedPackages.value, lastDeployed)
                if (success) {
                    Log.d("WhitelistViewModel", "守护脚本已更新")
                    // 可选：发个 Event 给 UI 弹 Toast
                }
            }
        }
    }
}