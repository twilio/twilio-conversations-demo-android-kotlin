package com.twilio.conversations.app.testUtil

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.twilio.conversations.app.CONDITION_CHECK_INTERVAL
import com.twilio.conversations.app.CONDITION_CHECK_TIMEOUT
import com.twilio.conversations.app.R
import org.hamcrest.Description
import org.hamcrest.Matcher
import java.util.concurrent.TimeoutException

fun replaceDynamicUi(activity: Activity, viewId: Int) {
    val notAnimatedDrawable =
        ContextCompat.getDrawable(activity, R.drawable.ic_launcher)
    (activity.findViewById(viewId) as ProgressBar).indeterminateDrawable = notAnimatedDrawable
}

/**
 * Removes the indeterminate drawables of all ProgressBars found in the [activity] and keeps
 * observing the layout and processing any ProgressBars that are added dynamically
 */
fun removeProgressBarIndeterminateDrawables(activity: Activity) {
    fun clearChildProgressBarIndeterminateDrawables(view: View) {
        if (view is ViewGroup) {
            for (child in view.children) {
                clearChildProgressBarIndeterminateDrawables(child)
            }
        } else if (view is ProgressBar) {
            view.indeterminateDrawable = null
        }
    }

    val view = activity.window.decorView
    view.viewTreeObserver.addOnGlobalLayoutListener {
        clearChildProgressBarIndeterminateDrawables(view)
    }
    clearChildProgressBarIndeterminateDrawables(view)
}

inline fun <reified T : Activity> isVisible() : Boolean {
    val topResumedActivity = arrayOfNulls<Activity>(1)
    InstrumentationRegistry.getInstrumentation().runOnMainSync {
        val resumedActivities = ActivityLifecycleMonitorRegistry.getInstance()
            .getActivitiesInStage(Stage.RESUMED)
        if (resumedActivities.iterator().hasNext()) {
            topResumedActivity[0] = resumedActivities.iterator().next()
        }
    }
    return topResumedActivity[0]?.let { it::class.java.name == T::class.java.name } ?: false
}

inline fun <reified T : Activity> verifyActivityVisible() {
    val startTime = System.currentTimeMillis()
    while (!isVisible<T>()) {
        Thread.sleep(CONDITION_CHECK_INTERVAL)
        if (System.currentTimeMillis() - startTime >= CONDITION_CHECK_TIMEOUT) {
            throw AssertionError("Activity ${T::class.java.simpleName} not visible after $CONDITION_CHECK_TIMEOUT milliseconds")
        }
    }
}

fun switchConversationTab(name: String) = WaitForViewMatcher.performOnView(withText(name), click())

fun atPosition(position: Int, itemMatcher: Matcher<View>): Matcher<View> {

    return object : BoundedMatcher<View, RecyclerView>(RecyclerView::class.java) {
        override fun describeTo(description: Description) {
            description.appendText("has item at position $position: ")
            itemMatcher.describeTo(description)
        }

        override fun matchesSafely(view: RecyclerView): Boolean {
            val viewHolder = view.findViewHolderForAdapterPosition(position) ?: return false
            return itemMatcher.matches(viewHolder.itemView)
        }
    }
}

fun <T: View> BottomSheetBehavior<T>.waitUntilPopupStateChanged(state: Int, timeout: Long = 5000L) {
    val startTime = System.currentTimeMillis()
    val endTime = startTime + timeout
    do {
        if (this.state == state) return
        else Thread.sleep(50)
    } while (System.currentTimeMillis() < endTime)
    throw TimeoutException()
}

fun closeKeyboard(): ViewAction = closeSoftKeyboard()
