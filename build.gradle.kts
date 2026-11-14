plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidMultiplatformLibrary) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinxSerialization) apply false
    alias(libs.plugins.detekt) apply false
}

// Static analysis on the shared code. The sources are listed by hand because detekt does not know
// the Kotlin Multiplatform source sets (commonMain, androidMain, iosMain, ...).
listOf(":shared", ":core:music").forEach { path ->
    project(path) {
        apply(plugin = "dev.detekt")
        extensions.configure<dev.detekt.gradle.extensions.DetektExtension> {
            buildUponDefaultConfig.set(true)
            config.setFrom(rootProject.file("config/detekt/detekt.yml"))
            baseline.set(rootProject.file("config/detekt/baseline-${project.name}.xml"))
            source.setFrom(
                fileTree("src") {
                    include("**/*.kt")
                    exclude("**/build/**")
                },
            )
        }
        dependencies.add("detektPlugins", rootProject.libs.compose.rules.detekt)
    }
}
