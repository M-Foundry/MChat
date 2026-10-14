package com.mroldl001.mimochat.ui.chat.components

import android.annotation.SuppressLint
import com.mroldl001.mimochat.R
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.ui.res.stringResource
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Css
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.*
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.runtime.*
import com.mroldl001.mimochat.ui.theme.LocalCodeBlockDark
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.CancellationException
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import dev.textmate.theme.FontStyle as TextMateFontStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.pow
import kotlin.math.roundToInt

internal data class CodeFullscreenRequest(
    val highlightedLines: List<AnnotatedString>,
    val maxLineNumberWidth: Int,
    val palette: CodeBlockPalette,
    val fenceLabel: String,
    val normalizedLanguage: String,
    val rawCode: String,
    val originBounds: CodeBlockBounds?
)

internal interface CodeFullscreenOverlayHost {
    fun show(request: CodeFullscreenRequest)
    fun dismiss()
}

internal val LocalCodeFullscreenOverlayHost =
    staticCompositionLocalOf<CodeFullscreenOverlayHost?> { null }

@Composable
fun CodeFullscreenOverlayProvider(content: @Composable () -> Unit) {
    val context = LocalContext.current
    var request by remember { mutableStateOf<CodeFullscreenRequest?>(null) }
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    val host = remember {
        object : CodeFullscreenOverlayHost {
            override fun show(r: CodeFullscreenRequest) {
                request = r
                scope.launch {
                    progress.animateTo(
                        1f,
                        spring(dampingRatio = 0.86f, stiffness = 380f)
                    )
                }
            }

            override fun dismiss() {
                scope.launch {
                    progress.animateTo(
                        0f,
                        spring(dampingRatio = 0.9f, stiffness = 460f)
                    )
                    request = null
                }
            }
        }
    }

    CompositionLocalProvider(LocalCodeFullscreenOverlayHost provides host) {
        Box(modifier = Modifier.fillMaxSize()) {
            content()
            request?.let { req ->
                CodeFullscreenOverlay(
                    highlightedLines = req.highlightedLines,
                    maxLineNumberWidth = req.maxLineNumberWidth,
                    palette = req.palette,
                    fenceLabel = req.fenceLabel,
                    normalizedLanguage = req.normalizedLanguage,
                    rawCode = req.rawCode,
                    animatedProgress = progress.value,
                    originBounds = req.originBounds,
                    onDismiss = { host.dismiss() },
                    onCopy = {
                        val clipboard =
                            context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("code", it))
                        Toast.makeText(
                            context,
                            context.getString(R.string.toast_code_copied),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            }
        }
    }
}

@Composable
internal fun CodeBlockView(code: String, info: String?) {
    val context = LocalContext.current
    val darkCodeBlock = LocalCodeBlockDark.current
    val palette = codeBlockPalette(darkCodeBlock)
    val lines = code.replace("\r\n", "\n").replace('\r', '\n').split("\n")
    val fenceLabel = info?.trim()?.substringBefore(' ').orEmpty()
    val normalizedLanguage = normalizeCodeLanguage(fenceLabel)
    val language = normalizedLanguage.takeIf { hasGrammar(context, it) }
    val codeLines = lines
    var originBounds by remember { mutableStateOf<CodeBlockBounds?>(null) }
    var blockCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }

    val lineCount = codeLines.size
    val maxLineNumberWidth = lineCount.toString().length
    val appContext = context.applicationContext
    val codeHScroll = rememberScrollState()
    val highlightedLines by produceState(
        initialValue = codeLines.map { AnnotatedString(it) },
        codeLines,
        language,
        darkCodeBlock
    ) {
        value = withContext(Dispatchers.Default) {
            try {
                highlightCodeLines(codeLines, language, darkCodeBlock, appContext)
            } catch (error: Throwable) {
                codeLines.map { AnnotatedString(it) }
            }
        }
    }

    val host = LocalCodeFullscreenOverlayHost.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .onGloballyPositioned { blockCoordinates = it }
            .background(
                color = palette.background,
                shape = RoundedCornerShape(8.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            fenceLabel.takeIf { it.isNotEmpty() }?.let {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    codeLanguageIcon(normalizedLanguage)?.let { icon ->
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = palette.muted,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = fenceLabel,
                        style = TextStyle(
                            color = palette.muted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.weight(1f))

            IconButton(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("code", codeLines.joinToString("\n"))
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, context.getString(R.string.toast_code_copied), Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.size(CODE_TOOLBAR_BUTTON)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = stringResource(R.string.copy_code),
                    tint = palette.muted,
                    modifier = Modifier.size(COPY_ICON_SIZE)
                )
            }

            IconButton(
                onClick = {
                    val bounds = blockCoordinates?.boundsInWindow()
                    originBounds = bounds?.let {
                        CodeBlockBounds(
                            left = it.left.roundToInt(),
                            top = it.top.roundToInt(),
                            right = it.right.roundToInt(),
                            bottom = it.bottom.roundToInt()
                        )
                    }
                    host?.show(
                        CodeFullscreenRequest(
                            highlightedLines = highlightedLines,
                            maxLineNumberWidth = maxLineNumberWidth,
                            palette = palette,
                            fenceLabel = fenceLabel,
                            normalizedLanguage = normalizedLanguage,
                            rawCode = code,
                            originBounds = originBounds
                        )
                    )
                },
                modifier = Modifier.size(CODE_TOOLBAR_BUTTON)
            ) {
                Icon(
                    imageVector = Icons.Default.Fullscreen,
                    contentDescription = stringResource(R.string.code_fullscreen),
                    tint = palette.muted,
                    modifier = Modifier.size(FULLSCREEN_ICON_SIZE)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .padding(bottom = 12.dp)
        ) {
            CodeBody(
                highlightedLines = highlightedLines,
                maxLineNumberWidth = maxLineNumberWidth,
                textColor = palette.text,
                numberColor = palette.muted,
                horizontalState = codeHScroll,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
@SuppressLint("UnusedBoxWithConstraintsScope")
private fun CodeFullscreenOverlay(
    highlightedLines: List<AnnotatedString>,
    maxLineNumberWidth: Int,
    palette: CodeBlockPalette,
    fenceLabel: String,
    normalizedLanguage: String,
    rawCode: String,
    animatedProgress: Float,
    originBounds: CodeBlockBounds?,
    onDismiss: () -> Unit,
    onCopy: (String) -> Unit
) {
    val vScroll = rememberScrollState()
    val hScroll = rememberScrollState()
    val hBarVisible by rememberScrollbarVisibility(hScroll)
    val vBarVisible by rememberScrollbarVisibility(vScroll)
    val anyActive by rememberScrollActivity(vScroll, hScroll)
    val bg = palette.background
    val isDark = bg.isDark()
    val scrollbarThumb = if (isDark) Color.White.copy(alpha = 0.4f) else Color.Black.copy(alpha = 0.4f)

    var predictiveBackProgress by remember { mutableFloatStateOf(0f) }
    val backScale by remember { derivedStateOf { 1f - predictiveBackProgress * 0.08f } }
    val backAlpha by remember { derivedStateOf { 1f - predictiveBackProgress * 0.35f } }

    PredictiveBackHandler { progress ->
        try {
            progress.collect { backEvent ->
                predictiveBackProgress = backEvent.progress
            }
            predictiveBackProgress = 1f
            onDismiss()
        } catch (_: CancellationException) {
            predictiveBackProgress = 0f
        }
    }

    BackHandler(onBack = onDismiss)

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        val density = LocalDensity.current
        val overlayMaxWidth = maxWidth
        val overlayMaxHeight = maxHeight
        val overlayWidthPx = with(density) { overlayMaxWidth.toPx() }
        val overlayHeightPx = with(density) { overlayMaxHeight.toPx() }

        val origin = originBounds
        val containerGeometry = remember(origin, overlayWidthPx, overlayHeightPx) {
            if (origin == null || origin.width() <= 0 || origin.height() <= 0 ||
                overlayWidthPx <= 0f || overlayHeightPx <= 0f
            ) {
                FULLSCREEN_GEOMETRY
            } else {
                val originCenterX = origin.left + origin.width() / 2f
                val originCenterY = origin.top + origin.height() / 2f
                ContainerGeometry(
                    offsetX = originCenterX - overlayWidthPx / 2f,
                    offsetY = originCenterY - overlayHeightPx / 2f,
                    scaleX = (origin.width().toFloat() / overlayWidthPx).coerceIn(0.05f, 1f),
                    scaleY = (origin.height().toFloat() / overlayHeightPx).coerceIn(0.05f, 1f),
                    corner = CODE_BLOCK_CORNER.dp
                )
            }
        }

        val resolved = containerGeometry.interpolateTo(FULLSCREEN_GEOMETRY, animatedProgress)
        val contentAlpha = FastOutSlowInEasing.transform(animatedProgress.coerceIn(0f, 1f))

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = resolved.offsetX
                    translationY = resolved.offsetY
                    scaleX = resolved.scaleX * backScale
                    scaleY = resolved.scaleY * backScale
                    transformOrigin = TransformOrigin.Center
                }
                .clipToBounds()
                .background(bg, RoundedCornerShape(resolved.corner))
        ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = contentAlpha * backAlpha }
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                codeLanguageIcon(normalizedLanguage)?.let { icon ->
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = palette.muted,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = fenceLabel.ifEmpty { stringResource(R.string.code_block) },
                    style = TextStyle(
                        color = palette.muted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { onCopy(rawCode) }) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = stringResource(R.string.copy_code),
                        tint = palette.muted,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.close_fullscreen),
                        tint = palette.muted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clipToBounds()
                    .pointerInput(Unit) {
                        detectTapGestures { onDismiss() }
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(vScroll)
                ) {
                    CodeBody(
                        highlightedLines = highlightedLines,
                        maxLineNumberWidth = maxLineNumberWidth,
                        textColor = palette.text,
                        numberColor = palette.muted,
                        horizontalState = hScroll,
                        showHorizontalBar = false,
                        fillHeight = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 32.dp, bottom = 24.dp)
                    )
                }

                CodeVerticalScrollbar(
                    state = vScroll,
                    visible = anyActive && vBarVisible,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(horizontal = SCROLLBAR_GAP),
                    thumbColor = scrollbarThumb
                )

                CodeHorizontalScrollbar(
                    state = hScroll,
                    visible = anyActive && hBarVisible,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(top = SCROLLBAR_GAP),
                    thumbColor = scrollbarThumb
                )
            }
        }
        }
    }
}

@Composable
private fun CodeBody(
    highlightedLines: List<AnnotatedString>,
    maxLineNumberWidth: Int,
    textColor: Color,
    numberColor: Color,
    horizontalState: ScrollState,
    showHorizontalBar: Boolean = true,
    fillHeight: Boolean = false,
    modifier: Modifier = Modifier
) {
    val hBarVisible by rememberScrollbarVisibility(horizontalState)
    val hBarActive by rememberScrollActivity(horizontalState)
    val numberStyle = codeLineStyle(numberColor)
    val textStyle = codeLineStyle(textColor)
    val scrollbarThumb = if (!textColor.isDark()) {
        Color.White.copy(alpha = 0.4f)
    } else {
        Color.Black.copy(alpha = 0.4f)
    }

    Box(modifier) {
        Column(
            modifier = if (fillHeight) {
                Modifier.fillMaxSize()
            } else {
                Modifier.fillMaxWidth()
            }.horizontalScroll(horizontalState)
        ) {
            highlightedLines.forEachIndexed { index, line ->
                Row {
                    Text(
                        text = (index + 1).toString().padStart(maxLineNumberWidth, ' '),
                        style = numberStyle,
                        modifier = Modifier
                            .width(numberColumnWidth(maxLineNumberWidth))
                            .alignByBaseline()
                    )
                    Text(
                        text = line.ifEmpty { AnnotatedString(" ") },
                        style = textStyle,
                        modifier = Modifier
                            .padding(start = CODE_GUTTER)
                            .alignByBaseline()
                    )
                }
            }
            if (fillHeight) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            }
        }

        if (showHorizontalBar) {
            CodeHorizontalScrollbar(
                state = horizontalState,
                visible = hBarActive && hBarVisible,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 2.dp)
                    .padding(top = SCROLLBAR_GAP),
                thumbColor = scrollbarThumb
            )
        }
    }
}

