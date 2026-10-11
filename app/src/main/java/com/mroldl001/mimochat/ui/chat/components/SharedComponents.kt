package com.mroldl001.mimochat.ui.chat.components

import com.mroldl001.mimochat.R
import android.content.Intent
import android.net.Uri
import android.text.util.Linkify
import android.widget.Toast
import androidx.compose.ui.res.stringResource
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.jeziellago.compose.markdowntext.MarkdownText
import com.mroldl001.mimochat.domain.model.WebSearchResult
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
private fun CollapsibleCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }

    val rotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "arrow_rotation"
    )

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null
                    ) { isExpanded = !isExpanded }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = if (isExpanded) stringResource(R.string.collapse) else stringResource(R.string.expand),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(18.dp)
                        .rotate(rotation)
                        .offset {
                            IntOffset(0, (-2).dp.roundToPx())
                        }
                )
            }

            if (isExpanded) {
                content()
            }
        }
    }
}

@Composable
fun ThinkingCard(
    reasoningContent: String,
    modifier: Modifier = Modifier
) {
    CollapsibleCard(
        icon = Icons.Default.Psychology,
        title = stringResource(R.string.thinking_process),
        modifier = modifier
    ) {
        Text(
            text = reasoningContent,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .padding(bottom = 12.dp)
        )
    }
}

@Composable
fun SearchResultsCard(
    searchResults: List<WebSearchResult>,
    modifier: Modifier = Modifier
) {
    CollapsibleCard(
        icon = Icons.Default.Search,
        title = stringResource(R.string.search_results),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .padding(bottom = 12.dp)
        ) {
            searchResults.forEachIndexed { index, result ->
                if (index > 0) {
                    Spacer(modifier = Modifier.height(6.dp))
                }
                SearchResultItem(result = result)
            }
        }
    }
}

