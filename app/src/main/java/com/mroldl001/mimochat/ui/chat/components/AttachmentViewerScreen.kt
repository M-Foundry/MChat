package com.mroldl001.mimochat.ui.chat.components

import android.annotation.SuppressLint
import android.graphics.SurfaceTexture
import android.net.Uri
import android.view.Surface as AndroidSurface
import android.view.TextureView
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import com.github.panpf.zoomimage.CoilZoomAsyncImage
import com.github.panpf.zoomimage.rememberCoilZoomState
import com.mroldl001.mimochat.R
import com.mroldl001.mimochat.domain.model.MessageAttachment
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.roundToInt

private const val PROGRESS_TICK_MILLIS = 200L
private const val SEEK_SETTLE_TOLERANCE_MS = 400
private val PROGRESS_THUMB_SIZE = 12.dp
private const val FALLBACK_VIDEO_ASPECT = 16f / 9f
private const val MIN_ZOOM = 1f
private const val MAX_ZOOM = 5f
private const val ZOOM_RESET_EPSILON = 1.02f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AttachmentViewerScreen(
    attachments: List<MessageAttachment>,
    initialIndex: Int,
    onNavigateBack: () -> Unit
) {
    if (attachments.isEmpty()) {
        LaunchedEffect(Unit) { onNavigateBack() }
        return
    }
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = initialIndex.coerceIn(0, attachments.lastIndex)
    )
    val currentIndex by remember { derivedStateOf { listState.firstVisibleItemIndex } }
    val currentAttachment = attachments.getOrNull(currentIndex)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = currentAttachment?.label ?: stringResource(R.string.attachment_preview),
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    if (attachments.size > 1) {
                        Text(
                            text = "${currentIndex + 1} / ${attachments.size}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(end = 16.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        LazyRow(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
        ) {
            items(count = attachments.size, key = { index -> attachments[index].uri }) { page ->
                val attachment = attachments[page]
                val mime = attachment.mimeType.orEmpty().lowercase()
                Box(
                    modifier = Modifier
                        .fillParentMaxWidth()
                        .fillParentMaxHeight()
                ) {
                    when {
                        mime.startsWith("image/") -> ImagePreviewPage(
                            attachment = attachment,
                            isActive = page == currentIndex
                        )
                        mime.startsWith("video/") -> VideoPreviewPage(
                            attachment = attachment,
                            isActive = page == currentIndex
                        )
                        mime.startsWith("audio/") -> AudioPreviewPage(attachment)
                        else -> GenericFilePreviewPage(attachment)
                    }
                }
            }
        }
    }
}

@Composable
private fun ImagePreviewPage(attachment: MessageAttachment, isActive: Boolean) {
    val zoomState = rememberCoilZoomState()
    LaunchedEffect(isActive) {
        if (!isActive) zoomState.zoomable.reset()
    }
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CoilZoomAsyncImage(
            model = Uri.parse(attachment.uri),
            contentDescription = attachment.label ?: stringResource(R.string.attachment_image),
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds(),
            zoomState = zoomState
        )
    }
}