private fun numberColumnWidth(digits: Int): Dp =
    (CODE_FONT_SIZE.value * 0.62f * digits).dp + CODE_GUTTER

private fun codeLineStyle(color: Color) = TextStyle(
    color = color,
    fontSize = CODE_FONT_SIZE,
    lineHeight = CODE_LINE_HEIGHT,
    fontFamily = FontFamily.Monospace
)

private fun Color.isDark(): Boolean {
    val r = if (red <= 0.04045f) red / 12.92f else ((red + 0.055f) / 1.055f).pow(2.4f)
    val g = if (green <= 0.04045f) green / 12.92f else ((green + 0.055f) / 1.055f).pow(2.4f)
    val b = if (blue <= 0.04045f) blue / 12.92f else ((blue + 0.055f) / 1.055f).pow(2.4f)
    return (0.2126f * r + 0.7152f * g + 0.0722f * b) < 0.5f
}

@Composable
@SuppressLint("UnusedBoxWithConstraintsScope")
private fun CodeHorizontalScrollbar(
    state: ScrollState,
    visible: Boolean,
    modifier: Modifier = Modifier,
    thumbColor: Color = SCROLLBAR_THUMB
) {
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(SCROLLBAR_FADE_MILLIS, easing = FastOutSlowInEasing),
        label = "hBarAlpha"
    )
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(SCROLLBAR_TRACK_HEIGHT)
    ) {
        val density = LocalDensity.current
        val viewportDp = maxWidth
        val viewportPx = with(density) { viewportDp.toPx() }
        val contentPx = viewportPx + state.maxValue
        if (contentPx > 0f && viewportPx > 0f) {
            val thumbWidth = viewportPx * (viewportPx / contentPx).coerceAtLeast(SCROLLBAR_MIN_FRACTION)
            val travel = viewportPx - thumbWidth
            val fraction = if (state.maxValue > 0) state.value.toFloat() / state.maxValue else 0f
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        this.alpha = alpha
                        translationX = travel * fraction
                        scaleX = (thumbWidth / viewportPx).coerceAtLeast(SCROLLBAR_MIN_FRACTION)
                        transformOrigin = TransformOrigin(0f, 0.5f)
                    }
                    .fillMaxHeight()
                    .fillMaxWidth()
                    .background(color = thumbColor, shape = RoundedCornerShape(50))
            )
        }
    }
}

