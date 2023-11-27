package com.twilio.conversations.app.ui.fragments

import android.app.Dialog
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.window.OnBackInvokedDispatcher
import androidx.core.os.BuildCompat
import androidx.fragment.app.DialogFragment
import com.twilio.conversations.app.R

class SplashFragment : DialogFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_FRAME, R.style.AppTheme_NoActionBar)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return if (Build.VERSION.SDK_INT >= 33) {
            Dialog(requireContext(), theme).apply {
                onBackInvokedDispatcher.registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_DEFAULT) {
                    activity?.moveTaskToBack(true)
                }
            }
        } else {
            object : Dialog(requireContext(), theme) {
                @Deprecated("Deprecated in Java")
                override fun onBackPressed() {
                    activity?.moveTaskToBack(true)
                }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        inflater.inflate(R.layout.fragment_splash_screen, container, false)
}
