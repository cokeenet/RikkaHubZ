package me.rerere.rikkahub.ui.pages.setting.hermes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.rerere.hugeicons.HugeIcons
import me.rerere.hugeicons.stroke.Database02
import me.rerere.hugeicons.stroke.Link02
import me.rerere.hugeicons.stroke.ServerStack01
import me.rerere.hugeicons.stroke.Shield01
import me.rerere.hugeicons.stroke.View
import me.rerere.hugeicons.stroke.ViewOff
import me.rerere.rikkahub.ui.components.nav.BackButton
import me.rerere.rikkahub.ui.components.ui.CardGroup
import me.rerere.rikkahub.ui.theme.CustomColors
import me.rerere.rikkahub.utils.plus
import org.koin.androidx.compose.koinViewModel

@Composable
fun SettingHermesPage(
    vm: SettingHermesViewModel = koinViewModel(),
) {
    val config by vm.config.collectAsStateWithLifecycle()
    val probeState by vm.probeState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    var tokenVisible by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("Hermes") },
                navigationIcon = { BackButton() },
                scrollBehavior = scrollBehavior,
                colors = CustomColors.topBarColors,
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = CustomColors.topBarColors.containerColor,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = innerPadding + PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                CardGroup(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    title = { Text("Bridge") },
                ) {
                    item(
                        leadingContent = { Icon(HugeIcons.Link02, null) },
                        headlineContent = { Text("地址") },
                        supportingContent = { Text("电脑端 Hermes Bridge，例如 http://192.168.1.10:3001") },
                        trailingContent = {
                            TextField(
                                value = config.baseUrl,
                                onValueChange = vm::setBaseUrl,
                                singleLine = true,
                                modifier = Modifier.width(230.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                                shape = CircleShape,
                                colors = textFieldColors(),
                            )
                        },
                    )
                    item(
                        leadingContent = { Icon(HugeIcons.Shield01, null) },
                        headlineContent = { Text("Token") },
                        supportingContent = { Text("对应电脑端 Security:ApiToken") },
                        trailingContent = {
                            TextField(
                                value = config.apiToken,
                                onValueChange = vm::setApiToken,
                                singleLine = true,
                                modifier = Modifier.width(230.dp),
                                visualTransformation = if (tokenVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    androidx.compose.material3.IconButton(onClick = { tokenVisible = !tokenVisible }) {
                                        Icon(
                                            imageVector = if (tokenVisible) HugeIcons.ViewOff else HugeIcons.View,
                                            contentDescription = null,
                                        )
                                    }
                                },
                                shape = CircleShape,
                                colors = textFieldColors(),
                            )
                        },
                    )
                    item(
                        leadingContent = { Icon(HugeIcons.ServerStack01, null) },
                        headlineContent = { Text("Fallback Provider") },
                        supportingContent = { Text("电脑离线时手机端优先使用的 Provider ID，稍后接入聊天流程") },
                        trailingContent = {
                            TextField(
                                value = config.fallbackProviderId,
                                onValueChange = vm::setFallbackProviderId,
                                singleLine = true,
                                modifier = Modifier.width(230.dp),
                                shape = CircleShape,
                                colors = textFieldColors(),
                            )
                        },
                    )
                }
            }

            item {
                CardGroup(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    title = { Text("连接测试") },
                ) {
                    item(
                        headlineContent = { Text("拉取电脑端状态、人格和记忆") },
                        supportingContent = { Text("只测试连接和读取，不会修改电脑端数据") },
                        trailingContent = {
                            Button(
                                onClick = vm::testConnection,
                                enabled = probeState !is HermesProbeUiState.Loading,
                            ) {
                                if (probeState is HermesProbeUiState.Loading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.width(18.dp),
                                        strokeWidth = 2.dp,
                                    )
                                } else {
                                    Text("测试")
                                }
                            }
                        },
                    )
                }
            }

            item {
                ProbeResult(state = probeState)
            }
        }
    }
}

@Composable
private fun ProbeResult(state: HermesProbeUiState) {
    when (state) {
        HermesProbeUiState.Idle -> {
            ListItem(
                headlineContent = { Text("尚未测试") },
                supportingContent = { Text("保存地址和 Token 后点击测试") },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            )
        }

        HermesProbeUiState.Loading -> {
            ListItem(
                headlineContent = { Text("正在连接") },
                supportingContent = { Text("请稍候") },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            )
        }

        is HermesProbeUiState.Success -> {
            Column {
                ListItem(
                    headlineContent = {
                        Text(
                            text = "连接成功",
                            color = MaterialTheme.colorScheme.primary,
                        )
                    },
                    supportingContent = {
                        Text(
                            "服务: ${state.service}\n" +
                                "数据目录: ${state.dataRoot}\n" +
                                "目录存在: ${if (state.dataRootExists) "是" else "否"}\n" +
                                "人格文件: ${if (state.personalityExists) "已找到" else "未找到"}\n" +
                                "记忆数量: ${state.memoryCount}"
                        )
                    },
                    leadingContent = { Icon(HugeIcons.Database02, null) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
            }
        }

        is HermesProbeUiState.Error -> {
            ListItem(
                headlineContent = {
                    Text(
                        text = "连接失败",
                        color = MaterialTheme.colorScheme.error,
                    )
                },
                supportingContent = { Text(state.message) },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            )
        }
    }
}

@Composable
private fun textFieldColors() = TextFieldDefaults.colors(
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    disabledIndicatorColor = Color.Transparent,
)
