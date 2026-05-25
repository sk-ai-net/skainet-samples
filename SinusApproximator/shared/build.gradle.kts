import org.gradle.kotlin.dsl.implementation
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
}

kotlin {
    jvmToolchain(21)

    androidTarget()


    jvm()

    iosArm64()
    iosSimulatorArm64()


    js {
        browser()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    sourceSets {
        // Temporarily disable wasmJsMain source set
        val wasmJsMain by getting

        commonMain.dependencies {
            implementation(libs.kotlinx.io.core)
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")

            implementation(libs.skainet.lang.core)
            implementation(libs.skainet.lang.models)
            implementation(libs.skainet.compile.core)
            implementation(libs.skainet.compile.dag)
            implementation(libs.skainet.backend.cpu)
            implementation(libs.skainet.data.api)
            implementation(libs.skainet.data.simple)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
        }

         wasmJsMain.dependencies {
             implementation(libs.kotlinx.io.core)
         }
    }
}

android {
    namespace = "sk.ai.net.client.shared"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
}
