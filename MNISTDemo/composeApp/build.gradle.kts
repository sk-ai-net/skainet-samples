import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    jvmToolchain(21)

    // Android app entry point (both MainActivity variants, manifest, launcher icons/assets)
    // lives in :androidApp — AGP 9 no longer allows 'com.android.application' directly in a KMP
    // module. This module is now a plain KMP library on the Android axis; :androidApp depends on
    // it for App() and the shared UI.
    android {
        namespace = "sk.ainet.app.sample.mnist.library"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        }
    }

    jvm("desktop")

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser {
            val rootDirPath = project.rootDir.path
            val projectDirPath = project.projectDir.path
            commonWebpackConfig {
                outputFileName = "composeApp.js"
                devServer = (devServer ?: KotlinWebpackConfig.DevServer()).apply {
                    static = (static ?: mutableListOf()).apply {
                        // Serve sources to debug inside browser
                        add(rootDirPath)
                        add(projectDirPath)
                    }
                }
            }
        }
        binaries.executable()
    }

    sourceSets {
        val desktopMain by getting

        commonMain.dependencies {

            implementation(compose.runtime)
            implementation(compose.foundation)
            // Material Design 3
            implementation(compose.material3)
            // Keep Material Design 2 for backward compatibility during migration
            implementation(compose.material)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(projects.shared)

            implementation(libs.kotlinx.io.core)
            implementation("sk.ainet.ui:skainet-ui")

        }
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
            // JVM-optimized SKaiNET backend
            implementation(libs.skainet.backend.cpu.jvm)
        }
    }
}

// androidApp now lives in a separate module and needs to reach the generated Res
// accessor (used by MainActivity) — default visibility is internal to this module.
compose.resources {
    publicResClass = true
}

compose.desktop {
    application {
        mainClass = "sk.ai.net.samples.kmp.mnist.demo.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "sk.ai.net.samples.kmp.mnist.demo"
            packageVersion = "1.0.0"
        }
    }
}
