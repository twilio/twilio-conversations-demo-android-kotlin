package com.twilio.conversations.app.common.extensions

import android.content.Context
import androidx.core.content.ContextCompat
import com.twilio.conversations.app.R
import com.twilio.conversations.app.common.enums.SendStatus
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.yearsUntil
import java.text.SimpleDateFormat
import java.util.*

fun Long.asTimeString(): String = SimpleDateFormat("H:mm", Locale.getDefault()).format(Date(this))

fun Long.asDateString(): String = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date(this))

fun Long.asMessageCount(): String = if (this > 99) "99+" else this.toString()

fun Long.asMessageDateString() : String {
    if (this == 0L) {
        return ""
    }

    val instant = Instant.fromEpochMilliseconds(this)
    val now = Clock.System.now()
    val timeZone = TimeZone.currentSystemDefault()
    val days: Int = instant.daysUntil(now, timeZone)

    val dateFormat = if (days == 0) "H:mm" else "dd-MM-yyyy H:mm"
    return SimpleDateFormat(dateFormat, Locale.getDefault()).format(Date(this))
}

fun Long.asLastMessageDateString(context: Context) : String {
    if (this == 0L) {
        return ""
    }

    val instant = Instant.fromEpochMilliseconds(this)
    val now = Clock.System.now()
    val timeZone = TimeZone.currentSystemDefault()

    val days: Int = instant.daysUntil(now, timeZone)
    val weeks: Int = instant.weeksUntil(now, timeZone)
    val years: Int = instant.yearsUntil(now, timeZone)

    return when {
        years > 0 -> context.resources.getQuantityString(R.plurals.years_ago, years, years)
        weeks > 0 -> context.resources.getQuantityString(R.plurals.weeks_ago, weeks, weeks)
        days > 1 -> context.getString(R.string.days_ago, days)
        days == 1 -> context.getString(R.string.yesterday)
        else -> asTimeString() // today
    }
}

fun SendStatus.asLastMesageStatusIcon() = when(this) {
    SendStatus.SENDING -> R.drawable.ic_waiting_message
    SendStatus.SENT -> R.drawable.ic_sent_message
    SendStatus.ERROR -> R.drawable.ic_failed_message
    else -> 0
}

fun SendStatus.asLastMessageTextColor(context: Context) = when (this) {
    SendStatus.ERROR -> ContextCompat.getColor(context, R.color.colorAccent)
    else -> ContextCompat.getColor(context, R.color.text_subtitle)
}
