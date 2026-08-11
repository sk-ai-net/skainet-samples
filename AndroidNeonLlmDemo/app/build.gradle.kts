import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "sk.ainet.demo"
    compileSdk = 36

    defaultConfig {
        applicationId = "sk.ainet.demo"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        // ARM only — the JNI NEON kernels are the whole point of the demo.
        ndk { abiFilters += "arm64-v8a" }

        // Optional Hugging Face token for gated repos. Lives in local.properties
        // (gitignored) as HF_TOKEN=hf_xxx — never in source, never committed.
        val hfToken = Properties().apply {
            rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use(::load)
        }.getProperty("HF_TOKEN") ?: ""
        buildConfigField("String", "HF_TOKEN", "\"$hfToken\"")
        // Shown next to the logo — always matches the dependency in the catalog.
        buildConfigField("String", "SKAINET_VERSION", "\"${libs.versions.skainet.asProvider().get()}\"")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
}

dependencies {
    // Shared SKaiNET design system from ../skainet-ui (composite build)
    implementation("sk.ainet.ui:skainet-ui")

    implementation(platform(libs.skainet.bom))
    implementation(libs.skainet.lang.core)
    implementation(libs.skainet.backend.cpu)
    // Hand-written ARM NEON kernels, dispatched at runtime (armv8-a / armv8.2-a+fp16+dotprod)
    implementation(libs.skainet.backend.jni.cpu)
    // hf:// model download from the Hugging Face Hub
    implementation(libs.skainet.data.source)
    implementation(libs.skainet.io.core)
    implementation(libs.skainet.io.gguf)

    implementation(platform(libs.skainet.transformers.bom))
    implementation(libs.skainet.transformers.core)
    implementation(libs.skainet.transformers.runtime.kllama)
    implementation(libs.skainet.transformers.inference.llama)
    implementation(libs.skainet.transformers.agent)
    implementation(libs.kotlinx.io.core)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.coroutines.android)
}
