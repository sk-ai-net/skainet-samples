import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    dependencies {
        implementation(projects.composeApp)
        implementation(projects.shared)
        implementation(compose.runtime)
        implementation(compose.foundation)
        implementation(compose.material3)
        implementation(compose.ui)
        implementation(libs.compose.uiTooling)
        implementation(libs.androidx.activity.compose)
        implementation(libs.kotlinx.coroutines)
        // Hand-written ARM NEON kernels — Android JNI only.
        implementation(libs.skainet.backend.jni.cpu)
    }

    target {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }
}

android {
    namespace = "sk.ainet.samples.kernelrace"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "sk.ainet.samples.kernelrace"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
        // ARM only — the JNI NEON kernels are the whole point of the race.
        ndk { abiFilters += "arm64-v8a" }

        // Optional Hugging Face token for gated repos. Lives in local.properties
        // (gitignored) as HF_TOKEN=hf_xxx — never in source, never committed.
        // SmolLM2 itself is a public repo, so this is normally left blank.
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
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}
