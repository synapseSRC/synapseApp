package com.synapse.social.studioasinc.shared.di

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.worker.WebWorkerDriver
import org.w3c.dom.Worker
import org.koin.dsl.module

actual val storageDriverModule = module {
    single<SqlDriver> {
        val worker = Worker("worker.js")
        WebWorkerDriver(worker)
    }
}