@Composable
@SuppressLint("UnusedBoxWithConstraintsScope")
private fun CodeVerticalScrollbar(
    state: ScrollState,
    visible: Boolean,
    modifier: Modifier = Modifier,
    thumbColor: Color = SCROLLBAR_THUMB
) {
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(SCROLLBAR_FADE_MILLIS, easing = FastOutSlowInEasing),
        label = "vBarAlpha"
    )
    BoxWithConstraints(modifier.fillMaxHeight().width(SCROLLBAR_TRACK_WIDTH)) {
        val density = LocalDensity.current
        val viewportDp = maxHeight
        val viewportPx = with(density) { viewportDp.toPx() }
        val contentPx = viewportPx + state.maxValue
        if (contentPx > 0f && viewportPx > 0f) {
            val thumbHeight = viewportPx * (viewportPx / contentPx).coerceAtLeast(SCROLLBAR_MIN_FRACTION)
            val travel = viewportPx - thumbHeight
            val fraction = if (state.maxValue > 0) state.value.toFloat() / state.maxValue else 0f
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        this.alpha = alpha
                        translationY = travel * fraction
                        scaleY = (thumbHeight / viewportPx).coerceAtLeast(SCROLLBAR_MIN_FRACTION)
                        transformOrigin = TransformOrigin(0.5f, 0f)
                    }
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .background(color = thumbColor, shape = RoundedCornerShape(50))
            )
        }
    }
}