@Composable
private fun SearchResultItem(result: WebSearchResult) {
    val context = LocalContext.current
    val faviconUrl = remember(result.logoUrl, result.url) {
        result.logoUrl?.takeIf { it.isNotBlank() } ?: runCatching {
            val host = Uri.parse(result.url).host?.removePrefix("www.")
            host?.let { "https://icons.duckduckgo.com/ip3/$it.ico" }
        }.getOrNull()
    }
    var faviconLoadFailed by remember(faviconUrl) { mutableStateOf(false) }
    val hostName = remember(result.url) {
        runCatching { Uri.parse(result.url).host?.removePrefix("www.") }.getOrNull()
    }
    val sourceName = result.siteName?.takeIf { it.isNotBlank() } ?: hostName
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(result.url))
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(context, context.getString(R.string.toast_link_failed), Toast.LENGTH_SHORT).show()
                }
            }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp)
        ) {
            if (faviconUrl != null && !faviconLoadFailed) {
                AsyncImage(
                    model = faviconUrl,
                    contentDescription = sourceName,
                    contentScale = ContentScale.Fit,
                    onError = { faviconLoadFailed = true },
                    modifier = Modifier.size(36.dp).padding(3.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .padding(3.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp))
                )
            }
            Spacer(modifier = Modifier.width(7.dp))
            Column(modifier = Modifier.weight(1f)) {
                if (sourceName != null) {
                    Text(
                        text = sourceName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.primaryContainer,
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                }
                Text(
                    text = result.title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun MixedMarkdownLatex(
    text: String,
    textColor: Color,
    isStreaming: Boolean = false,
    compact: Boolean = false,
    onLatexWidthMeasured: ((Float) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val inlineCodeBackground by rememberUpdatedState(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f).toArgb())
    val inlineCodeText by rememberUpdatedState(MaterialTheme.colorScheme.primary.toArgb())
    val markdownLinkColor = MaterialTheme.colorScheme.primary
    val markdownSelectionColors = androidx.compose.foundation.text.selection.TextSelectionColors(
        handleColor = MaterialTheme.colorScheme.primary,
        backgroundColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
    )
    val latestText by rememberUpdatedState(text)
    var renderedText by remember {
        mutableStateOf(if (isStreaming) stabilizeStreamingMarkdown(text) else text)
    }
    LaunchedEffect(text, isStreaming) {
        if (!isStreaming) renderedText = text
    }
    LaunchedEffect(isStreaming) {
        if (!isStreaming) return@LaunchedEffect
        while (true) {
            val stableText = stabilizeStreamingMarkdown(latestText)
            if (stableText != renderedText) renderedText = stableText
            delay(80)
        }
    }
    val segments by remember(renderedText) {
        derivedStateOf { splitContent(normalizeHtmlToMarkdown(renderedText), allowUnclosedCodeFence = true) }
    }

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        segments.forEach { segment ->
            when (segment.type) {
                ContentType.MARKDOWN -> {
                    val markdown = remember(segment.content) {
                        normalizeEscapedEmphasis(segment.content)
                    }
                    key(markdownLinkColor, textColor) {
                    MarkdownText(
                        markdown = markdown,
                        modifier = Modifier.fillMaxWidth(),
                        linkColor = markdownLinkColor,
                        linkifyMask = Linkify.WEB_URLS or Linkify.EMAIL_ADDRESSES,
                        textSelectionColors = markdownSelectionColors,
                        style = TextStyle(
                            color = textColor,
                            fontSize = 15.sp,
                            lineHeight = 22.sp
                        ),
                        isTextSelectable = true,
                        afterSetMarkdown = { textView ->
                            applyInlineCodeStyle(
                                textView,
                                inlineCodeBackground,
                                inlineCodeText,
                                markdown
                            )
                        }
                    )
                    }
                }
                ContentType.TABLE -> {
                    Box(modifier = Modifier.padding(vertical = 12.dp)) {
                        MarkdownTableView(
                            markdown = segment.content,
                            textColor = textColor
                        )
                    }
                }
                ContentType.CODE_BLOCK -> {
                    CodeBlockView(code = segment.content, info = segment.info)
                }
                ContentType.LATEX_BLOCK -> {
                    LatexBlockImage(
                        latex = segment.content,
                        textColor = textColor,
                        compact = compact,
                        onWidthMeasured = onLatexWidthMeasured
                    )
                }
                ContentType.LATEX_INLINE -> {
                    LatexInlineLine(
                        line = segment.content,
                        textColor = textColor,
                        onWidthMeasured = onLatexWidthMeasured
                    )
                }
                ContentType.QUOTE -> {
                    QuoteBlockView(content = segment.content, textColor = textColor)
                }
                ContentType.HR -> {
                    HrView()
                }
                else -> {}
            }
        }
    }
}

@Composable
private fun QuoteBlockView(
    content: String,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    val quoteBar = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(1.5.dp))
                .background(quoteBar)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Box(modifier = Modifier.weight(1f)) {
            MixedMarkdownLatex(text = content, textColor = textColor)
        }
    }
}

@Composable
private fun HrView() {
    val hrColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(hrColor)
        )
    }
}

