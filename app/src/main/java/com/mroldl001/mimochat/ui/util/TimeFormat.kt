package com.mroldl001.mimochat.ui.util

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dateOnlyFormatter =
    DateTimeFormatter.ofPattern("yyyy/MM/dd", Locale.getDefault())
private val timeFormatter =
    DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
private val dateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm", Locale.getDefault())

fun formatDateOnly(timestamp: Long): String =
    dateOnlyFormatter.format(LocalDateTime.ofInstant(toInstant(timestamp), zoneId()))

fun formatTimeOnly(timestamp: Long): String =
    timeFormatter.format(LocalDateTime.ofInstant(toInstant(timestamp), zoneId()))

fun formatDateTime(timestamp: Long): String =
    dateTimeFormatter.format(LocalDateTime.ofInstant(toInstant(timestamp), zoneId()))

fun formatBubbleTimestamp(timestamp: Long, yesterdayTemplate: String): String {
    val zone = zoneId()
    val dateTime = LocalDateTime.ofInstant(toInstant(timestamp), zone)
    val today = LocalDate.now(zone)
    val date = dateTime.toLocalDate()
    return when {
        date == today -> timeFormatter.format(dateTime)
        date == today.minusDays(1) -> String.format(yesterdayTemplate, timeFormatter.format(dateTime))
        else -> dateTimeFormatter.format(dateTime)
    }
}

private fun toInstant(timestamp: Long): Instant = Instant.ofEpochMilli(timestamp)

private fun zoneId(): ZoneId = ZoneId.systemDefault()