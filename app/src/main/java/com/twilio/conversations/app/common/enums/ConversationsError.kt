package com.twilio.conversations.app.common.enums

import com.twilio.conversations.ErrorInfo

enum class ConversationsError(val code: Int, val message: String) {
    UNKNOWN(-1, "Unknown error"),
    NO_ERROR(55, "No error"),
    EMPTY_USERNAME(56, "Username is empty"),
    EMPTY_PASSWORD(57, "Password is empty"),
    EMPTY_USERNAME_AND_PASSWORD(58, "Username and password are empty"),
    TOKEN_ERROR(59, "Could not get token"),
    GENERIC_ERROR(60, "Could not create client"),
    TOKEN_ACCESS_DENIED(61, "Access denied"),
    NO_STORED_CREDENTIALS(62, "No credentials in storage"),
    CONVERSATION_JOIN_FAILED(63, "Failed to join conversation"),
    CONVERSATION_CREATE_FAILED(64, "Failed to create conversation"),
    CONVERSATION_REMOVE_FAILED(65, "Failed to destroy conversation"),
    CONVERSATION_LEAVE_FAILED(66, "Failed to leave conversation"),
    CONVERSATION_FETCH_USER_FAILED(67, "Failed to fetch user conversations"),
    CONVERSATION_MUTE_FAILED(69, "Failed to mute conversation"),
    CONVERSATION_UNMUTE_FAILED(70, "Failed to unmute conversation"),
    CONVERSATION_RENAME_FAILED(71, "Failed to rename conversation"),
    CONVERSATION_GET_FAILED(72, "Failed to get conversation"),
    MESSAGE_FETCH_FAILED(73, "Failed to fetch messages"),
    MESSAGE_SEND_FAILED(74, "Failed to send message"),
    REACTION_UPDATE_FAILED(75, "Failed to update reaction"),
    PARTICIPANTS_FETCH_FAILED(76, "Failed to fetch participants"),
    PARTICIPANT_ADD_FAILED(77, "Failed to add participant"),
    PARTICIPANT_REMOVE_FAILED(78, "Failed to remove participant"),
    USER_UPDATE_FAILED(79, "Failed to update user"),
    MESSAGE_MEDIA_DOWNLOAD_FAILED(80, "Failed to download media"),
    SIGN_OUT_SUCCEEDED(81, "Successfully signed out"),
    MESSAGE_REMOVE_FAILED(82, "Failed to remove message"),
    MESSAGE_COPY_FAILED(82, "Failed to copy message"),
    NO_INTERNET_CONNECTION(83, "No internet connection");

    override fun toString() = "Error $code : $message"

    companion object {
        fun fromErrorInfo(errorInfo: ErrorInfo) = values().firstOrNull { it.code == errorInfo.code } ?: UNKNOWN
    }
}