@Composable
private fun GenericFilePreviewPage(attachment: MessageAttachment) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(
                modifier = Modifier
                    .widthIn(min = 220.dp, max = 300.dp)
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.AutoMirrored.Outlined.InsertDriveFile,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = attachment.label ?: stringResource(R.string.attachment_file),
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
private fun VideoPreviewPage(attachment: MessageAttachment, isActive: Boolean) {
    val playback = rememberMediaPlayback(attachment.uri, isActive)
    val isPlaying = playback.isPlaying
    val zoom = remember(attachment.uri) { ZoomTransformState() }
    LaunchedEffect(attachment.uri) { zoom.reset() }
    val surfaceTextureState = remember(attachment.uri) {
        mutableStateOf<SurfaceTexture?>(null)
    }
    val surfaceTexture by surfaceTextureState
    val isPrepared = playback.isPrepared

    LaunchedEffect(isPrepared, surfaceTexture, isActive) {
        val player = playback.player ?: return@LaunchedEffect
        val texture = surfaceTexture ?: return@LaunchedEffect
        if (isPrepared) {
            runCatching {
                player.clearVideoSurface()
                player.setVideoSurface(AndroidSurface(texture))
                player.playWhenReady = isActive
            }
        }
    }
    DisposableEffect(surfaceTexture) {
        onDispose {
            playback.player?.runCatching {
                clearVideoSurface()
                setVideoSurface(null)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            BoxWithConstraints(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                val aspect = if (playback.videoWidth > 0 && playback.videoHeight > 0) {
                    playback.videoWidth.toFloat() / playback.videoHeight
                } else {
                    FALLBACK_VIDEO_ASPECT
                }
                val frameWidth: Dp
                val frameHeight: Dp
                if (maxWidth > 0.dp && maxHeight > 0.dp && maxWidth / maxHeight > aspect) {
                    frameHeight = maxHeight
                    frameWidth = maxHeight * aspect
                } else {
                    frameWidth = maxWidth
                    frameHeight = if (maxWidth > 0.dp) maxWidth / aspect else maxHeight
                }

                Box(
                    modifier = Modifier
                        .width(frameWidth)
                        .height(frameHeight)
                        .onSizeChanged { zoom.containerSize = it }
                        .graphicsLayer {
                            scaleX = zoom.scale
                            scaleY = zoom.scale
                            translationX = zoom.offset.x
                            translationY = zoom.offset.y
                        }
                        .clipToBounds(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(color = Color.Black, shape = RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        AndroidView(
                            factory = { context ->
                                TextureView(context).apply {
                                    surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                                        override fun onSurfaceTextureAvailable(
                                            texture: SurfaceTexture,
                                            width: Int,
                                            height: Int
                                        ) {
                                            surfaceTextureState.value = texture
                                        }

                                        override fun onSurfaceTextureSizeChanged(
                                            texture: SurfaceTexture,
                                            width: Int,
                                            height: Int
                                        ) {
                                            surfaceTextureState.value = texture
                                        }

                                        override fun onSurfaceTextureDestroyed(texture: SurfaceTexture): Boolean {
                                            surfaceTextureState.value = null
                                            return true
                                        }

                                        override fun onSurfaceTextureUpdated(texture: SurfaceTexture) = Unit
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    if (playback.duration > 0) {
                        DurationBadge(
                            text = formatDuration(playback.duration),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(8.dp)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(attachment.uri) {
                            detectZoomTransformGestures(zoom)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    FilledIconButton(
                        onClick = { playback.toggle() },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f),
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = stringResource(if (isPlaying) R.string.action_pause else R.string.action_play),
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    if (zoom.isZoomed) {
                        FilledIconButton(
                            onClick = { zoom.reset() },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f),
                                contentColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(8.dp)
                                .size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ZoomOutMap,
                                contentDescription = stringResource(R.string.zoom_reset),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        MediaProgressBar(
            position = playback.position,
            duration = playback.duration,
            onSeek = { target -> playback.seekTo(target) },
            modifier = Modifier.widthIn(max = 560.dp).align(Alignment.CenterHorizontally)
        )
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun AudioPreviewPage(attachment: MessageAttachment) {
    val playback = rememberMediaPlayback(attachment.uri, true)
    val isPlaying = playback.isPlaying
    val isPrepared = playback.isPrepared

    LaunchedEffect(isPrepared) {
        val player = playback.player ?: return@LaunchedEffect
        if (isPrepared) {
            runCatching { player.play() }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.widthIn(max = 420.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.13f),
                shape = RoundedCornerShape(26.dp),
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Box(
                    modifier = Modifier.size(112.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.AudioFile,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = attachment.label ?: stringResource(R.string.attachment_audio),
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(20.dp))
            FilledIconButton(
                onClick = { playback.toggle() },
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.size(64.dp)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = stringResource(if (isPlaying) R.string.action_pause else R.string.action_play),
                    modifier = Modifier.size(34.dp)
                )
            }
            Spacer(Modifier.height(20.dp))
            MediaProgressBar(
                position = playback.position,
                duration = playback.duration,
                onSeek = { target -> playback.seekTo(target) },
                modifier = Modifier.fillMaxWidth()
            )
            if (!isPrepared) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.loading_ellipsis),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MediaProgressBar(
    position: Int,
    duration: Int,
    onSeek: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val safeDuration = duration.coerceAtLeast(1)
    val enabled = duration > 0
    var trackWidthPx by remember { mutableIntStateOf(0) }
    var dragFraction by remember { mutableFloatStateOf(-1f) }
    var seekTarget by remember { mutableStateOf(-1) }
    val currentOnSeek by rememberUpdatedState(onSeek)

    LaunchedEffect(position) {
        if (seekTarget >= 0 && abs(position - seekTarget) <= SEEK_SETTLE_TOLERANCE_MS) {
            seekTarget = -1
        }
    }
    LaunchedEffect(enabled) {
        if (!enabled) {
            dragFraction = -1f
            seekTarget = -1
        }
    }

    val fraction = when {
        dragFraction >= 0f -> dragFraction
        seekTarget >= 0 -> (seekTarget.toFloat() / safeDuration).coerceIn(0f, 1f)
        enabled -> (position.toFloat() / safeDuration).coerceIn(0f, 1f)
        else -> 0f
    }
    val displayPosition = (fraction * safeDuration).toInt()

    Column(modifier = modifier.padding(horizontal = 20.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .onSizeChanged { trackWidthPx = it.width }
                .pointerInput(safeDuration, enabled) {
                    if (!enabled) return@pointerInput
                    detectTapGestures { offset ->
                        val ratio = (offset.x / trackWidthPx.coerceAtLeast(1)).coerceIn(0f, 1f)
                        val target = (ratio * safeDuration).toInt()
                        seekTarget = target
                        currentOnSeek(target)
                    }
                }
                .pointerInput(safeDuration, enabled) {
                    if (!enabled) return@pointerInput
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            seekTarget = -1
                            dragFraction =
                                (offset.x / trackWidthPx.coerceAtLeast(1)).coerceIn(0f, 1f)
                        },
                        onHorizontalDrag = { change, _ ->
                            dragFraction = (change.position.x / trackWidthPx.coerceAtLeast(1))
                                .coerceIn(0f, 1f)
                        },
                        onDragEnd = {
                            if (dragFraction >= 0f) {
                                val target = (dragFraction * safeDuration).toInt()
                                seekTarget = target
                                currentOnSeek(target)
                            }
                            dragFraction = -1f
                        },
                        onDragCancel = { dragFraction = -1f }
                    )
                },
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f),
                        shape = CircleShape
                    )
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(4.dp)
                    .background(color = MaterialTheme.colorScheme.primary, shape = CircleShape)
            )
            if (enabled) {
                val density = LocalDensity.current
                val thumbOffsetPx = with(density) {
                    (trackWidthPx * fraction - PROGRESS_THUMB_SIZE.toPx() / 2f)
                        .coerceAtLeast(0f)
                        .roundToInt()
                }
                Box(
                    modifier = Modifier
                        .offset { IntOffset(thumbOffsetPx, 0) }
                        .size(PROGRESS_THUMB_SIZE)
                        .background(color = MaterialTheme.colorScheme.primary, shape = CircleShape)
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatDuration(displayPosition),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = formatDuration(duration),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DurationBadge(
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color.Black.copy(alpha = 0.6f),
        contentColor = Color.White,
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
        )
    }
}

private class MediaPlaybackState(var player: ExoPlayer?) {
    var isPrepared by mutableStateOf(false)
    var isPlaying by mutableStateOf(false)
    var position by mutableIntStateOf(0)
    var duration by mutableIntStateOf(0)
    var videoWidth by mutableIntStateOf(0)
    var videoHeight by mutableIntStateOf(0)

    fun toggle() {
        val current = player ?: return
        runCatching {
            if (current.isPlaying) current.pause() else current.play()
        }
    }

    fun seekTo(target: Int) {
        val current = player ?: return
        runCatching { current.seekTo(target.toLong()) }
        position = target
    }
}

@Composable
private fun rememberMediaPlayback(uri: String, isActive: Boolean): MediaPlaybackState {
    val context = LocalContext.current
    val state = remember(uri) { MediaPlaybackState(player = null) }
    val autoPlay = isActive

    DisposableEffect(uri) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                val current = state.player ?: return
                state.isPrepared = playbackState == Player.STATE_READY
                state.isPlaying = current.isPlaying
                if (playbackState == Player.STATE_ENDED) {
                    state.isPlaying = false
                    state.position = state.duration
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                state.isPlaying = isPlaying
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                state.videoWidth = videoSize.width
                state.videoHeight = videoSize.height
            }

            override fun onPlayerError(error: PlaybackException) {
                state.isPrepared = false
                state.isPlaying = false
            }
        }

        val created = runCatching {
            ExoPlayer.Builder(context).build().apply {
                addListener(listener)
                setMediaItem(MediaItem.fromUri(Uri.parse(uri)))
                prepare()
            }
        }.getOrNull()

        state.player = created
        state.isPrepared = false
        state.isPlaying = false
        state.position = 0
        state.duration = 0
        state.videoWidth = 0
        state.videoHeight = 0

        onDispose {
            runCatching {
                created?.removeListener(listener)
                created?.release()
            }
            state.player = null
            state.isPrepared = false
            state.isPlaying = false
            state.position = 0
            state.duration = 0
            state.videoWidth = 0
            state.videoHeight = 0
        }
    }

    LaunchedEffect(autoPlay, state.isPrepared) {
        if (autoPlay && state.isPrepared) {
            runCatching { state.player?.play() }
        } else if (!autoPlay) {
            runCatching { state.player?.pause() }
        }
    }

    LaunchedEffect(uri) {
        while (true) {
            delay(PROGRESS_TICK_MILLIS)
            val player = state.player ?: continue
            if (!state.isPrepared) continue
            state.position = runCatching { player.currentPosition.toInt() }
                .getOrDefault(state.position)
            state.isPlaying = runCatching { player.isPlaying }.getOrDefault(false)
            state.duration = runCatching { player.duration }
                .getOrDefault(0L)
                .let { if (it == C.TIME_UNSET || it < 0) 0 else it.toInt() }
        }
    }

    return state
}

private class ZoomTransformState {
    var scale by mutableFloatStateOf(MIN_ZOOM)
    var offset by mutableStateOf(Offset.Zero)
    var containerSize by mutableStateOf(IntSize.Zero)

    val isZoomed: Boolean get() = scale > ZOOM_RESET_EPSILON

    fun apply(focus: Offset, pan: Offset, zoomChange: Float) {
        val nextScale = (scale * zoomChange).coerceIn(MIN_ZOOM, MAX_ZOOM)
        if (nextScale <= MIN_ZOOM) {
            reset()
            return
        }
        val ratio = nextScale / scale
        val center = Offset(containerSize.width / 2f, containerSize.height / 2f)
        val focusShift = (focus - center) * (1f - ratio)
        val scaled = Offset(offset.x * ratio, offset.y * ratio)
        scale = nextScale
        val reachX = scaledContentReach(containerSize.width, nextScale)
        val reachY = scaledContentReach(containerSize.height, nextScale)
        offset = Offset(
            x = (scaled.x + pan.x + focusShift.x).coerceIn(-reachX, reachX),
            y = (scaled.y + pan.y + focusShift.y).coerceIn(-reachY, reachY)
        )
    }

    private fun scaledContentReach(containerExtent: Int, contentScale: Float): Float {
        if (containerExtent <= 0) return 0f
        val scaledHalf = containerExtent * contentScale / 2f
        return (scaledHalf - containerExtent / 2f).coerceAtLeast(0f)
    }

    fun reset() {
        scale = MIN_ZOOM
        offset = Offset.Zero
    }
}

private suspend fun PointerInputScope.detectZoomTransformGestures(state: ZoomTransformState) {
    val touchSlop = viewConfiguration.touchSlop
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false)
        var pastTouchSlop = false
        var previousSpan = 0f
        do {
            val event = awaitPointerEvent(PointerEventPass.Main)
            val pressed = event.changes.filter { it.pressed }
            if (pressed.isEmpty()) break

            if (pressed.size >= 2) {
                val span = spanOf(pressed[0].position, pressed[1].position)
                val centroid = event.calculateCentroid()
                if (previousSpan > 0f && span > 0f) {
                    state.apply(focus = centroid, pan = Offset.Zero, zoomChange = span / previousSpan)
                }
                previousSpan = span
            } else {
                previousSpan = 0f
                if (state.isZoomed) {
                    val change = pressed.first()
                    state.apply(
                        focus = change.position,
                        pan = change.position - change.previousPosition,
                        zoomChange = 1f
                    )
                }
            }

            val zoomInProgress = pressed.size >= 2
            if (!pastTouchSlop && (zoomInProgress || state.isZoomed)) {
                var travelled = 0f
                event.changes.forEach { travelled += (it.position - it.previousPosition).getDistance() }
                if (travelled > touchSlop) pastTouchSlop = true
            }

            if (pastTouchSlop && (zoomInProgress || state.isZoomed)) {
                event.changes.forEach { if (it.positionChanged()) it.consume() }
            }
        } while (event.changes.any { it.pressed })
    }
}

private fun spanOf(a: Offset, b: Offset): Float = hypot(b.x - a.x, b.y - a.y)

internal fun formatDuration(millis: Int): String {
    if (millis <= 0) return "0:00"
    val totalSeconds = millis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}
