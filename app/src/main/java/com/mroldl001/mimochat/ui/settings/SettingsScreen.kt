package com.mroldl001.mimochat.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.hilt.navigation.compose.hiltViewModel
import com.mroldl001.mimochat.data.preferences.PreferencesManager
import com.mroldl001.mimochat.ui.chat.ApiKeyDialog
import com.mroldl001.mimochat.ui.chat.CustomSystemPromptDialog
import com.mroldl001.mimochat.ui.chat.ParameterSettingsDialog
import com.mroldl001.mimochat.ui.chat.components.*
import com.mroldl001.mimochat.ui.chat.viewmodel.ChatViewModel
import com.mroldl001.mimochat.ui.chat.viewmodel.UpdateUiState
import com.mroldl001.mimochat.ui.theme.CodeBlockColorMode
import com.mroldl001.mimochat.ui.theme.ThemeColor
import com.mroldl001.mimochat.ui.theme.ThemeMode
import com.mroldl001.mimochat.ui.theme.supportsDynamicColor
import com.mroldl001.mimochat.ui.theme.themePreviewColorScheme
import com.mroldl001.mimochat.R
import com.mroldl001.mimochat.ui.settings.AppLocale
import com.mroldl001.mimochat.ui.settings.LanguageSettingsDialog
import com.mroldl001.mimochat.domain.model.AIModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.drop

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onThemeChanged: (ThemeColor, ThemeMode) -> Unit,
    onCodeBlockColorModeChanged: (CodeBlockColorMode) -> Unit = {},
    onNavigateToDisclaimer: () -> Unit,
    onNavigateToExperimentalFeatures: () -> Unit,
    onNavigateToEasterEggHistory: () -> Unit,
    isExpandedScreen: Boolean = false,
    scrollState: ScrollState = rememberScrollState(),
    appLanguage: String = AppLocale.SYSTEM,
    onLanguageSelected: (String) -> Unit = {},
    viewModel: ChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showApiKey by remember { mutableStateOf(false) }
    var showBackground by remember { mutableStateOf(false) }
    var showParameters by remember { mutableStateOf(false) }
    var showPrompt by remember { mutableStateOf(false) }
    var showApiUrl by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    var showCustomColor by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showTitleModel by remember { mutableStateOf(false) }
    var customColorHex by remember { mutableStateOf(viewModel.getCustomThemeColorHex()) }

    LaunchedEffect(Unit) {
        viewModel.preferencesManager.revision.drop(1).collect {
            customColorHex = viewModel.getCustomThemeColorHex()
        }
    }
    var pendingCropUri by rememberSaveable { mutableStateOf<String?>(null) }

    val backgroundPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) pendingCropUri = uri.toString()
    }
    val pickBackground = {
        backgroundPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }
    val clearBackground = {
        deleteStoredChatBackground(context, uiState.chatBackgroundUri)
        viewModel.setChatBackgroundUri(null)
    }

    SettingsPageContent(
        themeColor = uiState.themeColor,
        themeMode = uiState.themeMode,
        onThemeChanged = { color, mode ->
            viewModel.setThemeColor(color)
            viewModel.setThemeMode(mode)
            onThemeChanged(color, mode)
        },
        onApiKeyClick = { showApiKey = true },
        onBackgroundImageClick = { showBackground = true },
        updateState = uiState.updateState,
        onCheckForUpdate = viewModel::checkForUpdate,
        onParameterSettingsClick = { showParameters = true },
        onCustomPromptClick = { showPrompt = true },
        onApiBaseUrlClick = { showApiUrl = true },
        onAboutClick = { showAbout = true },
        onEasterEggHistoryClick = onNavigateToEasterEggHistory,
        onNavigateBack = onNavigateBack,
        onExperimentalFeaturesClick = onNavigateToExperimentalFeatures,
        customColorHex = customColorHex,
        onCustomColorClicked = { showCustomColor = true },
        onLanguageClick = { showLanguageDialog = true },
        onTitleModelClick = { showTitleModel = true },
        titleModelId = uiState.titleModelId,
        availableModels = uiState.availableModels,
        codeBlockColorMode = uiState.codeBlockColorMode,
        onCodeBlockColorModeSelected = { mode ->
            viewModel.setCodeBlockColorMode(mode)
            onCodeBlockColorModeChanged(mode)
        },
        isExpandedScreen = isExpandedScreen,
        scrollState = scrollState,
        appLanguage = appLanguage,
        onLanguageSelected = onLanguageSelected,
        predictiveBack = uiState.predictiveBack,
        onPredictiveBackChanged = viewModel::setPredictiveBack
    )

    if (showApiKey) {
        ApiKeyDialog(
            currentKey = uiState.apiKey,
            onDismiss = { showApiKey = false },
            onConfirm = { viewModel.setApiKey(it); showApiKey = false }
        )
    }
    if (showBackground) {
        BackgroundImageSettingsDialog(
            hasBackgroundImage = !uiState.chatBackgroundUri.isNullOrBlank(),
            opacity = uiState.chatBackgroundOpacity,
            onSelectImage = pickBackground,
            onOpacityChanged = viewModel::setChatBackgroundOpacity,
            onRestoreDefault = clearBackground,
            onDismiss = { showBackground = false }
        )
    }
    pendingCropUri?.let { uriString ->
        BackgroundCropDialog(
            sourceUri = Uri.parse(uriString),
            onCropped = { croppedUri ->
                val previousUri = uiState.chatBackgroundUri
                viewModel.setChatBackgroundUri(croppedUri)
                if (previousUri != croppedUri) deleteStoredChatBackground(context, previousUri)
                pruneStoredChatBackgrounds(context, croppedUri)
                pendingCropUri = null
            },
            onDismiss = { pendingCropUri = null }
        )
    }
    if (showParameters) {
        ParameterSettingsDialog(
            initialTemperature = uiState.temperature,
            initialTopP = uiState.topP,
            initialFrequencyPenalty = uiState.frequencyPenalty,
            initialPresencePenalty = uiState.presencePenalty,
            onDismiss = { showParameters = false },
            onConfirm = { temperature, topP, frequencyPenalty, presencePenalty ->
                viewModel.setTemperature(temperature)
                viewModel.setTopP(topP)
                viewModel.setFrequencyPenalty(frequencyPenalty)
                viewModel.setPresencePenalty(presencePenalty)
                showParameters = false
            },
            onReset = { viewModel.resetParameters(); showParameters = false }
        )
    }
    if (showPrompt) {
        CustomSystemPromptDialog(
            currentPrompt = uiState.customSystemPrompt,
            onDismiss = { showPrompt = false },
            onConfirm = { viewModel.setCustomSystemPrompt(it); showPrompt = false }
        )
    }
    if (showApiUrl) {
        ApiBaseUrlDialog(
            currentUrl = uiState.apiBaseUrl,
            onDismiss = { showApiUrl = false },
            onConfirm = { viewModel.setApiBaseUrl(it); showApiUrl = false }
        )
    }
    if (showAbout) {
        AboutDialog(
            onDismiss = { showAbout = false },
            onDisclaimerClick = {
                showAbout = false
                onNavigateToDisclaimer()
            }
        )
    }
    if (showCustomColor) {
        CustomColorDialog(
            initialHex = viewModel.getCustomThemeColorHex(),
            onCancel = { showCustomColor = false },
            onSave = { hex ->
                viewModel.setCustomThemeColorHex(hex)
                customColorHex = hex
                onThemeChanged(ThemeColor.CUSTOM, uiState.themeMode)
                showCustomColor = false
            }
        )
    }
    if (showLanguageDialog) {
        LanguageSettingsDialog(
            currentLanguage = appLanguage,
            onLanguageSelected = { lang ->
                onLanguageSelected(lang)
                showLanguageDialog = false
            },
            onDismiss = { showLanguageDialog = false }
        )
    }
    if (showTitleModel) {
        TitleModelSettingsDialog(
            currentTitleModelId = uiState.titleModelId,
            models = uiState.availableModels,
            onTitleModelSelected = { id ->
                viewModel.setTitleModelId(id)
                showTitleModel = false
            },
            onDismiss = { showTitleModel = false }
        )
    }
    (uiState.updateState as? UpdateUiState.Available)?.let { update ->
        UpdateReleaseDialog(
            release = update.release,
            onDismiss = viewModel::clearUpdateState,
            onDownload = { viewModel.downloadUpdate(update.release) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsPageContent(
    themeColor: ThemeColor,
    themeMode: ThemeMode,
    onThemeChanged: (ThemeColor, ThemeMode) -> Unit,
    onApiKeyClick: () -> Unit,
    onBackgroundImageClick: () -> Unit,
    updateState: UpdateUiState,
    onCheckForUpdate: () -> Unit,
    onParameterSettingsClick: () -> Unit,
    onCustomPromptClick: () -> Unit,
    onApiBaseUrlClick: () -> Unit,
    onAboutClick: () -> Unit,
    onEasterEggHistoryClick: () -> Unit,
    onNavigateBack: () -> Unit,
    onExperimentalFeaturesClick: () -> Unit,
        customColorHex: String,
        onCustomColorClicked: () -> Unit,
        onLanguageClick: () -> Unit,
        onTitleModelClick: () -> Unit,
        titleModelId: String = PreferencesManager.DEFAULT_TITLE_MODEL_ID,
        availableModels: List<AIModel> = emptyList(),
    codeBlockColorMode: CodeBlockColorMode,
    onCodeBlockColorModeSelected: (CodeBlockColorMode) -> Unit,
    isExpandedScreen: Boolean,
    scrollState: ScrollState,
    appLanguage: String,
    onLanguageSelected: (String) -> Unit,
    predictiveBack: Boolean,
    onPredictiveBackChanged: (Boolean) -> Unit
) {
    val pageColor by animateColorAsState(
        targetValue = MaterialTheme.colorScheme.background,
        animationSpec = tween(durationMillis = 450),
        label = "settings_page_color"
    )
    val previewDark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.FOLLOW_SYSTEM -> isSystemInDarkTheme()
    }
    Scaffold(
        containerColor = pageColor,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    val backInteractionSource = remember { MutableInteractionSource() }
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = backInteractionSource,
                                indication = null,
                                onClick = onNavigateBack
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = pageColor,
                    titleContentColor = animateThemeColor(MaterialTheme.colorScheme.onSurface, "settings_title"),
                    navigationIconContentColor = animateThemeColor(MaterialTheme.colorScheme.onSurface, "settings_back_icon")
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.TopStart
        ) {
            val columnModifier = if (isExpandedScreen) {
                Modifier.fillMaxWidth()
            } else {
                Modifier.widthIn(max = 560.dp).fillMaxWidth()
            }
            Column(
                modifier = columnModifier
                    .verticalScroll(scrollState)
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                OverrideEasterEggBanner()

                SettingsGroupTitle(stringResource(R.string.group_appearance))

                SettingSectionHeader(Icons.Outlined.Brightness7, stringResource(R.string.display_mode))
                // 自定义色也要灌进显示模式预览，否则回退成默认黑
                val displayModeCustomHex = if (themeColor == ThemeColor.CUSTOM) customColorHex else null
                if (isExpandedScreen) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            ThemePreviewCard(
                                label = stringResource(R.string.preview_day),
                                scheme = themePreviewColorScheme(themeColor, dark = false, customColorHex = displayModeCustomHex),
                                selected = themeMode == ThemeMode.LIGHT,
                                width = 104.dp
                            ) { onThemeChanged(themeColor, ThemeMode.LIGHT) }
                        }
                        item {
                            ThemePreviewCard(
                                label = stringResource(R.string.preview_night),
                                scheme = themePreviewColorScheme(themeColor, dark = true, customColorHex = displayModeCustomHex),
                                selected = themeMode == ThemeMode.DARK,
                                width = 104.dp
                            ) { onThemeChanged(themeColor, ThemeMode.DARK) }
                        }
                        item {
                            ThemePreviewCard(
                                label = stringResource(R.string.preview_follow_system),
                                scheme = themePreviewColorScheme(themeColor, dark = false, customColorHex = displayModeCustomHex),
                                bottomScheme = themePreviewColorScheme(themeColor, dark = true, customColorHex = displayModeCustomHex),
                                selected = themeMode == ThemeMode.FOLLOW_SYSTEM,
                                width = 104.dp
                            ) { onThemeChanged(themeColor, ThemeMode.FOLLOW_SYSTEM) }
                        }
                    }
                } else {
                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        val fullRowWidth = this.maxWidth + 24.dp
                        val startPadding = ((this.maxWidth - 104.dp * 3) / 4).coerceAtLeast(0.dp)
                        LazyRow(
                            contentPadding = PaddingValues(start = startPadding),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.requiredWidth(fullRowWidth)
                        ) {
                            item {
                                ThemePreviewCard(
                                    label = stringResource(R.string.preview_day),
                                    scheme = themePreviewColorScheme(themeColor, dark = false, customColorHex = displayModeCustomHex),
                                    selected = themeMode == ThemeMode.LIGHT,
                                    width = 104.dp
                                ) { onThemeChanged(themeColor, ThemeMode.LIGHT) }
                            }
                            item {
                                ThemePreviewCard(
                                    label = stringResource(R.string.preview_night),
                                    scheme = themePreviewColorScheme(themeColor, dark = true, customColorHex = displayModeCustomHex),
                                    selected = themeMode == ThemeMode.DARK,
                                    width = 104.dp
                                ) { onThemeChanged(themeColor, ThemeMode.DARK) }
                            }
                            item {
                                ThemePreviewCard(
                                    label = stringResource(R.string.preview_follow_system),
                                    scheme = themePreviewColorScheme(themeColor, dark = false, customColorHex = displayModeCustomHex),
                                    bottomScheme = themePreviewColorScheme(themeColor, dark = true, customColorHex = displayModeCustomHex),
                                    selected = themeMode == ThemeMode.FOLLOW_SYSTEM,
                                    width = 104.dp
                                ) { onThemeChanged(themeColor, ThemeMode.FOLLOW_SYSTEM) }
                            }
                        }
                    }
                }

                SettingSectionHeader(Icons.Outlined.Palette, stringResource(R.string.theme_color))
                val colors = buildList {
                    add(ThemeColor.WHITE)
                    add(ThemeColor.CUSTOM)
                    if (supportsDynamicColor()) add(ThemeColor.AUTO_COLOR)
                    add(ThemeColor.MIKU_GREEN)
                    add(ThemeColor.TETO_RED)
                    add(ThemeColor.MIYOU_ORANGE)
                    add(ThemeColor.SILENCE_GREEN)
                    add(ThemeColor.GREEN)
                    add(ThemeColor.PURPLE)
                    add(ThemeColor.DEEP_BLUE)
                }
                if (isExpandedScreen) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(colors, key = { it.name }) { option ->
                            ThemePreviewCard(
                                label = themeColorLabel(option),
                                scheme = themePreviewColorScheme(option, dark = previewDark, customColorHex = if (option == ThemeColor.CUSTOM) customColorHex else null),
                                selected = themeColor == option,
                                width = 104.dp
                            ) { if (option == ThemeColor.CUSTOM) onCustomColorClicked() else onThemeChanged(option, themeMode) }
                        }
                    }
                } else {
                    // 与显示模式/代码块预览行的首卡左对齐：SpaceEvenly 的首卡左侧留白 = 剩余空间 / 4
                    // 行宽 = 内容区宽 + 24dp，顶到屏幕右缘消除右侧白色遮挡；用 requiredWidth 避免负边距导致的 minWidth<0 崩溃
                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        val fullRowWidth = this.maxWidth + 24.dp
                        val themeRowStartPadding = ((this.maxWidth - 104.dp * 3) / 4).coerceAtLeast(0.dp)
                        LazyRow(
                            contentPadding = PaddingValues(start = themeRowStartPadding),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.requiredWidth(fullRowWidth)
                        ) {
                            items(colors, key = { it.name }) { option ->
                                ThemePreviewCard(
                                    label = themeColorLabel(option),
                                    scheme = themePreviewColorScheme(option, dark = previewDark, customColorHex = if (option == ThemeColor.CUSTOM) customColorHex else null),
                                    selected = themeColor == option,
                                    width = 104.dp
                                ) { if (option == ThemeColor.CUSTOM) onCustomColorClicked() else onThemeChanged(option, themeMode) }
                            }
                        }
                    }
                }

                SettingSectionHeader(Icons.Outlined.Code, stringResource(R.string.code_block_color))
                if (isExpandedScreen) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            CodeBlockPreviewCard(
                                label = stringResource(R.string.code_block_dark),
                                dark = true,
                                selected = codeBlockColorMode == CodeBlockColorMode.DARK,
                                width = 104.dp
                            ) { onCodeBlockColorModeSelected(CodeBlockColorMode.DARK) }
                        }
                        item {
                            CodeBlockPreviewCard(
                                label = stringResource(R.string.code_block_light),
                                dark = false,
                                selected = codeBlockColorMode == CodeBlockColorMode.LIGHT,
                                width = 104.dp
                            ) { onCodeBlockColorModeSelected(CodeBlockColorMode.LIGHT) }
                        }
                        item {
                            CodeBlockPreviewCard(
                                label = stringResource(R.string.code_block_follow),
                                dark = previewDark,
                                split = true,
                                selected = codeBlockColorMode == CodeBlockColorMode.FOLLOW,
                                width = 104.dp
                            ) { onCodeBlockColorModeSelected(CodeBlockColorMode.FOLLOW) }
                        }
                    }
                } else {
                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        val fullRowWidth = this.maxWidth + 24.dp
                        val startPadding = ((this.maxWidth - 104.dp * 3) / 4).coerceAtLeast(0.dp)
                        LazyRow(
                            contentPadding = PaddingValues(start = startPadding),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.requiredWidth(fullRowWidth)
                        ) {
                            item {
                                CodeBlockPreviewCard(
                                    label = stringResource(R.string.code_block_dark),
                                    dark = true,
                                    selected = codeBlockColorMode == CodeBlockColorMode.DARK,
                                    width = 104.dp
                                ) { onCodeBlockColorModeSelected(CodeBlockColorMode.DARK) }
                            }
                            item {
                                CodeBlockPreviewCard(
                                    label = stringResource(R.string.code_block_light),
                                    dark = false,
                                    selected = codeBlockColorMode == CodeBlockColorMode.LIGHT,
                                    width = 104.dp
                                ) { onCodeBlockColorModeSelected(CodeBlockColorMode.LIGHT) }
                            }
                            item {
                                CodeBlockPreviewCard(
                                    label = stringResource(R.string.code_block_follow),
                                    dark = previewDark,
                                    split = true,
                                    selected = codeBlockColorMode == CodeBlockColorMode.FOLLOW,
                                    width = 104.dp
                                ) { onCodeBlockColorModeSelected(CodeBlockColorMode.FOLLOW) }
                            }
                        }
                    }
                }

                SettingAction(Icons.Outlined.Image, stringResource(R.string.chat_background), stringResource(R.string.chat_background_desc), onBackgroundImageClick)

                SettingsGroupTitle(stringResource(R.string.group_api))

                SettingAction(Icons.Outlined.VpnKey, stringResource(R.string.api_key), stringResource(R.string.api_key_desc), onApiKeyClick)
                SettingAction(Icons.Outlined.Link, stringResource(R.string.api_base_url), stringResource(R.string.api_base_url_desc), onApiBaseUrlClick)

                SettingsGroupTitle(stringResource(R.string.group_experience))

                SettingAction(
                    Icons.Outlined.Translate,
                    stringResource(R.string.language),
                    AppLocale.label(appLanguage),
                    onClick = onLanguageClick
                )
                val titleModelLabel = if (titleModelId == PreferencesManager.DEFAULT_TITLE_MODEL_ID) {
                    stringResource(R.string.title_model_follow)
                } else {
                    availableModels.find { it.id == titleModelId }?.name ?: titleModelId
                }
                SettingAction(
                    Icons.Outlined.Title,
                    stringResource(R.string.title_model),
                    titleModelLabel,
                    onClick = onTitleModelClick
                )
                SettingAction(Icons.Outlined.Chat, stringResource(R.string.custom_system_prompt), stringResource(R.string.custom_system_prompt_desc), onCustomPromptClick)
                SettingAction(Icons.Outlined.Tune, stringResource(R.string.parameter_settings), stringResource(R.string.parameter_settings_desc), onParameterSettingsClick)

                if (android.os.Build.VERSION.SDK_INT >= 34) {
                    SettingSwitchRow(
                        Icons.AutoMirrored.Outlined.ArrowBack,
                        stringResource(R.string.predictive_back_title),
                        stringResource(R.string.predictive_back_desc),
                        predictiveBack,
                        onPredictiveBackChanged
                    )
                }

                SettingsGroupTitle(stringResource(R.string.group_other))

                SettingAction(Icons.Outlined.Science, stringResource(R.string.experimental_features), stringResource(R.string.experimental_features_desc), onExperimentalFeaturesClick)
                UpdateSettingsItem(updateState, onCheckForUpdate)
                SettingAction(Icons.Outlined.Egg, stringResource(R.string.history_easter_egg), stringResource(R.string.history_easter_egg_desc), onEasterEggHistoryClick)
                SettingAction(Icons.Outlined.Info, stringResource(R.string.about_mchat), stringResource(R.string.about_mchat_desc), onAboutClick)
            }
        }
    }
}

private val OVERRIDE_EASTER_EGG = LegacyEasterEgg(
    versionNumber = "V2.4.x",
    versionSubtitle = "Override",
    descriptionRes = R.string.easter_egg_override_desc,
    link = "https://y.music.163.com/m/song?id=2105374808",
    cover = CoverSource.Netease(2105374808L)
)

@Composable
private fun OverrideEasterEggBanner() {
    val context = LocalContext.current
    val coverUrl by produceState<String?>(initialValue = null, OVERRIDE_EASTER_EGG.cover) {
        value = fetchCoverUrl(OVERRIDE_EASTER_EGG.cover)
    }
    EasterEggBanner(
        versionNumber = OVERRIDE_EASTER_EGG.versionNumber,
        versionSubtitle = OVERRIDE_EASTER_EGG.versionSubtitle,
        description = stringResource(OVERRIDE_EASTER_EGG.descriptionRes),
        coverUrl = coverUrl,
        onClick = {
            runCatching {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse(OVERRIDE_EASTER_EGG.link))
                )
            }
        }
    )
}

