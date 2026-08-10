plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose") version "1.6.11"
    id("org.jetbrains.kotlin.plugin.compose")
}

kotlin {
    jvm()

    sourceSets {
        val jvmMain by getting {
            dependencies {
                implementation(project(":shared"))
                implementation("io.insert-koin:koin-core:4.1.1")
                implementation("io.insert-koin:koin-compose:4.1.1")
                implementation("io.github.jan-tennert.supabase:postgrest-kt:3.4.1")
                implementation("io.github.jan-tennert.supabase:auth-kt:3.4.1")
                implementation("io.github.jan-tennert.supabase:realtime-kt:3.4.1")
                implementation("io.github.jan-tennert.supabase:storage-kt:3.4.1")
                implementation("io.github.jan-tennert.supabase:functions-kt:3.4.1")
                implementation("io.github.aakira:napier:2.7.1")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.10.2")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime-jvm:0.6.1")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
                implementation(compose.desktop.currentOs)
                implementation("org.jetbrains.skiko:skiko-awt-runtime-windows-x64:0.9.4.2")
                implementation(compose.material3)
                implementation(compose.ui)
            }
        }
    }
}

configurations.all {
    resolutionStrategy {
        eachDependency {
            if (requested.group == "org.jetbrains.skiko") {
                useVersion("0.9.4.2")
            }
            if (requested.group == "org.jetbrains.kotlinx" && requested.name.startsWith("kotlinx-datetime")) {
                useVersion("0.6.1")
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "MainKt"
        nativeDistributions {
            targetFormats(org.jetbrains.compose.desktop.application.dsl.TargetFormat.Dmg, org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi, org.jetbrains.compose.desktop.application.dsl.TargetFormat.Deb)
            packageName = "Synapse Desktop"
            packageVersion = "1.0.0"
            modules(
                "java.sql",          // Required by SQLDelight JdbcSqliteDriver
                "java.naming",       // Required by some Ktor/OkHttp internals
                "java.net.http",     // HTTP client fallback
                "jdk.crypto.ec",     // TLS elliptic-curve support for HTTPS
                "java.security.jgss" // Kerberos/auth support
            )
        }
    }
}
