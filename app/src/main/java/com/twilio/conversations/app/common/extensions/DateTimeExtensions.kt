package com.twilio.conversations.app.common.extensions

import java.text.SimpleDateFormat
import java.util.*

fun Long.asTimeString(): String = SimpleDateFormat("H:mm", Locale.getDefault()).format(Date(this))

fun Long.asDateString(): String = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date(this))

fun Long.asMessageCount(): String = if (this > 99) "99+" else this.toString()
