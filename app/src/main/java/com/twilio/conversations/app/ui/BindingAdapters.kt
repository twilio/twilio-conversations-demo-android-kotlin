package com.twilio.conversations.app.ui

import android.content.res.ColorStateList
import android.widget.ImageView
import androidx.annotation.ColorInt
import androidx.core.widget.ImageViewCompat
import androidx.databinding.BindingAdapter

@BindingAdapter("android:src")
fun setImageViewResource(imageView: ImageView, resource: Int) {
    imageView.setImageResource(resource)
}

@BindingAdapter("tint")
fun setImageViewTint(imageView: ImageView, @ColorInt color: Int) {
    ImageViewCompat.setImageTintList(imageView, ColorStateList.valueOf(color));
}
