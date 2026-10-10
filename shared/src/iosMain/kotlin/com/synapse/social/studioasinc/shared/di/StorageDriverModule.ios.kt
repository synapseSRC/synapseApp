
package com.synapse.social.studioasinc.shared.di

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.synapse.social.studioasinc.shared.data.database.StorageDatabase
import org.koin.dsl.module

actual val storageDriverModule = module {
    single<SqlDriver> {
        NativeSqliteDriver(StorageDatabase.Schema, "storage.db")
    }
}
