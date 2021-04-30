package com.twilio.conversations.app.ui

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.rule.IntentsTestRule
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.espresso.matcher.ViewMatchers.Visibility.VISIBLE
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.twilio.conversations.app.R
import com.twilio.conversations.app.common.enums.ConversationsError
import com.twilio.conversations.app.data.ConversationsClientWrapper
import com.twilio.conversations.app.testUtil.replaceDynamicUi
import com.twilio.conversations.app.testUtil.verifyActivityVisible
import com.twilio.conversations.app.viewModel.LoginViewModel
import org.hamcrest.core.IsNot.not
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Cannot reliably test progressbar visibility by waiting on login - if device has no network connectivity,
 * login function will return before Espresso performs view visibility check. Test would fail, because activity
 * already would have received response and changed view visibility.
 *
 * Instead, check if correct views are displayed when observed data changes
 */
@RunWith(AndroidJUnit4::class)
class LoginActivityTest {

    @get:Rule
    var activityRule: IntentsTestRule<LoginActivity> = IntentsTestRule(LoginActivity::class.java)

    private lateinit var loginViewModel: LoginViewModel

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        ConversationsClientWrapper.recreateInstance(context)

        val activity = activityRule.activity
        loginViewModel = activity.loginViewModel

        replaceDynamicUi(activity, R.id.loginProgressBar)
    }

    @Test
    fun loginViewsDisplayedByDefault() {
        onView(withId(R.id.usernameTv)).check(matches(isDisplayed()))
        onView(withId(R.id.passwordTv)).check(matches(isDisplayed()))
        onView(withId(R.id.signInBtn)).check(matches(isDisplayed()))
        onView(withId(R.id.loginProgressBar)).check(matches(not(isDisplayed())))
    }

    @Test
    fun clickSignInButtonWithoutCredentials_displaysError() {
        onView(withId(R.id.signInBtn)).perform(closeSoftKeyboard(), click())
        onView(withText(R.string.sign_in_error)).check(matches(isDisplayed()))
    }

    @Test
    fun displaysProgressbarWhen_isLoading_inProgress_hidesWhenFinished() {
        activityRule.runOnUiThread {
            loginViewModel.isLoading.value = true
        }
        onView(withId(R.id.loginProgressBar)).check(matches(isDisplayed()))

        activityRule.runOnUiThread {
            loginViewModel.isLoading.value = false
        }
        onView(withId(R.id.loginProgressBar)).check(matches(not(isDisplayed())))
    }

    @Test
    fun displaysError_onSigninError() {
        activityRule.runOnUiThread {
            loginViewModel.onSignInError.value = ConversationsError.GENERIC_ERROR
        }

        // TODO: occasionally fails when view is displayed and visible on the screen
        // checking the effective visibility seems to find the view more reliably
        onView(withText(R.string.sign_in_error)).check(matches(withEffectiveVisibility(VISIBLE)))
    }

    @Test
    fun displaysConversationListActivity_onSigninSuccess() {
        activityRule.runOnUiThread {
            loginViewModel.onSignInSuccess.call()
        }

        verifyActivityVisible<ConversationListActivity>()
    }
}