@Composable
private fun rememberScrollbarVisibility(state: ScrollState): State<Boolean> {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(state) {
        snapshotFlow { state.maxValue > 0 }.collect { hasRange ->
            visible = hasRange
        }
    }
    return rememberUpdatedState(visible)
}

@Composable
private fun rememberScrollActivity(vararg states: ScrollState): State<Boolean> {
    var active by remember { mutableStateOf(false) }
    LaunchedEffect(*states) {
        var hideJob: Job? = null
        var previous = states.map { it.value }
        snapshotFlow { states.map { it.value to it.isScrollInProgress } }
            .collect { snapshot ->
                val scrolling = snapshot.any { it.second }
                val moved = snapshot.indices.any { snapshot[it].first != previous[it] }
                previous = snapshot.map { it.first }
                if (scrolling || moved) {
                    hideJob?.cancel()
                    hideJob = null
                    if (!active) active = true
                    if (!scrolling) {
                        hideJob = launch { delay(SCROLLBAR_HIDE_DELAY); active = false }
                    }
                } else if (active && hideJob == null) {
                    hideJob = launch { delay(SCROLLBAR_HIDE_DELAY); active = false }
                }
            }
    }
    return rememberUpdatedState(active)
}


private val CODE_FONT_SIZE = 13.sp
private val CODE_LINE_HEIGHT = 22.sp
private const val CODE_BLOCK_CORNER = 8
private const val FULLSCREEN_CORNER = 0

