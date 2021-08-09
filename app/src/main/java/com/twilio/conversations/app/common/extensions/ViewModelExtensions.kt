package com.twilio.conversations.app.common.extensions

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

// https://proandroiddev.com/view-model-creation-in-android-android-architecture-components-kotlin-ce9f6b93a46b
inline fun <reified T : ViewModel> FragmentActivity.getViewModel(noinline creator: (() -> T)? = null): T {
    return if (creator == null)
        ViewModelProvider(this).get(T::class.java)
    else
        ViewModelProvider(this, BaseViewModelFactory(creator)).get(T::class.java)
}

inline fun <reified T : ViewModel> Fragment.getViewModel(noinline creator: (() -> T)? = null): T {
    return if (creator == null)
        ViewModelProvider(this).get(T::class.java)
    else
        ViewModelProvider(this, BaseViewModelFactory(creator)).get(T::class.java)
}

inline fun <reified T : ViewModel> FragmentActivity.lazyViewModel(noinline creator: (() -> T)? = null) =
    lazy { getViewModel(creator) }

inline fun <reified T : ViewModel> Fragment.lazyActivityViewModel(noinline creator: (() -> T)? = null) =
    lazy { requireActivity().getViewModel(creator) }

inline fun <reified T : ViewModel> Fragment.lazyViewModel(noinline creator: (() -> T)? = null) =
    lazy { getViewModel(creator) }

class BaseViewModelFactory<T>(val creator: () -> T) : ViewModelProvider.Factory {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return creator() as T
    }
}
