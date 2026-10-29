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

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.batterywhitelist.plus.core.config.BatteryCoreConfig
import com.batterywhitelist.plus.core.config.ConfigJsonHelper
import com.batterywhitelist.plus.core.security.EulaManager
import com.batterywhitelist.plus.core.utils.SecurityUtils
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.io.InputStream
import java.io.OutputStream

class MainActivity : ComponentActivity() {

    private val viewModel: WhitelistViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 检查用户协议是否同意
        val isAgreed = EulaManager.isAgreed(this)

        setContent {
            // 全局 MIUIX 主题
            MiuixTheme {
                Surface(color = MiuixTheme.colorScheme.background) {
                    if (!isAgreed) {
                        EulaScreen(onAgree = { 
                            // 同意后刷新 Activity 或使用状态控制
                            recreate() 
                        })
                    } else {
                        MainScreen(viewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun MainScreen(viewModel: WhitelistViewModel) {
    val context = LocalContext.current
    val apps by viewModel.apps.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedPackages by viewModel.selectedPackages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // 过滤列表
    val filteredApps = remember(apps, searchQuery) {
        if (searchQuery.isBlank()) apps else {
            apps.filter { 
                it.name.contains(searchQuery, ignoreCase = true) || 
                it.pkg.contains(searchQuery, ignoreCase = true) 
            }
        }
    }

    // SAF 导入选择器
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            try {
                val is = context.contentResolver.openInputStream(uri)
                if (is != null) {
                    val result = ConfigJsonHelper.parseImportStream(is, context.packageName)
                    if (result.success) {
                        viewModel.setImportedPackages(result.importedPackages)
                        Toast.makeText(context, "成功导入 ${result.importedPackages.size} 个应用", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "导入失败: ${result.error}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "读取文件失败", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = "电池白名单 Plus",
                // MIUIX 的 Action 按钮通常放在这里
                actions = {
                    TextButton(text = "导入", onClick = { importLauncher.launch(arrayOf("application/json")) })
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // MIUIX 搜索框
            TextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                label = "搜索应用或包名"
            )

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(filteredApps, key = { it.pkg }) { app ->
                        val isChecked = selectedPackages.contains(app.pkg)
                        val isDangerous = BatteryCoreConfig.isDangerous(app.pkg)
                        
                        // 获取图标 (简单的转换)
                        val iconBitmap = remember(app.pkg) {
                            try {
                                context.packageManager.getApplicationIcon(app.pkg).toBitmap().asImageBitmap()
                            } catch (e: Exception) { null }
                        }

                        // MIUIX 卡片组件 (替换原来的 ViewHolder)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (iconBitmap != null) {
                                    Image(bitmap = iconBitmap, contentDescription = null, modifier = Modifier.size(40.dp))
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = app.name, style = MiuixTheme.textStyles.title4)
                                    Text(text = app.pkg, style = MiuixTheme.textStyles.footnote1, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                                }

                                // MIUIX 开关
                                Switch(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        if (checked && isDangerous) {
                                            Toast.makeText(context, "⚠️ 极高危应用，已禁止添加！", Toast.LENGTH_SHORT).show()
                                        } else {
                                            viewModel.toggleApp(app.pkg, checked)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// 简易的 EULA 同意页面
@Composable
fun EulaScreen(onAgree: () -> Unit) {
    val context = LocalContext.current
    val eulaText = remember { EulaManager.loadEulaFromAssets(context) ?: "协议加载失败" }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "用户许可协议", style = MiuixTheme.textStyles.title2, modifier = Modifier.padding(vertical = 16.dp))
        ScrollableText(text = eulaText, modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            text = "同意并继续",
            onClick = {
                val hash = SecurityUtils.sha256Hex(eulaText)
                context.getSharedPreferences(BatteryCoreConfig.PREFS_NAME, android.content.Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean(BatteryCoreConfig.KEY_AGREED, true)
                    .putString(BatteryCoreConfig.KEY_AGREED_HASH, hash)
                    .apply()
                onAgree()
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}