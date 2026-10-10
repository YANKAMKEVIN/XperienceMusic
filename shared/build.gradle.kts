import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinxSerialization)
}

// Supabase project keys come from local.properties (git-ignored) or the environment (CI).
// Left empty, the app runs in offline demo mode.
val localProperties = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use(::load)
}

fun supabaseSetting(property: String, env: String): String =
    localProperties.getProperty(property) ?: System.getenv(env) ?: ""

val generateSupabaseConfig by tasks.registering {
    val url = supabaseSetting("supabase.url", "SUPABASE_URL")
    val key = supabaseSetting("supabase.publishableKey", "SUPABASE_PUBLISHABLE_KEY")
    val outputDir = layout.buildDirectory.dir("generated/supabaseConfig/kotlin")
    inputs.property("url", url)
    inputs.property("key", key)
    outputs.dir(outputDir)
    doLast {
        val file = outputDir.get().file("com/xperiencemusic/shared/data/remote/SupabaseConfig.kt").asFile
        file.parentFile.mkdirs()
        file.writeText(
            """
            |package com.xperiencemusic.shared.data.remote
            |
            |// Generated from local.properties by :shared:generateSupabaseConfig. Do not edit.
            |internal object SupabaseConfig {
            |    const val URL: String = "$url"
            |    const val PUBLISHABLE_KEY: String = "$key"
            |    val isConfigured: Boolean get() = URL.isNotBlank() && PUBLISHABLE_KEY.isNotBlank()
            |}
            |""".trimMargin(),
        )
    }
}

kotlin {
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    androidLibrary {
        namespace = "com.xperiencemusic.shared"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
        androidResources {
            enable = true
        }
        withHostTest {}
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.core.music)

            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.compose.material.icons.core)
            implementation(libs.navigation.compose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.serialization.json)
            implementation(project.dependencies.platform(libs.supabase.bom))
            implementation(libs.supabase.auth)
            implementation(libs.supabase.postgrest)
            implementation(libs.supabase.storage)
        }
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
            implementation(libs.androidx.activity.compose)
            implementation(libs.media3.exoplayer)
            implementation(libs.media3.session)
            implementation(libs.koin.android)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        commonMain {
            kotlin.srcDir(generateSupabaseConfig)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.turbine)
        }
    }
}

compose.resources {
    publicResClass = false
    packageOfResClass = "com.xperiencemusic.shared.resources"
}
