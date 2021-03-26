package com.twilio.conversations.app.common

import android.view.View
import com.google.android.material.bottomsheet.BottomSheetBehavior

class SheetListener(
    private val sheetBackground: View,
    private val onHidden: () -> Unit = {}
) : BottomSheetBehavior.BottomSheetCallback() {

    override fun onSlide(bottomSheet: View, slideOffset: Float) {
        sheetBackground.visibility = View.VISIBLE
        sheetBackground.alpha = slideOffset
        if (slideOffset < -0.15f) {
            onHidden()
        }
    }

    override fun onStateChanged(bottomSheet: View, newState: Int) {
        if (newState == BottomSheetBehavior.STATE_HIDDEN || newState == BottomSheetBehavior.STATE_COLLAPSED) {
            sheetBackground.visibility = View.GONE
            sheetBackground.alpha = 0f
            onHidden()
        }
    }
}
