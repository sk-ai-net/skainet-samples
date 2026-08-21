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

    // Android app entry point (MainActivity, manifest) lives in :androidApp — AGP 9 no longer
    // allows 'com.android.application' directly in a KMP module. This module is now a plain KMP
    // library on the Android axis; :androidApp depends on it for App().
    android {
        namespace = "sk.ainet.samples.glove.app.library"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }

    listOf(iosArm64(), iosSimulatorArm64()).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "GloVeApp"
            isStatic = true
        }
    }

    jvm("desktop")

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser {
            commonWebpackConfig {
                outputFileName = "composeApp.js"
                devServer = (devServer ?: KotlinWebpackConfig.DevServer()).apply {
                    static = (static ?: mutableListOf()).apply {
                        add(project.rootDir.path)
                        add(project.projectDir.path)
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
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            // Shared SKaiNET design system: SKaiNETTheme, ThemeController, LoadingIndicator.
            implementation("sk.ainet.ui:skainet-ui")
            // The GloVe library: Vocabulary, Embeddings, GloVeTextReader, analogy().
            implementation(projects.glove)
        }
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            // Provides Dispatchers.Main for the JVM desktop target. Without it,
            // viewModelScope.launch (which dispatches on Main) throws
            // "Module with the Main dispatcher is missing" at runtime.
            implementation(libs.kotlinx.coroutines.swing)
        }
    }
}

// Deterministic package for the generated Res class so commonMain can import it.
compose.resources {
    publicResClass = true
    packageOfResClass = "sk.ainet.samples.glove.app.resources"
}

compose.desktop {
    application {
        mainClass = "sk.ainet.samples.glove.app.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "sk.ainet.samples.glove.app"
            packageVersion = "1.0.0"
        }
    }
}
