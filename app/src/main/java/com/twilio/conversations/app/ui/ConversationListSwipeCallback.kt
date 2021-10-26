package com.twilio.conversations.app.ui

import android.content.Context
import android.graphics.Canvas
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ItemTouchHelper.LEFT
import androidx.recyclerview.widget.ItemTouchHelper.RIGHT
import androidx.recyclerview.widget.RecyclerView
import com.twilio.conversations.app.R
import com.twilio.conversations.app.adapters.ConversationListAdapter
import kotlin.math.max
import kotlin.math.min

class ConversationListSwipeCallback(val context: Context, val adapter: ConversationListAdapter) :
    ItemTouchHelper.SimpleCallback(0, LEFT or RIGHT) {

    var onLeave: (String) -> Unit = {}

    var onMute: (String) -> Unit = {}

    var onUnMute: (String) -> Unit = {}

    private val leaveIcon = ContextCompat.getDrawable(context, R.drawable.ic_swipe_leave)!!

    private val muteIcon = ContextCompat.getDrawable(context, R.drawable.ic_swipe_mute)!!

    private val unMuteIcon = ContextCompat.getDrawable(context, R.drawable.ic_swipe_unmute)!!

    private val leaveBackground = ContextCompat.getDrawable(context, R.color.colorAccent)!!

    private val muteBackground = ContextCompat.getDrawable(context, R.color.swipe_mute_background)!!

    private val swipeLimit = context.resources.getDimension(R.dimen.swipe_limit)

    private var swipeBack = false

    private var onSwipeBackAction = {}

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ) = false

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) = Unit

    override fun onChildDraw(
        canvas: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        recyclerView.setOnTouchListener { _, event ->
            swipeBack = event.action == MotionEvent.ACTION_CANCEL || event.action == MotionEvent.ACTION_UP
            false
        }

        val position = viewHolder.adapterPosition
        val conversationSid = adapter.conversations[position].sid

        when {
            dX > swipeLimit && adapter.isMuted(position) -> onSwipeBackAction = { onUnMute(conversationSid) }

            dX > swipeLimit && !adapter.isMuted(position) -> onSwipeBackAction = { onMute(conversationSid) }

            dX < -swipeLimit -> onSwipeBackAction = { onLeave(conversationSid) }

            else -> onSwipeBackAction = {}
        }

        val limitedDX = max(-swipeLimit, min(dX, swipeLimit))
        super.onChildDraw(canvas, recyclerView, viewHolder, limitedDX, dY, actionState, isCurrentlyActive)

        val itemView = viewHolder.itemView

        if (limitedDX > 0) { // Swiping to the right - mute/unmute conversation
            drawMuteIcon(canvas, itemView, limitedDX, position)
        } else if (limitedDX < 0) { // Swiping to the left - leave conversation
            drawLeaveIcon(canvas, itemView, limitedDX)
        }
    }

    override fun convertToAbsoluteDirection(flags: Int, layoutDirection: Int): Int {
        if (swipeBack) {
            swipeBack = false
            onSwipeBackAction()
            return 0
        }
        return super.convertToAbsoluteDirection(flags, layoutDirection)
    }

    private fun drawMuteIcon(canvas: Canvas, itemView: View, dX: Float, position: Int) {
        val isMuted = adapter.isMuted(position)
        val icon = if (isMuted) unMuteIcon else muteIcon
        val iconMargin = (itemView.height - icon.intrinsicHeight) / 2
        val iconTop = itemView.top + (itemView.height - icon.intrinsicHeight) / 2
        val iconBottom = iconTop + icon.intrinsicHeight
        val iconLeft = itemView.left + iconMargin
        val iconRight = iconLeft + icon.intrinsicWidth

        icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
        muteBackground.setBounds(itemView.left, itemView.top, itemView.left + dX.toInt(), itemView.bottom)
        muteBackground.draw(canvas)
        icon.draw(canvas)
    }

    private fun drawLeaveIcon(canvas: Canvas, itemView: View, dX: Float) {
        val iconMargin = (itemView.height - leaveIcon.intrinsicHeight) / 2
        val iconTop = itemView.top + (itemView.height - leaveIcon.intrinsicHeight) / 2
        val iconBottom = iconTop + leaveIcon.intrinsicHeight
        val iconLeft = itemView.right - iconMargin - leaveIcon.intrinsicWidth
        val iconRight = itemView.right - iconMargin

        leaveIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
        leaveBackground.setBounds(itemView.right + dX.toInt(), itemView.top, itemView.right, itemView.bottom)
        leaveBackground.draw(canvas)
        leaveIcon.draw(canvas)
    }
}
