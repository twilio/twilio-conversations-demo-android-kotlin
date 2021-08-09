package com.twilio.conversations.app.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import com.twilio.conversations.app.R
import kotlin.math.PI
import kotlin.math.tan

const val kLineAngleDegrees = 12
const val kLineAngleRadians = kLineAngleDegrees * PI.toFloat() / 180

class LoginBackgroundDrawable(context: Context) : Drawable() {

    val topColor = ContextCompat.getColor(context, R.color.colorPrimaryDark)
    val bottomColor = ContextCompat.getColor(context, R.color.splashBottom)

    val path = Path()

    override fun draw(canvas: Canvas) {
        val width = bounds.width().toFloat()
        val height = bounds.height().toFloat()

        val rightY = height / 2
        val leftY = rightY + width * tan(kLineAngleRadians)

        path.reset()
        path.moveTo(0f, 0f)
        path.lineTo(width, 0f)
        path.lineTo(width, rightY)
        path.lineTo(0f, leftY)
        path.lineTo(0f, 0f)

        canvas.drawColor(bottomColor)
        canvas.clipPath(path)
        canvas.drawColor(topColor)
    }

    override fun setAlpha(alpha: Int) = Unit

    override fun setColorFilter(colorFilter: ColorFilter?) = Unit

    override fun getOpacity() = PixelFormat.OPAQUE
}
