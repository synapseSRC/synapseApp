package com.synapse.social.studioasinc.core.monitoring

import android.app.Application
import com.synapse.social.studioasinc.BuildConfig
import com.synapse.social.studioasinc.data.repository.ai.SentryAgentTracing
import io.sentry.SentryOptions
import io.sentry.protocol.SentryTransaction
import io.sentry.android.core.SentryAndroid

object SentryInitializer {
    fun initialize(application: Application) {
        if (BuildConfig.SENTRY_DSN.isBlank()) return
        SentryAndroid.init(application) { options ->
            options.dsn = BuildConfig.SENTRY_DSN
            options.setDebug(BuildConfig.DEBUG)
            options.tracesSampleRate = 0.1
            options.dataCollection.userInfo = false
            options.dataCollection.httpBodies = emptySet()
            options.release = BuildConfig.APPLICATION_ID + "@" + BuildConfig.VERSION_NAME
            options.environment = if (BuildConfig.DEBUG) "debug" else "production"
            options.setBeforeSendTransaction(SentryOptions.BeforeSendTransactionCallback { transaction: SentryTransaction, _ ->
                if (SentryAgentTracing.hasUnsafeGenAiContent(transaction)) null else transaction
            })
        }
    }
}
