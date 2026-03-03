package com.synapse.social.studioasinc.core.ui.animation

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.ActivityOptionsCompat
import com.synapse.social.studioasinc.R



object ActivityTransitions {



    fun startActivityWithTransition(context: Context, intent: Intent) {
        context.startActivity(intent)
        if (context is Activity) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                context.overrideActivityTransition(
                    Activity.OVERRIDE_TRANSITION_OPEN,
                    R.anim.activity_open_enter,
                    R.anim.activity_open_exit
                )
            } else {
                @Suppress("DEPRECATION")
                context.overridePendingTransition(
                    R.anim.activity_open_enter,
                    R.anim.activity_open_exit
                )
            }
        }
    }



    fun finishWithTransition(activity: Activity) {
        activity.finish()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            activity.overrideActivityTransition(
                Activity.OVERRIDE_TRANSITION_CLOSE,
                R.anim.activity_close_enter,
                R.anim.activity_close_exit
            )
        } else {
            @Suppress("DEPRECATION")
            activity.overridePendingTransition(
                R.anim.activity_close_enter,
                R.anim.activity_close_exit
            )
        }
    }
}



fun Activity.startActivityWithPremiumTransition(intent: Intent) {
    ActivityTransitions.startActivityWithTransition(this, intent)
}



fun Activity.finishWithPremiumTransition() {
    ActivityTransitions.finishWithTransition(this)
}
