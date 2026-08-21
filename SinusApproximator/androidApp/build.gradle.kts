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
        implementation(compose.preview)
        implementation(libs.androidx.activity.compose)
        debugImplementation(compose.uiTooling)
    }

    target {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }
}

android {
    namespace = "sk.ai.net.compiler.composable.demo"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "sk.ai.net.compiler.composable.demo"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
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
