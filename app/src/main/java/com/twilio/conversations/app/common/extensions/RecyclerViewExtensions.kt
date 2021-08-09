package com.twilio.conversations.app.common.extensions

import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton

fun RecyclerView.addFabExtendingOnScrollListener(extendedFab: ExtendedFloatingActionButton) =
    addOnScrollListener(FabExtendingOnScrollListener(extendedFab))

private class FabExtendingOnScrollListener(
    private val floatingActionButton: ExtendedFloatingActionButton
) : RecyclerView.OnScrollListener() {

    override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
        if (newState == RecyclerView.SCROLL_STATE_IDLE
            && recyclerView.computeVerticalScrollOffset() == 0
            && !floatingActionButton.isExtended
        ) {
            floatingActionButton.extend()
        }
        super.onScrollStateChanged(recyclerView, newState)
    }

    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        if (dy != 0 && floatingActionButton.isExtended) {
            floatingActionButton.shrink()
        }
        super.onScrolled(recyclerView, dx, dy)
    }
}