internal data class CodeBlockBounds(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
) {
    fun width(): Int = right - left
    fun height(): Int = bottom - top
}

private data class ContainerGeometry(
    val offsetX: Float,
    val offsetY: Float,
    val scaleX: Float,
    val scaleY: Float,
    val corner: Dp
)

private val FULLSCREEN_GEOMETRY = ContainerGeometry(
    offsetX = 0f,
    offsetY = 0f,
    scaleX = 1f,
    scaleY = 1f,
    corner = 0.dp
)

private fun ContainerGeometry.interpolateTo(target: ContainerGeometry, fraction: Float): ContainerGeometry =
    ContainerGeometry(
        offsetX = offsetX + (target.offsetX - offsetX) * fraction,
        offsetY = offsetY + (target.offsetY - offsetY) * fraction,
        scaleX = scaleX + (target.scaleX - scaleX) * fraction,
        scaleY = scaleY + (target.scaleY - scaleY) * fraction,
        corner = (corner + (target.corner - corner) * fraction).coerceAtLeast(0.dp)
    )
private val CODE_GUTTER = 14.dp
private val COPY_ICON_SIZE = 18.dp
private val FULLSCREEN_ICON_SIZE = COPY_ICON_SIZE / FULLSCREEN_INK_RATIO
private val CODE_TOOLBAR_BUTTON = 34.dp
private const val FULLSCREEN_INK_RATIO = 0.64f
private const val SCROLLBAR_HIDE_DELAY = 700L
private const val SCROLLBAR_FADE_MILLIS = 220
private val SCROLLBAR_GAP = 8.dp
private val SCROLLBAR_TRACK_HEIGHT = 5.dp
private val SCROLLBAR_TRACK_WIDTH = 5.dp
private val SCROLLBAR_THUMB = Color(0x66FFFFFF)
private const val SCROLLBAR_MIN_FRACTION = 0.12f

private fun codeLanguageIcon(language: String?): ImageVector? {
    if (language == null) return Icons.Default.Code
    return when (language) {
        "cpp", "go", "rust" -> Icons.Default.Code
        else -> codeLanguageVectorIcon(language) ?: when (language) {
            "java" -> Icons.Default.Coffee
            "csharp" -> Icons.Default.Code
            "powershell" -> Icons.Default.Terminal
            "sql" -> Icons.Default.Storage
            "scss" -> Icons.Default.Css
            else -> Icons.Default.Code
        }
    }
}

private fun normalizeCodeLanguage(label: String): String {
    return when (label.lowercase().substringBefore(' ').trim()) {
        "py", "python3" -> "python"
        "js", "mjs", "cjs", "node" -> "javascript"
        "ts", "mts", "cts" -> "typescript"
        "kt", "kts" -> "kotlin"
        "c++", "cc", "cxx", "hpp" -> "cpp"
        "c#", "cs" -> "csharp"
        "golang" -> "go"
        "rs" -> "rust"
        "sh", "zsh", "shell", "shellscript" -> "shellscript"
        "bash", "pwsh", "ps1" -> "powershell"
        "htm" -> "html"
        "yml" -> "yaml"
        "pl", "perl" -> "perl"
        "hs" -> "haskell"
        "ex", "exs" -> "elixir"
        "erl" -> "erlang"
        "clj", "cljs" -> "clojure"
        "jl" -> "julia"
        "sol" -> "solidity"
        "gql" -> "graphql"
        "f90", "f95", "f03" -> "fortran-free-form"
        "cr" -> "crystal"
        "ml", "mli" -> "ocaml"
        "fs", "fsi", "fsx" -> "fsharp"
        "rkt" -> "racket"
        "lisp", "cl", "el" -> "common-lisp"
        "adb", "ads" -> "ada"
        "purs", "rescript" -> "purescript"
        "hx" -> "haxe"
        "wl", "nb", "mathematica" -> "wolfram"
        "tex" -> "latex"
        "md", "mkd" -> "markdown"
        "dockerfile" -> "docker"
        ".env", "dotenv" -> "dotenv"
        "tf", "tfvars" -> "terraform"
        "el", "emacs", "gnuemacs" -> "emacs-lisp"
        "proto" -> "proto"
        "make" -> "make"
        "objc" -> "objective-c"
        "objcpp" -> "objective-cpp"
        "coffee", "coffeescript" -> "coffee"
        "htmles" -> "html-derivative"
        "cshtml" -> "razor"
        "restructuredtext", "rst" -> "rst"
        "f#" -> "fsharp"
        "vb", "vbnet" -> "vb"
        "plaintext", "txt" -> "text"
        else -> label.lowercase().substringBefore(' ').trim()
    }
}

