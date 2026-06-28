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
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.rerere.hugeicons.HugeIcons
import me.rerere.hugeicons.stroke.Database02
import me.rerere.hugeicons.stroke.Delete01
import me.rerere.hugeicons.stroke.Download01
import me.rerere.hugeicons.stroke.Home01
import me.rerere.hugeicons.stroke.Link02
import me.rerere.hugeicons.stroke.Refresh03
import me.rerere.hugeicons.stroke.ServerStack01
import me.rerere.hugeicons.stroke.Shield01
import me.rerere.hugeicons.stroke.View
import me.rerere.hugeicons.stroke.ViewOff
import me.rerere.rikkahub.ui.components.nav.BackButton
import me.rerere.rikkahub.ui.components.ui.CardGroup
import me.rerere.rikkahub.ui.shortcut.AssistantShortcutInstaller
import me.rerere.rikkahub.ui.theme.CustomColors
import me.rerere.rikkahub.utils.plus
import org.koin.androidx.compose.koinViewModel
import java.text.DateFormat
import java.util.Date

@Composable
fun SettingHermesPage(
    vm: SettingHermesViewModel = koinViewModel(),
) {
    val config by vm.config.collectAsStateWithLifecycle()
    val probeState by vm.probeState.collectAsStateWithLifecycle()
    val syncState by vm.syncState.collectAsStateWithLifecycle()
    val cacheState by vm.cacheState.collectAsStateWithLifecycle()
    val hermesAssistant by vm.hermesAssistant.collectAsStateWithLifecycle()
    val routeState by vm.routeState.collectAsStateWithLifecycle()
    val bridgeSyncState by vm.bridgeSyncState.collectAsStateWithLifecycle()
    val memoryQueueState by vm.memoryQueueState.collectAsStateWithLifecycle()
    val memoryMutationState by vm.memoryMutationState.collectAsStateWithLifecycle()
    val memoryUploadState by vm.memoryUploadState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    var baseUrlText by rememberSaveable { mutableStateOf(config.baseUrl) }
    var apiTokenText by rememberSaveable { mutableStateOf(config.apiToken) }
    var fallbackProviderText by rememberSaveable { mutableStateOf(config.fallbackProviderId) }
    var memoryTargetIdText by rememberSaveable { mutableStateOf("mobile.md") }
    var memoryBaseHashText by rememberSaveable { mutableStateOf("") }
    var memoryContentText by rememberSaveable { mutableStateOf("") }
    var baseUrlFocused by remember { mutableStateOf(false) }
    var apiTokenFocused by remember { mutableStateOf(false) }
    var fallbackProviderFocused by remember { mutableStateOf(false) }
    var tokenVisible by remember { mutableStateOf(false) }
    val context = LocalContext.current

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
                    title = { Text("Hermes 助手") },
                ) {
                    item(
                        leadingContent = { Icon(HugeIcons.ServerStack01, null) },
                        headlineContent = { Text(hermesAssistant.name.ifBlank { "Hermes" }) },
                        supportingContent = {
                            Text(
                                "独立助手 ID: ${hermesAssistant.id}\n" +
                                    "头像跟随助手: ${if (hermesAssistant.useAssistantAvatar) "是" else "否"}"
                            )
                        },
                        trailingContent = {
                            OutlinedButton(
                                onClick = {
                                    AssistantShortcutInstaller.requestPinShortcut(context, hermesAssistant)
                                },
                            ) {
                                Icon(HugeIcons.Home01, null)
                            }
                        },
                    )
                    item(
                        leadingContent = { Icon(HugeIcons.Refresh03, null) },
                        headlineContent = { Text(routeState.title) },
                        supportingContent = { Text(routeState.diagnostic) },
                    )
                }
            }

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

            item {
                CardGroup(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    title = { Text("离线分身缓存") },
                ) {
                    item(
                        leadingContent = { Icon(HugeIcons.Download01, null) },
                        headlineContent = { Text("同步到手机") },
                        supportingContent = { Text("保存电脑端 Hermes 的人格和记忆，用于电脑离线后的手机侧回答") },
                        trailingContent = {
                            Button(
                                onClick = vm::syncToPhone,
                                enabled = syncState !is HermesSyncUiState.Loading,
                            ) {
                                if (syncState is HermesSyncUiState.Loading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                    )
                                } else {
                                    Icon(HugeIcons.Refresh03, null)
                                }
                            }
                        },
                    )
                    item(
                        leadingContent = { Icon(HugeIcons.Database02, null) },
                        headlineContent = {
                            Text(if (cacheState.isUsable) "缓存可用" else "暂无可用缓存")
                        },
                        supportingContent = {
                            Text(
                                "来源: ${cacheState.sourceBaseUrl.ifBlank { "未同步" }}\n" +
                                    "上次同步: ${formatMillis(cacheState.lastSyncedAt)}\n" +
                                    "人格: ${if (cacheState.personalityExists) "已缓存" else "未缓存"}\n" +
                                    "记忆: ${cacheState.memoryCount} 条"
                            )
                        },
                    )
                    item(
                        leadingContent = { Icon(HugeIcons.Delete01, null) },
                        headlineContent = { Text("清除手机缓存") },
                        supportingContent = { Text("只删除手机侧 Hermes 快照，不会修改电脑端数据") },
                        trailingContent = {
                            OutlinedButton(
                                onClick = vm::clearCache,
                                enabled = cacheState.isUsable && syncState !is HermesSyncUiState.Loading,
                            ) {
                                Text("清除")
                            }
                        },
                    )
                }
            }

            item {
                SyncResult(state = syncState)
            }

            item {
                CardGroup(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    title = { Text("电脑端同步") },
                ) {
                    item(
                        leadingContent = { Icon(HugeIcons.ServerStack01, null) },
                        headlineContent = { Text("Bridge 同步状态") },
                        supportingContent = { Text("查看电脑端最近一次同步阶段、结果和错误") },
                        trailingContent = {
                            OutlinedButton(
                                onClick = vm::refreshBridgeSyncStatus,
                                enabled = bridgeSyncState !is HermesBridgeSyncUiState.Loading &&
                                    bridgeSyncState !is HermesBridgeSyncUiState.RunningAction,
                            ) {
                                Text("刷新")
                            }
                        },
                    )
                    item(
                        leadingContent = { Icon(HugeIcons.Refresh03, null) },
                        headlineContent = { Text("触发电脑同步") },
                        supportingContent = { Text("请求电脑端 Bridge 立即执行一次同步，不会拉取到手机缓存") },
                        trailingContent = {
                            Button(
                                onClick = vm::triggerBridgeSync,
                                enabled = bridgeSyncState !is HermesBridgeSyncUiState.Loading &&
                                    bridgeSyncState !is HermesBridgeSyncUiState.RunningAction,
                            ) {
                                if (bridgeSyncState is HermesBridgeSyncUiState.RunningAction) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                    )
                                } else {
                                    Text("触发")
                                }
                            }
                        },
                    )
                }
            }

            item {
                BridgeSyncResult(state = bridgeSyncState)
            }

            item {
                CardGroup(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    title = { Text("手机记忆队列") },
                ) {
                    item(
                        leadingContent = { Icon(HugeIcons.Database02, null) },
                        headlineContent = { Text("新增待上传记忆") },
                        supportingContent = {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                HermesInputItem(
                                    leadingContent = { Icon(HugeIcons.Database02, null) },
                                    title = "Target ID",
                                    description = "电脑端记忆文件 ID，例如 mobile.md 或 daily.md",
                                    value = memoryTargetIdText,
                                    onValueChange = { memoryTargetIdText = it },
                                )
                                HermesInputItem(
                                    leadingContent = { Icon(HugeIcons.Shield01, null) },
                                    title = "Base Hash",
                                    description = "可选；留空表示直接导入，填写后用于冲突检测",
                                    value = memoryBaseHashText,
                                    onValueChange = { memoryBaseHashText = it },
                                )
                                HermesTextAreaItem(
                                    title = "内容",
                                    description = "手机离线期间产生的新记忆内容",
                                    value = memoryContentText,
                                    onValueChange = { memoryContentText = it },
                                )
                            }
                        },
                        trailingContent = {
                            Button(
                                onClick = {
                                    vm.createMemoryMutation(
                                        targetId = memoryTargetIdText,
                                        content = memoryContentText,
                                        baseHash = memoryBaseHashText,
                                    )
                                },
                                enabled = memoryMutationState !is HermesMemoryMutationUiState.Loading,
                            ) {
                                if (memoryMutationState is HermesMemoryMutationUiState.Loading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                    )
                                } else {
                                    Text("加入")
                                }
                            }
                        },
                    )
                    item(
                        leadingContent = { Icon(HugeIcons.Refresh03, null) },
                        headlineContent = { Text("上传待处理记忆") },
                        supportingContent = {
                            Text(
                                "总数: ${memoryQueueState.summary.total}\n" +
                                    "待上传: ${memoryQueueState.summary.pending}，失败待重试: ${memoryQueueState.summary.failed}\n" +
                                    "已导入: ${memoryQueueState.summary.imported}，冲突: ${memoryQueueState.summary.conflict}"
                            )
                        },
                        trailingContent = {
                            Button(
                                onClick = vm::uploadMemoryMutations,
                                enabled = memoryQueueState.summary.hasRetryable &&
                                    memoryUploadState !is HermesMemoryUploadUiState.Loading,
                            ) {
                                if (memoryUploadState is HermesMemoryUploadUiState.Loading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                    )
                                } else {
                                    Text("上传")
                                }
                            }
                        },
                    )
                    item(
                        leadingContent = { Icon(HugeIcons.Delete01, null) },
                        headlineContent = { Text("清理已导入记录") },
                        supportingContent = { Text("只清理手机侧队列中已成功导入的 mutation") },
                        trailingContent = {
                            OutlinedButton(
                                onClick = vm::clearImportedMemoryMutations,
                                enabled = memoryQueueState.summary.imported > 0,
                            ) {
                                Text("清理")
                            }
                        },
                    )
                }
            }

            item {
                MemoryQueueResult(
                    mutationState = memoryMutationState,
                    uploadState = memoryUploadState,
                    queueState = memoryQueueState,
                )
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
private fun HermesTextAreaItem(
    title: String,
    description: String,
    value: String,
    onValueChange: (String) -> Unit,
    onFocusChanged: (Boolean) -> Unit = {},
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
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
        TextField(
            value = value,
            onValueChange = onValueChange,
            minLines = 3,
            maxLines = 8,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 104.dp)
                .onFocusChanged { onFocusChanged(it.isFocused) },
            textStyle = MaterialTheme.typography.bodyLarge,
            shape = RoundedCornerShape(22.dp),
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
private fun BridgeSyncResult(state: HermesBridgeSyncUiState) {
    when (state) {
        HermesBridgeSyncUiState.Idle -> {
            ListItem(
                headlineContent = { Text("尚未读取电脑端同步状态") },
                supportingContent = { Text("点击刷新状态或触发电脑同步") },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            )
        }

        HermesBridgeSyncUiState.Loading -> {
            ListItem(
                headlineContent = { Text("正在读取电脑端同步状态") },
                supportingContent = { Text("请稍候") },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            )
        }

        is HermesBridgeSyncUiState.RunningAction -> {
            ListItem(
                headlineContent = { Text("正在触发电脑同步") },
                supportingContent = { Text(state.lastKnown?.diagnosticMessage ?: "请求已发送") },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            )
        }

        is HermesBridgeSyncUiState.Available -> {
            BridgeSyncStatusItem(
                status = state.status,
                actionMessage = state.actionMessage,
            )
        }

        is HermesBridgeSyncUiState.Error -> {
            ListItem(
                headlineContent = {
                    Text(
                        text = "电脑端同步状态读取失败",
                        color = MaterialTheme.colorScheme.error,
                    )
                },
                supportingContent = {
                    Text(
                        state.message +
                            (state.lastKnown?.let { "\n上次已知状态: ${it.headline}，${it.diagnosticMessage}" } ?: "")
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            )
        }
    }
}

@Composable
private fun BridgeSyncStatusItem(
    status: BridgeSyncStatusUi,
    actionMessage: String?,
) {
    ListItem(
        headlineContent = {
            Text(
                text = status.headline,
                color = if (status.lastSucceeded == false) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                },
            )
        },
        supportingContent = {
            val phaseText = status.lastPhaseResults
                .takeLast(4)
                .joinToString(separator = "\n") { phase ->
                    "${phase.phase}: ${if (phase.success) "成功" else "失败"} ${phase.message}".trim()
                }
            Text(
                buildString {
                    actionMessage?.let {
                        appendLine(it)
                    }
                    appendLine(status.diagnosticMessage)
                    appendLine("触发来源: ${status.lastTrigger.ifBlank { "未知" }}")
                    appendLine("开始时间: ${status.lastStartedAtUtc ?: "未知"}")
                    if (phaseText.isNotBlank()) {
                        appendLine("阶段结果:")
                        append(phaseText)
                    }
                }.trim()
            )
        },
        leadingContent = { Icon(HugeIcons.ServerStack01, null) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}

@Composable
private fun SyncResult(state: HermesSyncUiState) {
    when (state) {
        HermesSyncUiState.Idle -> {
            ListItem(
                headlineContent = { Text("等待同步") },
                supportingContent = { Text("电脑在线时点击同步到手机") },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            )
        }

        HermesSyncUiState.Loading -> {
            ListItem(
                headlineContent = { Text("正在同步") },
                supportingContent = { Text("正在拉取人格和记忆") },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            )
        }

        is HermesSyncUiState.Success -> {
            ListItem(
                headlineContent = {
                    Text(
                        text = "同步成功",
                        color = MaterialTheme.colorScheme.primary,
                    )
                },
                supportingContent = {
                    Text(
                        "同步时间: ${formatMillis(state.syncedAtMillis)}\n" +
                            "人格: ${if (state.personalityExists) "已缓存" else "未缓存"}\n" +
                            "记忆: ${state.memoryCount} 条"
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            )
        }

        is HermesSyncUiState.Error -> {
            ListItem(
                headlineContent = {
                    Text(
                        text = "同步失败",
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
private fun MemoryQueueResult(
    mutationState: HermesMemoryMutationUiState,
    uploadState: HermesMemoryUploadUiState,
    queueState: HermesMemoryQueueUiState,
) {
    val latestMutations = queueState.mutations.takeLast(4).asReversed()
    val message = buildString {
        when (mutationState) {
            HermesMemoryMutationUiState.Idle -> Unit
            HermesMemoryMutationUiState.Loading -> appendLine("正在加入记忆队列")
            is HermesMemoryMutationUiState.Success -> appendLine("已加入队列: ${mutationState.mutationId.take(8)}")
            is HermesMemoryMutationUiState.Error -> appendLine("加入失败: ${mutationState.message}")
        }

        when (uploadState) {
            HermesMemoryUploadUiState.Idle -> Unit
            HermesMemoryUploadUiState.Loading -> appendLine("正在上传待处理记忆")
            is HermesMemoryUploadUiState.Success -> appendLine(
                "上传完成: 尝试 ${uploadState.summary.attempted}，导入 ${uploadState.summary.imported}，冲突 ${uploadState.summary.conflict}，失败 ${uploadState.summary.failed}"
            )
            is HermesMemoryUploadUiState.Error -> appendLine("上传失败: ${uploadState.message}")
        }

        queueState.summary.latestError?.let { appendLine("最近错误: $it") }

        if (latestMutations.isNotEmpty()) {
            appendLine("最近记录:")
            latestMutations.forEach { mutation ->
                appendLine(
                    "${mutation.mutationId.take(8)} ${memoryStatusLabel(mutation.status)} -> ${mutation.targetId}"
                )
            }
        }
    }.trim()

    ListItem(
        headlineContent = {
            Text(
                text = if (queueState.summary.total > 0) "记忆队列已就绪" else "记忆队列为空",
                color = if (queueState.summary.conflict > 0 || queueState.summary.failed > 0) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                },
            )
        },
        supportingContent = { Text(message.ifBlank { "手机离线产生的新记忆会先进入本地队列" }) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}

private fun memoryStatusLabel(status: me.rerere.rikkahub.hermes.HermesMemoryMutationStatus): String = when (status) {
    me.rerere.rikkahub.hermes.HermesMemoryMutationStatus.Pending -> "待上传"
    me.rerere.rikkahub.hermes.HermesMemoryMutationStatus.Uploading -> "上传中"
    me.rerere.rikkahub.hermes.HermesMemoryMutationStatus.Imported -> "已导入"
    me.rerere.rikkahub.hermes.HermesMemoryMutationStatus.Conflict -> "冲突"
    me.rerere.rikkahub.hermes.HermesMemoryMutationStatus.Failed -> "失败"
}

private fun formatMillis(value: Long?): String {
    if (value == null || value <= 0L) return "未同步"
    return DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(value))
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
