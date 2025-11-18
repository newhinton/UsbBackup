package de.felixnuesse.usbbackup.extension

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.view.View

fun View.visible(isVisible: Boolean) {
    this.visibility = if (isVisible) {
        View.VISIBLE
    } else {
        View.GONE
    }
}


fun View.visibleAnimated(isVisible: Boolean) {
    var view = this
    if (isVisible) {
        view.animate()
            .alpha(1f)
            .setDuration(250)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    view.visibility = View.VISIBLE
                }
            })
    } else {
        view.animate()
            .alpha(0f)
            .setDuration(250)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    view.visibility = View.GONE
                }
            })
    }
}
