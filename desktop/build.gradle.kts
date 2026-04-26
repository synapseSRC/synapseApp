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
                implementation(compose.desktop.currentOs)
                implementation(compose.material3)
                implementation(compose.ui)
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
        }
    }
}
