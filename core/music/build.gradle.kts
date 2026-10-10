import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Pure Kotlin music theory: chords, Nashville numbers, transposition, pitch detection.
// No Android, no Compose — the Gradle boundary is what enforces this.
plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

kotlin {
    jvm {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
    }
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
