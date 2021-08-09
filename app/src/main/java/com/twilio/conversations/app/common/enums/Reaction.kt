package com.twilio.conversations.app.common.enums

import android.content.Context
import com.twilio.conversations.app.R

enum class Reaction(val value: String, val emoji: Int, val sortOrder: Int) {
    HEART("heart", R.string.reaction_heart, 0),
    THUMBS_UP("thumbs_up", R.string.reaction_thumbs_up, 1),
    LAUGH("laugh", R.string.reaction_laugh, 2),
    SAD("sad", R.string.reaction_sad, 3),
    POUTING("pouting", R.string.reaction_pouting, 4),
    THUMBS_DOWN("thumbs_down", R.string.reaction_thumbs_down,5);

    companion object {
        private val valuesMap = values().associateBy { it.value }

        private lateinit var emojiMap: Map<String, Reaction>

        fun fromString(value: String) = valuesMap[value] ?: error("Invalid value $value for Reaction")

        fun fromEmoji(context: Context, emoji: String): Reaction {
            if (!this::emojiMap.isInitialized) {
                emojiMap = values().associateBy { context.getString(it.emoji) }
            }
            return emojiMap[emoji] ?: error("Invalid emoji $emoji for Reaction")
        }
    }
}

typealias Reactions = Map<Reaction, Set<String>> // maps a reaction to set of identities
