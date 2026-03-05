import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.library")
    kotlin("multiplatform")
    kotlin("plugin.serialization") version "2.1.0"
    id("com.google.devtools.ksp")
    id("app.cash.sqldelight")
}

version = project.findProperty("projectVersion") as String

kotlin {
    applyDefaultHierarchyTemplate()

    androidTarget {
        publishLibraryVariants("release", "debug")
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach {
        it.binaries.framework {
            baseName = "shared"
            isStatic = true
        }
    }

    sourceSets {
        all {
            languageSettings.apply {
                apiVersion = "2.1"
                languageVersion = "2.1"
                optIn("kotlin.ExperimentalStdlibApi")
            }
        }

        val commonMain by getting {
            dependencies {
                // Supabase & Ktor
                implementation(project.dependencies.platform("io.github.jan-tennert.supabase:bom:3.1.1"))
                implementation("io.github.jan-tennert.supabase:postgrest-kt")
                implementation("io.github.jan-tennert.supabase:auth-kt")
                implementation("io.github.jan-tennert.supabase:realtime-kt")
                implementation("io.github.jan-tennert.supabase:storage-kt")
                implementation("io.github.jan-tennert.supabase:functions-kt")
                implementation("io.ktor:ktor-client-core:3.0.3")
                implementation("io.ktor:ktor-client-content-negotiation:3.0.3")
                implementation("io.ktor:ktor-serialization-kotlinx-json:3.0.3")

                // Serialization
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

                // Coroutines
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")

                // DateTime
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")

                // Logging
                implementation("io.github.aakira:napier:2.7.1")

                // DI
                implementation("io.insert-koin:koin-core:4.0.0")

                // Settings
                implementation("com.russhwolf:multiplatform-settings-no-arg:1.2.0")

                // SQLDelight
                implementation("app.cash.sqldelight:runtime:2.0.2")
                implementation("app.cash.sqldelight:coroutines-extensions:2.0.2")
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
            }
        }

        val androidMain by getting {
            dependencies {
                implementation("io.ktor:ktor-client-okhttp:3.0.3")
                implementation("org.whispersystems:signal-protocol-android:2.8.1")
                implementation("androidx.security:security-crypto:1.1.0-alpha06")
                implementation("androidx.exifinterface:exifinterface:1.3.7")
                implementation("app.cash.sqldelight:android-driver:2.0.2")
                implementation("io.insert-koin:koin-android:4.0.0")
            }
        }

        val androidUnitTest by getting {
            dependencies {
                implementation("junit:junit:4.13.2")
                implementation("org.mockito:mockito-core:5.11.0")
            }
        }

        val iosMain by getting {
            dependencies {
                implementation("io.ktor:ktor-client-darwin:3.0.3")
                implementation("app.cash.sqldelight:native-driver:2.0.2")
            }
        }
    }
}

sqldelight {
  databases {
    create("StorageDatabase") {
      packageName.set("com.synapse.social.studioasinc.shared.data.database")
      verifyMigrations.set(false)
    }
  }
}

dependencies {
}

android {
    namespace = "com.synapse.social.studioasinc.shared"
    compileSdk = 36
    buildToolsVersion = "36.0.0"
    defaultConfig {
        minSdk = 26

        buildConfigField("String", "SUPABASE_URL", "\"${System.getenv("SUPABASE_URL") ?: project.findProperty("SUPABASE_URL") ?: ""}\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"${System.getenv("SUPABASE_ANON_KEY") ?: project.findProperty("SUPABASE_ANON_KEY") ?: ""}\"")
        buildConfigField("String", "SUPABASE_SYNAPSE_S3_ENDPOINT_URL", "\"${System.getenv("SUPABASE_SYNAPSE_S3_ENDPOINT_URL") ?: project.findProperty("SUPABASE_SYNAPSE_S3_ENDPOINT_URL") ?: ""}\"")
        buildConfigField("String", "SUPABASE_SYNAPSE_S3_ENDPOINT_REGION", "\"${System.getenv("SUPABASE_SYNAPSE_S3_ENDPOINT_REGION") ?: project.findProperty("SUPABASE_SYNAPSE_S3_ENDPOINT_REGION") ?: ""}\"")
        buildConfigField("String", "SUPABASE_SYNAPSE_S3_ACCESS_KEY_ID", "\"${System.getenv("SUPABASE_SYNAPSE_S3_ACCESS_KEY_ID") ?: project.findProperty("SUPABASE_SYNAPSE_S3_ACCESS_KEY_ID") ?: ""}\"")
        buildConfigField("String", "IMGBB_API_KEY", "\"${System.getenv("IMGBB_API_KEY") ?: project.findProperty("IMGBB_API_KEY") ?: ""}\"")
        buildConfigField("String", "CLOUDINARY_CLOUD_NAME", "\"${System.getenv("CLOUDINARY_CLOUD_NAME") ?: project.findProperty("CLOUDINARY_CLOUD_NAME") ?: ""}\"")
        buildConfigField("String", "CLOUDINARY_API_KEY", "\"${System.getenv("CLOUDINARY_API_KEY") ?: project.findProperty("CLOUDINARY_API_KEY") ?: ""}\"")
        buildConfigField("String", "CLOUDINARY_API_SECRET", "\"${System.getenv("CLOUDINARY_API_SECRET") ?: project.findProperty("CLOUDINARY_API_SECRET") ?: ""}\"")
        buildConfigField("String", "SUPABASE_SYNAPSE_S3_ACCESS_KEY", "\"${System.getenv("SUPABASE_SYNAPSE_S3_ACCESS_KEY") ?: project.findProperty("SUPABASE_SYNAPSE_S3_ACCESS_KEY") ?: ""}\"")
        
        consumerProguardFiles("proguard-rules.pro")
    }
    buildFeatures {
        buildConfig = true
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

configurations.all {
    resolutionStrategy {
        force("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
        force("org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")
        eachDependency {
            if (requested.group == "org.jetbrains.kotlin" && requested.name.startsWith("kotlin-stdlib")) {
                useVersion("2.1.0")
            }
        }
    }
}

// Disable SQLDelight migration verification due to Windows file locking issues
tasks.withType<app.cash.sqldelight.gradle.VerifyMigrationTask> {
    enabled = false
}
