package com.mroldl001.mimochat.ui.util

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

data class MediaMeta(
    val thumbnail: Bitmap?,
    val durationMs: Int
)

private object MediaMetaCache {
    val entries = LinkedHashMap<String, MediaMeta>(16, 0.75f, true)
    val mutex = Mutex()
    const val MAX_ENTRIES = 16

    fun get(uri: String): MediaMeta? = synchronized(entries) { entries[uri] }

    fun put(uri: String, value: MediaMeta) {
        synchronized(entries) {
            if (entries.size >= MAX_ENTRIES && !entries.containsKey(uri)) {
                val oldest = entries.keys.firstOrNull()
                if (oldest != null) {
                    entries.remove(oldest)
                }
            }
            entries[uri] = value
        }
    }

    fun evict(uri: String) {
        synchronized(entries) { entries.remove(uri) }
    }
}

suspend fun loadMediaMeta(context: Context, uri: String): MediaMeta {
    MediaMetaCache.get(uri)?.let { return it }
    return MediaMetaCache.mutex.withLock {
        MediaMetaCache.get(uri)?.let { return@withLock it }
        val meta = withContext(Dispatchers.IO) {
            runCatching {
                MediaMetadataRetriever().let { retriever ->
                    try {
                        retriever.setDataSource(context, Uri.parse(uri))
                        MediaMeta(
                            thumbnail = retriever.getFrameAtTime(
                                0L,
                                MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                            ),
                            durationMs = retriever
                                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                                ?.toIntOrNull() ?: 0
                        )
                    } finally {
                        retriever.release()
                    }
                }
            }.getOrElse { MediaMeta(thumbnail = null, durationMs = 0) }
        }
        MediaMetaCache.put(uri, meta)
        meta
    }
}

fun evictMediaMeta(uri: String) {
    MediaMetaCache.evict(uri)
}