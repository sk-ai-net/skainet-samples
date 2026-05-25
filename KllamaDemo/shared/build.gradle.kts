import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
}

kotlin {
    jvmToolchain(21)

    androidTarget()

    iosArm64()
    iosSimulatorArm64()

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
            implementation(libs.kotlinx.coroutines)
            implementation(libs.kotlinx.io.core)
            implementation(libs.kotlinx.datetime)

            // SKaiNET core
            implementation(libs.skainet.lang.core)
            implementation(libs.skainet.lang.models)

            // SKaiNET compilation
            implementation(libs.skainet.compile.core)

            // SKaiNET backend
            implementation(libs.skainet.backend.cpu)

            // SKaiNET I/O
            implementation(libs.skainet.io.core)
            implementation(libs.skainet.io.gguf)

            // SKaiNET-transformers: Llama + Qwen inference (DecoderGgufWeightLoader, QwenNetworkLoader)
            implementation(libs.skainet.inference.llama)
            implementation(libs.skainet.inference.qwen)
            // SKaiNET LLM core - OptimizedLLMRuntime, Tokenizer, TokenizerFactory, GenerateExtensions
            implementation(libs.skainet.llm)
        }
        jvmMain.dependencies {
            // SKaiNET KLlama (GGUFTokenizer, CpuAttentionBackend) - JVM only
            implementation(libs.skainet.kllama)
            // SKaiNET Agent APIs (ChatSession, ToolRegistry, ChatTemplate) - JVM only
            implementation(libs.skainet.kllama.agents)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        wasmJsMain.dependencies {
            implementation(libs.kotlinx.browser)
        }
    }
}

tasks.withType<Test>().configureEach {
    jvmArgs("--enable-preview", "--add-modules", "jdk.incubator.vector")
}

android {
    namespace = "sk.ainet.apps.kllama.chat.shared"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
}
