package com.twilio.conversations.app.common.enums

import com.twilio.conversations.app.R

enum class Reaction(val value: String, val emoji: Int) {
    HEART("heart", R.string.reaction_heart),
    LAUGH("laugh", R.string.reaction_laugh),
    SAD("sad", R.string.reaction_sad),
    THUMBS_UP("thumbs_up", R.string.reaction_thumbs_up),
    THUMBS_DOWN("thumbs_down", R.string.reaction_thumbs_down);

    companion object {
        private val valuesMap = values().associateBy { it.value }
        fun fromString(value: String) = valuesMap[value] ?: error("Invalid value $value for Reaction")
    }
}
