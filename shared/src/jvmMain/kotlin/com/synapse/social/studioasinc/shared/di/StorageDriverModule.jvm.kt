package com.synapse.social.studioasinc.shared.di

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.synapse.social.studioasinc.shared.data.database.StorageDatabase
import org.koin.dsl.module
import java.io.File

actual val storageDriverModule = module {
    single<SqlDriver> {
        val dbFile = File(System.getProperty("user.home"), ".synapse/storage.db")
        if (!dbFile.parentFile.exists()) {
            dbFile.parentFile.mkdirs()
        }
        val driver = JdbcSqliteDriver("jdbc:sqlite:${dbFile.absolutePath}")

        try {
            StorageDatabase.Schema.create(driver)
        } catch (e: Exception) {
            // Already created
        }

        driver
    }
}
