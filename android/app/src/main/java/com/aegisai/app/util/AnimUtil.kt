package com.aegisai.app.util

import android.app.Activity
import android.view.View
import android.view.animation.AnimationUtils
import com.aegisai.app.R

object AnimUtil {
    fun fadeInUp(view: View) {
        view.alpha = 0f
        view.translationY = 48f
        view.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(420)
            .setInterpolator(android.view.animation.DecelerateInterpolator())
            .start()
    }

    fun pulse(view: View) {
        view.animate().scaleX(0.96f).scaleY(0.96f).setDuration(80).withEndAction {
            view.animate().scaleX(1f).scaleY(1f).setDuration(120).start()
        }.start()
    }

    fun activityOpen(activity: Activity) {
        activity.overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }

    fun activityClose(activity: Activity) {
        activity.overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }

    fun shake(view: View) {
        val anim = AnimationUtils.loadAnimation(view.context, android.R.anim.slide_in_left)
        view.startAnimation(anim)
    }
}
