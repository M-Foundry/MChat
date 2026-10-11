package com.mroldl001.mimochat.ui.chat.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mroldl001.mimochat.R
import com.mroldl001.mimochat.domain.model.Chat
import com.mroldl001.mimochat.ui.util.formatDateOnly

internal val CHAT_HISTORY_ITEM_HEIGHT = 72.dp

@Composable
internal fun ChatSelectionHighlight(
    listState: LazyListState,
    selectedIndex: Int,
    selectedChatId: Long? = null,
    settledChatId: MutableState<Long?>,
    isAnimating: MutableState<Boolean>,
    durationMillis: Int = 360,
    itemHeight: Dp = CHAT_HISTORY_ITEM_HEIGHT
) {
    if (selectedIndex < 0) return

    val animatable = remember { Animatable(0f) }
    var prevIndex by remember { mutableStateOf(selectedIndex) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { translationY = animatable.value }
            .padding(horizontal = 8.dp)
            .height(itemHeight)
            .clip(RoundedCornerShape(20.dp))
            .background(
                MaterialTheme.colorScheme.primary.copy(
                    alpha = if (isAnimating.value) 0.14f else 0f
                )
            )
    )

    LaunchedEffect(selectedChatId, selectedIndex) {
        if (selectedIndex == prevIndex) {
            settledChatId.value = selectedChatId
            return@LaunchedEffect
        }
        val oldOffset = listState.layoutInfo.visibleItemsInfo
            .firstOrNull { it.index == prevIndex }?.offset
        val newOffset = listState.layoutInfo.visibleItemsInfo
            .firstOrNull { it.index == selectedIndex }?.offset
        if (oldOffset != null && newOffset != null) {
            isAnimating.value = true
            animatable.snapTo(oldOffset.toFloat())
            try {
                animatable.animateTo(
                    newOffset.toFloat(),
                    animationSpec = tween(durationMillis = durationMillis, easing = FastOutSlowInEasing)
                )
            } finally {
                isAnimating.value = false
            }
        }
        prevIndex = selectedIndex
        settledChatId.value = selectedChatId
    }
}

@Composable
internal fun ChatListItem(
    chat: Chat,
    isSelected: Boolean,
    settledSelectedChatId: Long?,
    isAnimating: Boolean = false,
    contentOffsetDurationMillis: Int = 240,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val contentOffset by animateDpAsState(
        targetValue = if (isSelected) 8.dp else 0.dp,
        animationSpec = tween(durationMillis = contentOffsetDurationMillis, easing = FastOutSlowInEasing),
        label = "chatItemContentOffset"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .background(
                color = MaterialTheme.colorScheme.primary.copy(
                    alpha = if (chat.id == settledSelectedChatId && !isAnimating) 0.14f else 0f
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .height(CHAT_HISTORY_ITEM_HEIGHT)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .graphicsLayer { translationX = contentOffset.toPx() }
        ) {
            Text(
                text = chat.title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = formatDateOnly(chat.updatedAt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = stringResource(R.string.delete),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}