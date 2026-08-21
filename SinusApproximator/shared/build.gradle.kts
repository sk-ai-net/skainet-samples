import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
}

kotlin {
    jvmToolchain(21)

    android {
        namespace = "sk.ai.net.client.shared"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
    }

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