@Composable
private fun MarkdownTableView(
    markdown: String,
    textColor: Color
) {
    val rows = remember(markdown) {
        markdown.lineSequence()
            .map { it.removeSuffix("\r").trim() }
            .filter { it.isNotBlank() && !isMarkdownTableDelimiter(it) }
            .map { line ->
                line.removePrefix("|")
                    .removeSuffix("|")
                    .split(UNESCAPED_PIPE)
                    .map(String::trim)
            }
            .toList()
    }

    if (rows.isEmpty()) return

    val tableShape = RoundedCornerShape(10.dp)
    val borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.58f)
    val headerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    val maxColumnCount = rows.maxOf { it.size }
    val renderedFormulaWidths = remember(markdown) { mutableStateMapOf<Int, Float>() }
    val columnWidths = List(maxColumnCount) { columnIndex ->
        val widestCell = rows.fold(0) { widest, row ->
            val cellText = stripLatexForTableWidth(row.getOrNull(columnIndex).orEmpty())
            val cellWidth = cellText.fold(0) { width, character ->
                width + if (character.code > 0x7F) 14 else 8
            }
            maxOf(widest, cellWidth)
        }
        val estimatedWidth = (widestCell + 28).coerceIn(96, 260).dp
        val renderedWidth = (renderedFormulaWidths[columnIndex] ?: 0f).dp + 28.dp
        maxOf(estimatedWidth, renderedWidth)
    }
    val tableWidth = columnWidths.fold(0.dp) { total, width -> total + width }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
    ) {
        Column(
            modifier = Modifier
                .then(if (maxColumnCount == 1) Modifier.fillMaxWidth() else Modifier.width(tableWidth))
                .clip(tableShape)
                .border(BorderStroke(1.dp, borderColor), tableShape)
        ) {
            rows.forEachIndexed { rowIndex, cells ->
                Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                    cells.forEachIndexed { columnIndex, cell ->
                        Box(
                            modifier = Modifier
                                .then(if (maxColumnCount == 1) Modifier.fillMaxWidth() else Modifier.width(columnWidths[columnIndex]))
                                .fillMaxHeight()
                                .background(if (rowIndex == 0) headerColor else MaterialTheme.colorScheme.surface)
                                .border(BorderStroke(0.5.dp, borderColor))
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            MixedMarkdownLatex(
                                text = cell.replace("\\|", "|").let {
                                    if (rowIndex == 0 && !it.startsWith("**")) "**$it**" else it
                                },
                                textColor = textColor,
                                compact = true,
                                onLatexWidthMeasured = { width ->
                                    val previous = renderedFormulaWidths[columnIndex] ?: 0f
                                    if (width > previous) {
                                        renderedFormulaWidths[columnIndex] = width
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

private val LATEX_BLOCK_DOLLAR = Regex("\\$\\$[\\s\\S]*?\\$\\$")
private val LATEX_INLINE_DOLLAR = Regex("(?<!\\\\)\\$(?!\\$)[^$]*?\\$")
private val LATEX_BRACKET = Regex("\\\\\\[[\\s\\S]*?\\\\\\]")
private val LATEX_PAREN = Regex("\\\\\\([\\s\\S]*?\\\\\\)")
private val UNESCAPED_PIPE = Regex("(?<!\\\\)\\|")

private fun stripLatexForTableWidth(cell: String): String {
    return cell
        .replace(LATEX_BLOCK_DOLLAR, "")
        .replace(LATEX_INLINE_DOLLAR, "")
        .replace(LATEX_BRACKET, "")
        .replace(LATEX_PAREN, "")
        .trim()
}

private val winkFrames = listOf(
    R.drawable.ic_wink_0,
    R.drawable.ic_wink_1,
    R.drawable.ic_wink_2,
    R.drawable.ic_wink_3,
    R.drawable.ic_wink_4
)

private const val BREATH_MIN = 0.88f
private const val BREATH_MAX = 1.12f

private class WinkAnimationState {
    var frameIndex by mutableIntStateOf(0)
    val scaleX = Animatable(1f)
    val scaleY = Animatable(1f)
    val breath = Animatable(1f)

    suspend fun loop() {
        coroutineScope {
            launch {
                val breathSpec = tween<Float>(1100, easing = FastOutSlowInEasing)
                while (true) {
                    breath.animateTo(BREATH_MAX, breathSpec)
                    breath.animateTo(BREATH_MIN, breathSpec)
                }
            }
            launch { blinkLoop() }
        }
    }

    private suspend fun blinkLoop() {
        val idle = spring<Float>(dampingRatio = 0.45f, stiffness = 110f)
        val bounce = spring<Float>(dampingRatio = 0.35f, stiffness = 320f)
        while (true) {
            scaleY.animateTo(1.05f, idle)
            scaleY.animateTo(1f, idle)
            delay(1100)
            coroutineScope {
                launch { scaleY.animateTo(0.86f, bounce) }
                launch { scaleX.animateTo(1.10f, bounce) }
                launch {
                    for (i in 1..4) {
                        frameIndex = i
                        delay(45)
                    }
                    delay(140)
                }
            }
            coroutineScope {
                launch { scaleY.animateTo(1f, bounce) }
                launch { scaleX.animateTo(1f, bounce) }
                launch {
                    for (i in 3 downTo 1) {
                        frameIndex = i
                        delay(45)
                    }
                    frameIndex = 0
                }
            }
            delay(900)
        }
    }
}

@Composable
private fun rememberWinkAnimation(): WinkAnimationState {
    val state = remember { WinkAnimationState() }
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            state.loop()
        }
    }
    return state
}

@Composable
fun StreamingIndicator(
    modifier: Modifier = Modifier,
    startTime: Long = 0L
) {
    val primary = MaterialTheme.colorScheme.primary
    val softer = lerp(primary, Color.White, 0.55f)
    val wink = rememberWinkAnimation()

    Image(
        painter = painterResource(winkFrames[wink.frameIndex]),
        contentDescription = null,
        colorFilter = ColorFilter.tint(softer),
        modifier = modifier
            .size(22.dp)
            .scale(wink.scaleX.value * wink.breath.value, wink.scaleY.value * wink.breath.value)
    )
}

@Composable
internal fun MessageVisibility(
    messageId: Long,
    animatingOutIds: List<Long>,
    restoringIds: List<Long>,
    messageFadeMillis: Int,
    listReady: Boolean,
    content: @Composable () -> Unit
) {
    // 撤回编辑时消息是从列表里重新插入的，AnimatedVisibility 首次组合不会播 enter，
    // 所以先以 visible=false 入场，再在下一帧放开让 fadeIn 跑起来
    var entering by remember(messageId) { mutableStateOf(messageId !in restoringIds) }
    LaunchedEffect(messageId, entering) {
        if (!entering) {
            withFrameNanos { }
            entering = true
        }
    }
    AnimatedVisibility(
        visible = entering && messageId !in animatingOutIds,
        enter = if (listReady) fadeIn(tween(messageFadeMillis)) else EnterTransition.None,
        exit = fadeOut(tween(messageFadeMillis))
    ) {
        content()
    }
}

@Composable
internal fun BouncyWinkIcon(
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    tint: Color? = null,
    onClick: (() -> Unit)? = null
) {
    var clickSquash by remember { mutableIntStateOf(0) }
    val wink = rememberWinkAnimation()
    val squashX = remember { Animatable(1f) }
    val squashY = remember { Animatable(1f) }

    LaunchedEffect(clickSquash) {
        if (clickSquash == 0) return@LaunchedEffect
        val b = spring<Float>(dampingRatio = 0.5f, stiffness = 800f)
        listOf(
            async { squashY.animateTo(0.7f, b) },
            async { squashX.animateTo(1.25f, b) }
        ).awaitAll()
        listOf(
            async { squashY.animateTo(1f, b) },
            async { squashX.animateTo(1f, b) }
        ).awaitAll()
    }

    Image(
        painter = painterResource(winkFrames[wink.frameIndex]),
        contentDescription = contentDescription,
        colorFilter = if (tint != null) ColorFilter.tint(tint) else null,
        modifier = modifier
            .then(
                if (onClick != null) {
                    Modifier.pointerInput(onClick) {
                        detectTapGestures(
                            onPress = {
                                onClick.invoke()
                                clickSquash++
                            }
                        )
                    }
                } else {
                    Modifier
                }
            )
            .scale(wink.scaleX.value * squashX.value, wink.scaleY.value * squashY.value)
    )
}

