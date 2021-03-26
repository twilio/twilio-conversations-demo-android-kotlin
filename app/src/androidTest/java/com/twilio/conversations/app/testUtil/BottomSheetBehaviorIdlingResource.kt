package com.twilio.conversations.app.testUtil

import android.view.View
import androidx.test.espresso.IdlingResource
import androidx.test.espresso.IdlingResource.ResourceCallback
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback

class BottomSheetBehaviorIdlingResource(behavior: BottomSheetBehavior<*>) : BottomSheetCallback(),
    IdlingResource {

    private var isIdle: Boolean
    private var callback: ResourceCallback? = null

    init {
        behavior.addBottomSheetCallback(this)
        isIdle = isIdleState(behavior.state)
    }

    override fun getName(): String {
        return BottomSheetBehaviorIdlingResource::class.java.simpleName
    }

    override fun isIdleNow(): Boolean {
        return isIdle
    }

    override fun registerIdleTransitionCallback(callback: ResourceCallback) {
        this.callback = callback
    }

    override fun onStateChanged(bottomSheet: View, @BottomSheetBehavior.State newState: Int) {
        val wasIdle = isIdle
        isIdle = isIdleState(newState)
        if (!wasIdle && isIdle) {
            callback?.onTransitionToIdle()
        }
    }

    override fun onSlide(bottomSheet: View, slideOffset: Float) {}

    private fun isIdleState(state: Int): Boolean {
        return state != BottomSheetBehavior.STATE_DRAGGING && state != BottomSheetBehavior.STATE_SETTLING
    }
}
