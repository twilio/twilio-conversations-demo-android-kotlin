package com.twilio.conversations.app.common.extensions

import android.app.DownloadManager
import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.snackbar.Snackbar
import com.twilio.conversations.app.R
import com.twilio.conversations.app.common.enums.ConversationsError

fun FragmentActivity.hideKeyboard() {
    val view = currentFocus ?: window.decorView
    val token = view.windowToken
    view.clearFocus()
    ContextCompat.getSystemService(this, InputMethodManager::class.java)?.hideSoftInputFromWindow(token, 0)
}

fun BottomSheetBehavior<View>.isShowing() = state == BottomSheetBehavior.STATE_EXPANDED

fun BottomSheetBehavior<View>.show() {
    if (!isShowing()) {
        state = BottomSheetBehavior.STATE_EXPANDED
    }
}

fun BottomSheetBehavior<View>.hide() {
    if (state != BottomSheetBehavior.STATE_HIDDEN) {
        state = BottomSheetBehavior.STATE_HIDDEN
    }
}

fun Context.getErrorMessage(error: ConversationsError): String {
    return when (error) {
        ConversationsError.CONVERSATION_CREATE_FAILED -> getString(R.string.err_failed_to_create_conversation)
        ConversationsError.CONVERSATION_JOIN_FAILED -> getString(R.string.err_failed_to_join_conversation)
        ConversationsError.CONVERSATION_REMOVE_FAILED -> getString(R.string.err_failed_to_remove_conversation)
        ConversationsError.CONVERSATION_LEAVE_FAILED -> getString(R.string.err_failed_to_leave_conversation)
        ConversationsError.CONVERSATION_FETCH_USER_FAILED -> getString(R.string.err_failed_to_fetch_user_conversations)
        ConversationsError.CONVERSATION_MUTE_FAILED -> getString(R.string.err_failed_to_mute_conversations)
        ConversationsError.CONVERSATION_UNMUTE_FAILED -> getString(R.string.err_failed_to_unmute_conversation)
        ConversationsError.CONVERSATION_RENAME_FAILED-> getString(R.string.err_failed_to_rename_conversation)
        ConversationsError.REACTION_UPDATE_FAILED -> getString(R.string.err_failed_to_update_reaction)
        ConversationsError.PARTICIPANTS_FETCH_FAILED -> getString(R.string.err_failed_to_fetch_participants)
        ConversationsError.PARTICIPANT_ADD_FAILED -> getString(R.string.err_failed_to_add_participant)
        ConversationsError.PARTICIPANT_REMOVE_FAILED -> getString(R.string.err_failed_to_remove_participant)
        ConversationsError.USER_UPDATE_FAILED -> getString(R.string.err_failed_to_update_user)
        ConversationsError.MESSAGE_MEDIA_DOWNLOAD_FAILED -> getString(R.string.err_failed_to_download_media)
        else -> getString(R.string.err_conversation_generic_error)
    }
}

fun CoordinatorLayout.showSnackbar(message: String) {
    Snackbar.make(this, message, Snackbar.LENGTH_SHORT).show()
}

fun AppCompatActivity.showToast(resId: Int) {
    Toast.makeText(this, resId, Toast.LENGTH_LONG).show()
}

fun ContentResolver.getString(uri: Uri, columnName: String): String? {
    val cursor = query(uri, arrayOf(columnName), null, null, null)
    return cursor?.let {
        it.moveToFirst()
        val name = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
        it.close()
        return@let name
    }
}

fun Cursor.getInt(columnName: String): Int = getInt(getColumnIndex(columnName))

fun Cursor.getLong(columnName: String): Long = getLong(getColumnIndex(columnName))

fun Cursor.getString(columnName: String): String = getString(getColumnIndex(columnName))

fun DownloadManager.queryById(id: Long): Cursor =
    query(DownloadManager.Query().apply {
        setFilterById(id)
    })
