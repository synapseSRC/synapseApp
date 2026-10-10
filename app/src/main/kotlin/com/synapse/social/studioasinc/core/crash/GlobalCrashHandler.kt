package com.synapse.social.studioasinc.core.crash

import android.content.Context
import android.content.Intent
import com.synapse.social.studioasinc.feature.crash.CrashActivity

class GlobalCrashHandler(
    private val context: Context,
    private val defaultHandler: Thread.UncaughtExceptionHandler?
) : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        val log = throwable.stackTraceToString()
        val intent = Intent(context, CrashActivity::class.java).apply {
            putExtra(CrashActivity.EXTRA_CRASH_LOG, log)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        context.startActivity(intent)
        defaultHandler?.uncaughtException(thread, throwable)
    }

    companion object {
        fun install(context: Context) {
            val default = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler(GlobalCrashHandler(context, default))
        }
    }
}
