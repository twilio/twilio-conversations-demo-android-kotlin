package com.twilio.conversations.app.common

import timber.log.Timber

/**
 * Makes logged out class names clickable in Logcat
 */
class LineNumberDebugTree(private val tag: String) : Timber.DebugTree() {
    override fun createStackElementTag(element: StackTraceElement) =
        "$tag: (${element.fileName}:${element.lineNumber}) #${element.methodName} "
}
