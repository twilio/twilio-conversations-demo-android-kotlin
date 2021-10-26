package com.twilio.conversations.app.common.extensions

import android.app.Activity
import android.app.DownloadManager
import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import android.view.KeyEvent.ACTION_DOWN
import android.view.KeyEvent.KEYCODE_ENTER
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LiveData
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputLayout
import com.twilio.conversations.app.R
import com.twilio.conversations.app.common.enums.ConversationsError
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.until

fun FragmentActivity.hideKeyboard() {
    val view = currentFocus ?: window.decorView
    val token = view.windowToken
    view.clearFocus()
    ContextCompat.getSystemService(this, InputMethodManager::class.java)?.hideSoftInputFromWindow(token, 0)
}

val Fragment.applicationContext get() = requireContext().applicationContext

fun EditText.onSubmit(func: () -> Unit) = setOnEditorActionListener { _, actionId, keyEvent ->

    if (actionId == EditorInfo.IME_ACTION_DONE
        || (keyEvent.action == ACTION_DOWN && keyEvent.keyCode == KEYCODE_ENTER)
    ) {
        hideKeyboard()
        func()
        return@setOnEditorActionListener true
    }

    return@setOnEditorActionListener false
}

fun View.hideKeyboard() {
    val imm = context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.hideSoftInputFromWindow(windowToken, 0)
}

fun BottomSheetBehavior<*>.isShowing() = state == BottomSheetBehavior.STATE_EXPANDED

fun BottomSheetBehavior<*>.show() {
    if (!isShowing()) {
        state = BottomSheetBehavior.STATE_EXPANDED
    }
}

fun BottomSheetBehavior<*>.hide() {
    if (state != BottomSheetBehavior.STATE_HIDDEN) {
        state = BottomSheetBehavior.STATE_HIDDEN
    }
}

fun Context.getErrorMessage(
    error: ConversationsError,
    @StringRes defaultId: Int = R.string.err_conversation_generic_error
): String {
    return when (error) {
        ConversationsError.CONVERSATION_CREATE_FAILED -> getString(R.string.err_failed_to_create_conversation)
        ConversationsError.CONVERSATION_JOIN_FAILED -> getString(R.string.err_failed_to_join_conversation)
        ConversationsError.CONVERSATION_REMOVE_FAILED -> getString(R.string.err_failed_to_remove_conversation)
        ConversationsError.CONVERSATION_LEAVE_FAILED -> getString(R.string.err_failed_to_leave_conversation)
        ConversationsError.CONVERSATION_FETCH_USER_FAILED -> getString(R.string.err_failed_to_fetch_user_conversations)
        ConversationsError.CONVERSATION_MUTE_FAILED -> getString(R.string.err_failed_to_mute_conversations)
        ConversationsError.CONVERSATION_UNMUTE_FAILED -> getString(R.string.err_failed_to_unmute_conversation)
        ConversationsError.CONVERSATION_RENAME_FAILED -> getString(R.string.err_failed_to_rename_conversation)
        ConversationsError.REACTION_UPDATE_FAILED -> getString(R.string.err_failed_to_update_reaction)
        ConversationsError.PARTICIPANTS_FETCH_FAILED -> getString(R.string.err_failed_to_fetch_participants)
        ConversationsError.PARTICIPANT_ADD_FAILED -> getString(R.string.err_failed_to_add_participant)
        ConversationsError.PARTICIPANT_REMOVE_FAILED -> getString(R.string.err_failed_to_remove_participant)
        ConversationsError.USER_UPDATE_FAILED -> getString(R.string.err_failed_to_update_user)
        ConversationsError.MESSAGE_MEDIA_DOWNLOAD_FAILED -> getString(R.string.err_failed_to_download_media)
        ConversationsError.SIGN_OUT_SUCCEEDED -> getString(R.string.sign_out_succeeded)
        ConversationsError.MESSAGE_REMOVE_FAILED -> getString(R.string.err_failed_to_remove_message)
        ConversationsError.MESSAGE_COPY_FAILED -> getString(R.string.err_failed_to_copy_message)

        else -> getString(defaultId)
    }
}

fun View.showSnackbar(message: String, anchorViewId: Int? = null) {
    Snackbar.make(this, message, Snackbar.LENGTH_SHORT).apply {
        anchorViewId?.let { setAnchorView(it) }
        show()
    }
}

fun View.showSnackbar(@StringRes resId: Int, anchorViewId: Int? = null) =
    showSnackbar(context.getString(resId), anchorViewId)

inline fun OnSnackbarDismissed(crossinline block: (event: Int) -> Unit) =
    object : BaseTransientBottomBar.BaseCallback<Snackbar>() {
        override fun onDismissed(transientBottomBar: Snackbar, event: Int) = block(event)
    }

inline fun Snackbar.onDismissed(crossinline block: (event: Int) -> Unit) = addCallback(OnSnackbarDismissed(block))

fun <T> LiveData<T>.requireValue() = value ?: error("Not null LiveData value required")

fun AppCompatActivity.showToast(resId: Int) {
    Toast.makeText(this, resId, Toast.LENGTH_LONG).show()
}

fun ContentResolver.getString(uri: Uri, columnName: String): String? {
    val cursor = query(uri, arrayOf(columnName), null, null, null)
    return cursor?.let {
        it.moveToFirst()
        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        val name = cursor.getString(index)
        it.close()
        return@let name
    }
}

fun Cursor.getInt(columnName: String): Int {
    val index = getColumnIndex(columnName)
    return getInt(index)
}

fun Cursor.getLong(columnName: String): Long {
    val index = getColumnIndex(columnName)
    return getLong(index)
}

fun Cursor.getString(columnName: String): String {
    val index = getColumnIndex(columnName)
    return getString(index)
}

fun DownloadManager.queryById(id: Long): Cursor =
    query(DownloadManager.Query().apply {
        setFilterById(id)
    })

fun Instant.weeksUntil(other: Instant, timeZone: TimeZone): Int =
    until(other, DateTimeUnit.WEEK, timeZone).toInt()

fun TextInputLayout.enableErrorResettingOnTextChanged() {
    editText?.doOnTextChanged { _, _, _, _ ->
        error = null
    }
}
