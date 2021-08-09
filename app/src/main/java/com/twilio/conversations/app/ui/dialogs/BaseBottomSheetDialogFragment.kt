package com.twilio.conversations.app.ui.dialogs

import android.view.View
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

open class BaseBottomSheetDialogFragment : BottomSheetDialogFragment() {

    override fun onStart() {
        super.onStart()

        // this forces the sheet to appear at max height even on landscape
        // https://stackoverflow.com/questions/41591733/bottom-sheet-landscape-issue
        val behavior = BottomSheetBehavior.from(requireView().parent as View)
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
    }
}
