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

    js {
        browser()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.io.core)
            implementation(libs.kotlinx.coroutines)

            // SKaiNET core
            implementation(libs.skainet.lang.core)
            implementation(libs.skainet.lang.models)
            implementation(libs.skainet.lang.dag)

            // SKaiNET compilation
            implementation(libs.skainet.compile.core)
            implementation(libs.skainet.compile.dag)

            // SKaiNET backend
            implementation(libs.skainet.backend.cpu)

            // SKaiNET data
            implementation(libs.skainet.data.api)
            implementation(libs.skainet.data.simple)

            // SKaiNET I/O
            implementation(libs.skainet.io.core)
            implementation(libs.skainet.io.gguf)
            implementation(libs.skainet.io.safetensors)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines)
            implementation(libs.kotlinx.coroutines.test)
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

