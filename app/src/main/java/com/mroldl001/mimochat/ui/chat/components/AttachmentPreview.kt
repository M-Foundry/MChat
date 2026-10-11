package com.mroldl001.mimochat.ui.chat.components

import android.graphics.Bitmap
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.VideoFile
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.mroldl001.mimochat.R
import com.mroldl001.mimochat.domain.model.MessageAttachment
import com.mroldl001.mimochat.ui.util.MediaMeta
import com.mroldl001.mimochat.ui.util.loadMediaMeta
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun AttachmentPreviewList(
    attachments: List<MessageAttachment>,
    onClear: ((Int) -> Unit)? = null,
    onClick: ((Int) -> Unit)? = null,
    compactVisual: Boolean = true,
    modifier: Modifier = Modifier
) {
    if (attachments.isEmpty()) return
    var removingUris by remember { mutableStateOf<Set<String>>(emptySet()) }
    val scope = rememberCoroutineScope()
    val exitMillis = 200
    val requestRemove: (String) -> Unit = { uri ->
        removingUris = removingUris + uri
        scope.launch {
            delay(exitMillis + 20L)
            val idx = attachments.indexOfFirst { it.uri == uri }
            if (idx >= 0) onClear?.invoke(idx)
            removingUris = removingUris - uri
        }
    }

    val cardIndices = remember(attachments) {
        attachments.indices.filter {
            val mime = attachments[it].mimeType.orEmpty().lowercase()
            mime.startsWith("image/") || mime.startsWith("video/") || mime.startsWith("audio/")
        }.toList()
    }
    val cardSet = remember(cardIndices) { cardIndices.toSet() }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (cardIndices.isNotEmpty()) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                cardIndices.forEach { index ->
                    val att = attachments[index]
                    AnimatedVisibility(
                        visible = att.uri !in removingUris,
                        enter = EnterTransition.None,
                        exit = fadeOut(tween(exitMillis)) + scaleOut(tween(exitMillis), targetScale = 0.8f)
                    ) {
                        AttachmentPreview(
                            uri = att.uri,
                            mimeType = att.mimeType,
                            label = att.label,
                            compact = compactVisual,
                            onClear = onClear?.let { { requestRemove(att.uri) } },
                            onClick = onClick?.let { { it(index) } }
                        )
                    }
                }
            }
        }

        attachments.indices.filter { it !in cardSet }.forEach { index ->
            val att = attachments[index]
            AnimatedVisibility(
                visible = att.uri !in removingUris,
                enter = EnterTransition.None,
                exit = fadeOut(tween(exitMillis)) + scaleOut(tween(exitMillis), targetScale = 0.8f)
            ) {
                AttachmentPreview(
                    uri = att.uri,
                    mimeType = att.mimeType,
                    label = att.label,
                    onClear = onClear?.let { { requestRemove(att.uri) } },
                    onClick = onClick?.let { { it(index) } },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
internal fun AttachmentPreview(
    uri: String,
    mimeType: String?,
    label: String? = null,
    compact: Boolean = false,
    onClear: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val normalizedMimeType = mimeType.orEmpty().lowercase()
    val context = LocalContext.current
    val resolvedLabel by produceState(initialValue = label, uri, label) {
        value = label ?: withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.query(
                    Uri.parse(uri),
                    arrayOf(OpenableColumns.DISPLAY_NAME),
                    null,
                    null,
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) cursor.getString(0) else null
                }
            }.getOrNull()
        }
    }

    when {
        normalizedMimeType.startsWith("image/") -> VisualAttachmentPreview(
            compact = compact,
            onClear = onClear,
            onClick = onClick,
            modifier = modifier
        ) {
            AsyncImage(
                model = Uri.parse(uri),
                contentDescription = "图片附件",
                contentScale = if (compact) ContentScale.Crop else ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }

        normalizedMimeType.startsWith("video/") -> VideoAttachmentPreview(
            uri = uri,
            compact = compact,
            onClear = onClear,
            onClick = onClick,
            modifier = modifier
        )

        normalizedMimeType.startsWith("audio/") -> AudioAttachmentPreview(
            uri = uri,
            compact = compact,
            onClear = onClear,
            onClick = onClick,
            modifier = modifier
        )

        else -> FileAttachmentPreview(
            icon = { Icon(Icons.AutoMirrored.Outlined.InsertDriveFile, contentDescription = null) },
            title = resolvedLabel ?: stringResource(R.string.attachment_file),
            subtitle = fileTypeLabel(normalizedMimeType, resolvedLabel),
            onClear = onClear,
            onClick = onClick,
            modifier = modifier
        )
    }
}

@Composable
private fun VisualAttachmentPreview(
    compact: Boolean,
    onClear: (() -> Unit)?,
    onClick: (() -> Unit)?,
    modifier: Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(if (compact) 14.dp else 15.dp)
    val sizeModifier = if (compact) {
        Modifier.size(96.dp)
    } else {
        Modifier
            .width(236.dp)
            .height(164.dp)
    }

    Box(
        modifier = modifier
            .then(sizeModifier)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                shape = shape
            )
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        content()
        ClearAttachmentButton(onClear)
    }
}

