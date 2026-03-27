
package com.synapse.social.studioasinc.shared.di

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.synapse.social.studioasinc.shared.data.database.StorageDatabase
import org.koin.dsl.module
import org.koin.android.ext.koin.androidContext

actual val storageDriverModule = module {
    single<SqlDriver> {
        AndroidSqliteDriver(StorageDatabase.Schema, androidContext(), "storage.db")
    }
}
