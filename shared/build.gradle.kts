import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.library")
    kotlin("multiplatform")
    kotlin("plugin.serialization") version "2.3.20"
    id("com.google.devtools.ksp")
    id("app.cash.sqldelight")
}

version = project.findProperty("projectVersion") as String

configurations.all {
    resolutionStrategy {
        eachDependency {
            if (requested.group == "org.jetbrains.kotlinx" && requested.name.startsWith("kotlinx-datetime")) {
                useVersion("0.6.1")
            }
        }
    }
}

kotlin {
    applyDefaultHierarchyTemplate()

    jvm()
    wasmJs {
        browser {
            commonWebpackConfig {
                devServer = (devServer ?: org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig.DevServer()).apply {
                    static = (static ?: mutableListOf()).apply {
                        // Serve sources to debug inside browser
                        add(project.projectDir.path + "/src/wasmJsMain/resources")
                    }
                }
            }
        }
        binaries.executable()
    }

    androidTarget {
        publishLibraryVariants("release", "debug")
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
            freeCompilerArgs.add("-Xexpect-actual-classes")
        }
    }

    // Define iOS targets only on Mac OS to avoid build failures on other platforms
    if (true) {
        listOf(
            iosX64(),
            iosArm64(),
            iosSimulatorArm64()
        ).forEach {
            it.binaries.framework {
                baseName = "shared"
                isStatic = true
            }
            it.compilations.getByName("main").cinterops {
                val corecrypto by creating {
                    defFile(project.file("src/nativeInterop/cinterop/corecrypto.def"))
                }
            }
        }
    }

    sourceSets {
        all {
            languageSettings.apply {
                apiVersion = "2.3"
                languageVersion = "2.3"
                optIn("kotlin.ExperimentalStdlibApi")
                optIn("kotlinx.cinterop.ExperimentalForeignApi")
                optIn("kotlinx.cinterop.BetaInteropApi")
                optIn("kotlin.experimental.ExperimentalNativeApi")
            }
        }

        val commonMain by getting {
            dependencies {
                // Supabase & Ktor
                implementation(project.dependencies.platform("io.github.jan-tennert.supabase:bom:3.4.1"))
                implementation("io.github.jan-tennert.supabase:postgrest-kt")
                implementation("io.github.jan-tennert.supabase:auth-kt")
                implementation("io.github.jan-tennert.supabase:realtime-kt")
                implementation("io.github.jan-tennert.supabase:storage-kt")
                implementation("io.github.jan-tennert.supabase:functions-kt")
                implementation("io.ktor:ktor-client-core:3.4.2")
                implementation("com.fleeksoft.ksoup:ksoup:0.2.0")
                implementation("io.ktor:ktor-client-content-negotiation:3.4.2")
                implementation("io.ktor:ktor-serialization-kotlinx-json:3.4.2")

                // Serialization
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

                // Coroutines
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")

                // DateTime
                api("org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")

                // Logging
                implementation("io.github.aakira:napier:2.7.1")

                // DI
                implementation("io.insert-koin:koin-core:4.1.1")
                implementation("javax.inject:javax.inject:1")

                // Settings
                implementation("com.russhwolf:multiplatform-settings-no-arg:1.3.0")

                // SQLDelight
                implementation("app.cash.sqldelight:runtime:2.3.2")
                implementation("app.cash.sqldelight:coroutines-extensions:2.3.2")
            }
        }


        val jvmMain by getting {
            dependencies {
                // CIO engine is the recommended engine for Compose Desktop — pure Kotlin,
                // no native libs, no jlink stripping issues unlike ktor-client-okhttp
                implementation("io.ktor:ktor-client-cio:3.4.2")
                implementation("app.cash.sqldelight:sqlite-driver:2.3.2")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.10.2")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime-jvm:0.6.1")
            }
        }
        val wasmJsMain by getting {
            dependencies {
                implementation("app.cash.sqldelight:web-worker-driver:2.3.2")
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
            }
        }

        val androidMain by getting {
            dependencies {
                implementation("io.ktor:ktor-client-okhttp:3.4.2")
                implementation("org.whispersystems:signal-protocol-android:2.8.1")
                implementation("androidx.security:security-crypto:1.1.0-beta01")
                implementation("androidx.exifinterface:exifinterface:1.4.2")
                implementation("app.cash.sqldelight:android-driver:2.3.2")
                implementation("io.insert-koin:koin-android:4.1.1")
            }
        }

        val androidUnitTest by getting {
            dependencies {
                implementation("junit:junit:4.13.2")
                implementation("org.mockito:mockito-core:5.18.0")
            }
        }

        if (true) {
            val iosMain by getting {
                dependencies {
                    implementation("io.ktor:ktor-client-darwin:3.4.2")
                    implementation("app.cash.sqldelight:native-driver:2.3.2")
                }
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
        buildConfigField("String", "GEMINI_API_KEY", "\"${System.getenv("GEMINI_API_KEY") ?: project.findProperty("GEMINI_API_KEY") ?: ""}\"")

        buildConfigField("String", "OPENAI_API_ENDPOINT", "\"${System.getenv("OPENAI_API_ENDPOINT") ?: project.findProperty("OPENAI_API_ENDPOINT") ?: "https://api.openai.com/v1/chat/completions"}\"")
        buildConfigField("String", "ANTHROPIC_API_ENDPOINT", "\"${System.getenv("ANTHROPIC_API_ENDPOINT") ?: project.findProperty("ANTHROPIC_API_ENDPOINT") ?: "https://api.anthropic.com/v1/messages"}\"")
        buildConfigField("String", "OPENROUTER_API_ENDPOINT", "\"${System.getenv("OPENROUTER_API_ENDPOINT") ?: project.findProperty("OPENROUTER_API_ENDPOINT") ?: "https://openrouter.ai/api/v1/chat/completions"}\"")
        buildConfigField("String", "GEMINI_API_ENDPOINT", "\"${System.getenv("GEMINI_API_ENDPOINT") ?: project.findProperty("GEMINI_API_ENDPOINT") ?: "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent"}\"")
        buildConfigField("String", "IMGBB_API_ENDPOINT", "\"${System.getenv("IMGBB_API_ENDPOINT") ?: project.findProperty("IMGBB_API_ENDPOINT") ?: "https://api.imgbb.com/1/upload"}\"")
        buildConfigField("String", "CLOUDINARY_API_BASE_URL", "\"${System.getenv("CLOUDINARY_API_BASE_URL") ?: project.findProperty("CLOUDINARY_API_BASE_URL") ?: "https://api.cloudinary.com/v1_1/"}\"")
        buildConfigField("String", "APP_WEBSITE_URL", "\"${System.getenv("APP_WEBSITE_URL") ?: project.findProperty("APP_WEBSITE_URL") ?: "https://synapsesocial.vercel.app"}\"")
        buildConfigField("String", "GITHUB_BUG_REPORT_URL", "\"${System.getenv("GITHUB_BUG_REPORT_URL") ?: project.findProperty("GITHUB_BUG_REPORT_URL") ?: "https://github.com/synapseSRC/synapseApp/issues/new?template=bug_report.md"}\"")
        buildConfigField("String", "DEFAULT_QUIET_HOURS_START", "\"${System.getenv("DEFAULT_QUIET_HOURS_START") ?: project.findProperty("DEFAULT_QUIET_HOURS_START") ?: "22:00"}\"")
        buildConfigField("String", "DEFAULT_QUIET_HOURS_END", "\"${System.getenv("DEFAULT_QUIET_HOURS_END") ?: project.findProperty("DEFAULT_QUIET_HOURS_END") ?: "08:00"}\"")
        buildConfigField("String", "SUPABASE_REDIRECT_URL", "\"${System.getenv("SUPABASE_REDIRECT_URL") ?: project.findProperty("SUPABASE_REDIRECT_URL") ?: "https://synapseofficial.vercel.app/"}\"")
        buildConfigField("String", "X_BASE_URL", "\"${System.getenv("X_BASE_URL") ?: project.findProperty("X_BASE_URL") ?: "https://x.com/"}\"")
        buildConfigField("String", "INSTAGRAM_BASE_URL", "\"${System.getenv("INSTAGRAM_BASE_URL") ?: project.findProperty("INSTAGRAM_BASE_URL") ?: "https://instagram.com/"}\"")
        buildConfigField("String", "FACEBOOK_BASE_URL", "\"${System.getenv("FACEBOOK_BASE_URL") ?: project.findProperty("FACEBOOK_BASE_URL") ?: "https://facebook.com/"}\"")
        buildConfigField("String", "LINKEDIN_BASE_URL", "\"${System.getenv("LINKEDIN_BASE_URL") ?: project.findProperty("LINKEDIN_BASE_URL") ?: "https://linkedin.com/in/"}\"")
        buildConfigField("String", "GITHUB_BASE_URL", "\"${System.getenv("GITHUB_BASE_URL") ?: project.findProperty("GITHUB_BASE_URL") ?: "https://github.com/"}\"")
        buildConfigField("String", "YOUTUBE_BASE_URL", "\"${System.getenv("YOUTUBE_BASE_URL") ?: project.findProperty("YOUTUBE_BASE_URL") ?: "https://youtube.com/@"}\"")

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
        force("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
        force("org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")
        eachDependency {
            if (requested.group == "org.jetbrains.kotlin" && requested.name.startsWith("kotlin-stdlib")) {
                useVersion("2.3.20")
            }
        }
    }
}

// Disable SQLDelight migration verification due to Windows file locking issues
tasks.withType<app.cash.sqldelight.gradle.VerifyMigrationTask> {
    enabled = false
}