@Composable
private fun VideoAttachmentPreview(
    uri: String,
    compact: Boolean,
    onClear: (() -> Unit)?,
    onClick: (() -> Unit)?,
    modifier: Modifier
) {
    val context = LocalContext.current
    val videoMeta by produceState(initialValue = MediaMeta(null, 0), uri) {
        value = loadMediaMeta(context, uri)
    }
    val thumbnail = videoMeta.thumbnail
    val durationMs = videoMeta.durationMs

    VisualAttachmentPreview(
        compact = compact,
        onClear = onClear,
        onClick = onClick,
        modifier = modifier
    ) {
        thumbnail?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = stringResource(R.string.attachment_video),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } ?: run {
            Icon(
                imageVector = Icons.Outlined.VideoFile,
                contentDescription = stringResource(R.string.attachment_video),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
        }
        Surface(
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
            contentColor = MaterialTheme.colorScheme.primary,
            shape = RoundedCornerShape(50),
            tonalElevation = 1.dp,
            modifier = Modifier.size(if (compact) 36.dp else 44.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "播放视频",
                    modifier = Modifier.size(if (compact) 23.dp else 28.dp)
                )
            }
        }
        if (durationMs > 0) {
            Surface(
                color = Color.Black.copy(alpha = 0.55f),
                contentColor = Color.White,
                shape = RoundedCornerShape(5.dp),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
            ) {
                Text(
                    text = formatDuration(durationMs),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }
        }
    }
}

@Composable
private fun AudioAttachmentPreview(
    uri: String,
    compact: Boolean,
    onClear: (() -> Unit)?,
    onClick: (() -> Unit)?,
    modifier: Modifier
) {
    val context = LocalContext.current
    val audioMeta by produceState(initialValue = MediaMeta(null, 0), uri) {
        value = loadMediaMeta(context, uri)
    }
    val durationMs = audioMeta.durationMs

    VisualAttachmentPreview(
        compact = compact,
        onClear = onClear,
        onClick = onClick,
        modifier = modifier
    ) {
        Icon(
            imageVector = Icons.Outlined.AudioFile,
            contentDescription = stringResource(R.string.attachment_audio),
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(if (compact) 36.dp else 44.dp)
        )
        if (durationMs > 0) {
            Surface(
                color = Color.Black.copy(alpha = 0.55f),
                contentColor = Color.White,
                shape = RoundedCornerShape(5.dp),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
            ) {
                Text(
                    text = formatDuration(durationMs),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }
        }
    }
}

@Composable
private fun FileAttachmentPreview(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    onClear: (() -> Unit)?,
    onClick: (() -> Unit)?,
    modifier: Modifier
) {
    Box(
        modifier = modifier
            .widthIn(min = 204.dp, max = 244.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(14.dp)
        ) {
            Row(
                modifier = Modifier.padding(
                    start = 10.dp,
                    top = 9.dp,
                    end = if (onClear == null) 12.dp else 42.dp,
                    bottom = 9.dp
                ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.13f),
                    shape = RoundedCornerShape(10.dp),
                    contentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        icon()
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        ClearAttachmentButton(onClear)
    }
}

@Composable
private fun fileTypeLabel(mimeType: String, fileName: String?): String {
    return when {
        mimeType == "application/pdf" -> "PDF"
        mimeType.contains("word") -> "Word"
        mimeType.contains("sheet") || mimeType.contains("excel") -> stringResource(R.string.attachment_type_sheet)
        mimeType.contains("presentation") || mimeType.contains("powerpoint") -> stringResource(R.string.attachment_type_slides)
        mimeType.startsWith("text/") -> stringResource(R.string.attachment_type_text)
        else -> fileName
            ?.substringAfterLast('.', missingDelimiterValue = "")
            ?.takeIf { it.isNotBlank() }
            ?.uppercase()
            ?: stringResource(R.string.attachment_type_file)
    }
}

@Composable
private fun BoxScope.ClearAttachmentButton(onClear: (() -> Unit)?) {
    if (onClear == null) return
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        shape = RoundedCornerShape(50),
        modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(4.dp)
            .size(28.dp)
    ) {
        IconButton(onClick = onClear, modifier = Modifier.size(28.dp)) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(R.string.action_remove_attachment),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
