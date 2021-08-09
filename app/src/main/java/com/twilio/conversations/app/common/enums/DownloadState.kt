package com.twilio.conversations.app.common.enums

enum class DownloadState(val value: Int) {
    NOT_STARTED(0),
    DOWNLOADING(1),
    COMPLETED(2),
    ERROR(3);

    companion object {
        private val valuesMap = values().associateBy { it.value }
        fun fromInt(value: Int) = valuesMap[value] ?: error("Invalid value $value for DownloadState")
    }
}
