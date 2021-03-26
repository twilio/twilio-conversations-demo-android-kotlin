package com.twilio.conversations.app.testUtil

import android.view.View
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.ViewAssertion
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.espresso.util.TreeIterables
import org.hamcrest.Matcher
import timber.log.Timber

object WaitForViewMatcher {

    private const val WAIT_TIME_MAX = 5000
    private const val MATCH_RETRY_STEP = 100L
    private const val PERFORM_RETRY_COUNT = 3
    private const val ASSERT_RETRY_COUNT = 3

    fun performOnView(matcher: Matcher<View>, vararg actions: ViewAction) {
        for (i in 0..PERFORM_RETRY_COUNT) {
            try {
                actions.forEach {
                    waitForView(matcher).perform(it).withFailureHandler { _, _ ->
                        Timber.d("Failed to perform on view: $i")
                        if (i == ASSERT_RETRY_COUNT) {
                            throw Exception("Error performing view assertion")
                        }
                    }
                }
                break
            } catch (e: Exception) {
                Timber.d(e,"Failed to perform on view: $i")
                if (i == PERFORM_RETRY_COUNT) {
                    throw e
                }
                Thread.sleep(MATCH_RETRY_STEP)
            }
        }
    }

    fun assertOnView(matcher: Matcher<View>, vararg assertions: ViewAssertion) {
        for (i in 0..ASSERT_RETRY_COUNT) {
            try {
                assertions.forEach {
                    waitForView(matcher).check(it).withFailureHandler { _, _ ->
                        Timber.d("Failed to assert on view: $i")
                        if (i == ASSERT_RETRY_COUNT) {
                            throw Exception("Error performing view assertion")
                        }
                    }
                }
                break
            } catch (e: Exception) {
                Timber.d(e,"Failed to assert on view: $i")
                if (i == ASSERT_RETRY_COUNT) {
                    throw e
                }
                Thread.sleep(MATCH_RETRY_STEP)
            }
        }
    }

    private fun waitForView(viewMatcher: Matcher<View>): ViewInteraction {
        val maxTries = WAIT_TIME_MAX / MATCH_RETRY_STEP.toInt()
        for (i in 0..maxTries) {
            try {
                Timber.d("Wait for view: $i")
                onView(isRoot()).perform(searchFor(viewMatcher)).withFailureHandler { _, _ ->
                    Timber.d("Failed to wait for view: $i $maxTries")
                    if (i == maxTries) {
                        throw Exception("Failed to find view")
                    }
                }
                return onView(viewMatcher)
            } catch (e: Exception) {
                Timber.d(e, "Failed to wait for view: $i $maxTries")
                if (i == maxTries) {
                    throw e
                }
                Thread.sleep(MATCH_RETRY_STEP)
            }
        }

        throw Exception("Error finding a view matching $viewMatcher")
    }

    private fun searchFor(matcher: Matcher<View>) = object : ViewAction {

        override fun getConstraints() = isRoot()

        override fun getDescription() = "searching for view $matcher in the root view"

        override fun perform(uiController: UiController, view: View) {
            val childViews: Iterable<View> = TreeIterables.breadthFirstViewTraversal(view)

            childViews.forEach {
                if (matcher.matches(it)) return
            }

            throw NoMatchingViewException.Builder()
                .withRootView(view)
                .withViewMatcher(matcher)
                .build()
        }
    }
}