internal data class CodeBlockPalette(
    val background: Color,
    val text: Color,
    val muted: Color,
    val keyword: Color,
    val string: Color,
    val number: Color,
    val comment: Color,
    val type: Color,
    val function: Color,
    val operator: Color,
    val annotation: Color,
    val property: Color
)

internal val DarkCodeBlockPalette = CodeBlockPalette(
    background = Color.Black,
    text = Color.White,
    muted = Color.Gray,
    keyword = Color(0xFFC792EA),
    string = Color(0xFFC3E88D),
    number = Color(0xFFF78C6C),
    comment = Color(0xFF7C8495),
    type = Color(0xFFFFCB6B),
    function = Color(0xFF82AAFF),
    operator = Color(0xFF89DDFF),
    annotation = Color(0xFFFF5370),
    property = Color(0xFFF07178)
)

internal val LightCodeBlockPalette = CodeBlockPalette(
    background = Color.White,
    text = Color(0xFF1F2328),
    muted = Color(0xFF6B7280),
    keyword = Color(0xFFCF222E),
    string = Color(0xFF0A3069),
    number = Color(0xFF0550AE),
    comment = Color(0xFF6E7781),
    type = Color(0xFF953800),
    function = Color(0xFF8250DF),
    operator = Color(0xFF0550AE),
    annotation = Color(0xFF953800),
    property = Color(0xFF116329)
)

// 浅色用 primary 6% 叠白而非纯白：跟主题联动，又不压过语法高亮
@Composable
internal fun codeBlockPalette(dark: Boolean): CodeBlockPalette {
    return if (dark) {
        DarkCodeBlockPalette
    } else {
        LightCodeBlockPalette.copy(
            background = MaterialTheme.colorScheme.primary
                .copy(alpha = 0.06f)
                .compositeOver(Color.White)
        )
    }
}

private fun highlightCodeLines(
    lines: List<String>,
    language: String?,
    dark: Boolean,
    context: Context
): List<AnnotatedString> {
    val plain = lines.map { AnnotatedString(it) }
    if (language == null) return plain
    return try {
        val palette = if (dark) DarkCodeBlockPalette else LightCodeBlockPalette
        val grammar = codeGrammar(context, language) ?: return plain
        val tokens = codeTokens(grammar, lines)
        val theme = codeTheme(dark, palette)
        val defaultColor = palette.text

        lines.mapIndexed { index, line ->
            if (line.isEmpty()) return@mapIndexed AnnotatedString("")
            val resolved = tokens.getOrNull(index).orEmpty().map { token ->
                Pair(token, resolveStyle(theme, token.scopes))
            }
            buildAnnotatedString {
                append(line)
                resolved.forEach { (token, style) ->
                    val start = token.start.coerceIn(0, line.length)
                    val end = token.end.coerceIn(start, line.length)
                    if (end <= start) return@forEach
                    val resolvedColor = style?.foreground
                        ?.takeIf { it != 0L }
                        ?.let { Color(textMateForegroundColor(it)) }
                    val italic = style?.fontStyle?.any { it == TextMateFontStyle.ITALIC } == true
                    if (resolvedColor == null && !italic) return@forEach
                    addStyle(
                        SpanStyle(
                            color = resolvedColor ?: defaultColor,
                            fontStyle = if (italic) FontStyle.Italic else null
                        ),
                        start,
                        end
                    )
                }
            }
        }
    } catch (error: Throwable) {
        plain
    }
}
