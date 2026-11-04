package com.synapse.social.studioasinc.shared.data.passkey

import android.app.Activity
import android.app.Application
import android.os.Bundle
import java.lang.ref.WeakReference

/**
 * Holds the currently resumed Activity so Credential Manager can show its prompt.
 *
 * Credential Manager refuses to launch any UI unless it is handed an Activity-based context, but
 * [AndroidPasskeyPlatformManager] is an application-scoped singleton that must not retain an
 * Activity itself. Register an instance with [Application.registerActivityLifecycleCallbacks] at
 * process start.
 */
class PasskeyActivityProvider : Application.ActivityLifecycleCallbacks {

    private var resumedActivity: WeakReference<Activity>? = null

    val currentActivity: Activity?
        get() = resumedActivity?.get()

    override fun onActivityResumed(activity: Activity) {
        resumedActivity = WeakReference(activity)
    }

    override fun onActivityPaused(activity: Activity) {
        if (resumedActivity?.get() === activity) {
            resumedActivity = null
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

    override fun onActivityStarted(activity: Activity) {}

    override fun onActivityStopped(activity: Activity) {}

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    override fun onActivityDestroyed(activity: Activity) {}
}
