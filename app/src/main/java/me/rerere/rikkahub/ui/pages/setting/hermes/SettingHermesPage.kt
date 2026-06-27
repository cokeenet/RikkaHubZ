package me.rerere.rikkahub.ui.pages.setting.hermes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
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

    var baseUrlText by rememberSaveable { mutableStateOf(config.baseUrl) }
    var apiTokenText by rememberSaveable { mutableStateOf(config.apiToken) }
    var fallbackProviderText by rememberSaveable { mutableStateOf(config.fallbackProviderId) }
    var baseUrlFocused by remember { mutableStateOf(false) }
    var apiTokenFocused by remember { mutableStateOf(false) }
    var fallbackProviderFocused by remember { mutableStateOf(false) }
    var tokenVisible by remember { mutableStateOf(false) }

    LaunchedEffect(config.baseUrl, baseUrlFocused) {
        if (!baseUrlFocused && baseUrlText != config.baseUrl) {
            baseUrlText = config.baseUrl
        }
    }
    LaunchedEffect(config.apiToken, apiTokenFocused) {
        if (!apiTokenFocused && apiTokenText != config.apiToken) {
            apiTokenText = config.apiToken
        }
    }
    LaunchedEffect(config.fallbackProviderId, fallbackProviderFocused) {
        if (!fallbackProviderFocused && fallbackProviderText != config.fallbackProviderId) {
            fallbackProviderText = config.fallbackProviderId
        }
    }

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
            contentPadding = innerPadding + PaddingValues(start = 8.dp, top = 8.dp, end = 8.dp, bottom = 32.dp),
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
                        headlineContent = {
                            HermesInputItem(
                                leadingContent = { Icon(HugeIcons.Link02, null) },
                                title = "地址",
                                description = "电脑端 Hermes Bridge，例如 http://192.168.1.10:3001",
                                value = baseUrlText,
                                onValueChange = { value ->
                                    baseUrlText = value
                                    vm.setBaseUrl(value)
                                },
                                onFocusChanged = { baseUrlFocused = it },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                            )
                        },
                    )
                    item(
                        headlineContent = {
                            HermesInputItem(
                                leadingContent = { Icon(HugeIcons.Shield01, null) },
                                title = "Token",
                                description = "对应电脑端 Security:ApiToken",
                                value = apiTokenText,
                                onValueChange = { value ->
                                    apiTokenText = value
                                    vm.setApiToken(value)
                                },
                                onFocusChanged = { apiTokenFocused = it },
                                visualTransformation = if (tokenVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    androidx.compose.material3.IconButton(onClick = { tokenVisible = !tokenVisible }) {
                                        Icon(
                                            imageVector = if (tokenVisible) HugeIcons.ViewOff else HugeIcons.View,
                                            contentDescription = null,
                                        )
                                    }
                                },
                            )
                        },
                    )
                    item(
                        headlineContent = {
                            HermesInputItem(
                                leadingContent = { Icon(HugeIcons.ServerStack01, null) },
                                title = "Fallback Provider",
                                description = "电脑离线时手机端优先使用的 Provider ID，稍后接入聊天流程",
                                value = fallbackProviderText,
                                onValueChange = { value ->
                                    fallbackProviderText = value
                                    vm.setFallbackProviderId(value)
                                },
                                onFocusChanged = { fallbackProviderFocused = it },
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
                                        modifier = Modifier.size(18.dp),
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
private fun HermesInputItem(
    leadingContent: @Composable () -> Unit,
    title: String,
    description: String,
    value: String,
    onValueChange: (String) -> Unit,
    onFocusChanged: (Boolean) -> Unit = {},
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 6.dp)
                    .size(40.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = RoundedCornerShape(14.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                leadingContent()
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = description,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        TextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 54.dp)
                .onFocusChanged { onFocusChanged(it.isFocused) },
            textStyle = MaterialTheme.typography.bodyLarge,
            keyboardOptions = keyboardOptions,
            visualTransformation = visualTransformation,
            trailingIcon = trailingIcon,
            shape = CircleShape,
            colors = textFieldColors(),
        )
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
    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    disabledIndicatorColor = Color.Transparent,
)
