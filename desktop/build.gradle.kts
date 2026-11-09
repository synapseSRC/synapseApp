plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose") version "1.11.1"
    id("org.jetbrains.kotlin.plugin.compose")
}

kotlin {
    jvm()

    sourceSets {
        val jvmMain = getByName("jvmMain") {
            dependencies {
                implementation(project(":shared"))
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.7.1")
                implementation("io.insert-koin:koin-core:4.2.2")
                implementation("io.insert-koin:koin-compose:4.2.2")
                implementation("io.github.jan-tennert.supabase:postgrest-kt:3.8.0")
                implementation("io.github.jan-tennert.supabase:auth-kt:3.8.0")
                implementation("io.github.jan-tennert.supabase:realtime-kt:3.8.0")
                implementation("io.github.jan-tennert.supabase:storage-kt:3.8.0")
                implementation("io.github.jan-tennert.supabase:functions-kt:3.8.0")
                implementation("io.github.aakira:napier:2.7.1")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.11.0")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime-jvm:0.7.1")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
                implementation(compose.desktop.currentOs)
                // Coordinates spelled out instead of the compose.material3 / compose.ui /
                // compose.materialIconsExtended accessors, which the Compose plugin now deprecates.
                // The versions are the ones those accessors resolved to here (material3 is held at
                // 1.9.0 and the icons library was last released as 1.7.3, both below the 1.11.1
                // plugin), so this changes no artifact - it only stops the build script from asking
                // for one through a retired property.
                implementation("org.jetbrains.compose.material3:material3:1.9.0")
                implementation("org.jetbrains.compose.material:material-icons-extended:1.7.3")
                implementation("org.jetbrains.compose.ui:ui:1.11.1")
            }
        }
    }
}

configurations.all {
    resolutionStrategy {
        eachDependency {
            if (requested.group == "org.jetbrains.kotlinx" && requested.name.startsWith("kotlinx-datetime")) {
                useVersion("0.7.1")
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "MainKt"
        nativeDistributions {
            targetFormats(org.jetbrains.compose.desktop.application.dsl.TargetFormat.Dmg, org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi, org.jetbrains.compose.desktop.application.dsl.TargetFormat.Deb, org.jetbrains.compose.desktop.application.dsl.TargetFormat.Exe)
            packageName = "Synapse Desktop"
            packageVersion = "1.0.0"
            jvmArgs("--enable-native-access=ALL-UNNAMED")
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
