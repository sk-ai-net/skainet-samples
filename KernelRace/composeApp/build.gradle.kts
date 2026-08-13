import java.util.Properties
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    jvmToolchain(21)

    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    jvm()

    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        binaries.executable()
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.compose.uiTooling)
            implementation(libs.androidx.activity.compose)
            // Hand-written ARM NEON kernels — Android JNI only.
            implementation(libs.skainet.backend.jni.cpu)
        }
        commonMain.dependencies {
            implementation(projects.shared)
            implementation("sk.ainet.ui:skainet-ui")
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.kotlinx.coroutines)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
        }
        wasmJsMain.dependencies {
            implementation(libs.kotlinx.coroutines)
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

// Single source-of-truth for the JVM flags SKaiNET needs: the JDK Vector API (incubator) for
// SIMD-accelerated CPU ops on desktop.
val skainetSimdJvmArgs = listOf(
    "--add-modules", "jdk.incubator.vector",
    "--enable-preview",
    "-Dskainet.cpu.vector.enabled=true",
)

tasks.withType<JavaExec>().configureEach {
    jvmArgs(skainetSimdJvmArgs)
}

tasks.withType<Test>().configureEach {
    jvmArgs(skainetSimdJvmArgs)
}

val fetchModel by tasks.registering(Exec::class) {
    description = "Downloads the SmolLM2-135M-Instruct Q8_0 GGUF into composeResources/files. Skips if present."
    group = "build setup"
    val scriptPath = rootProject.layout.projectDirectory.file("scripts/fetch-model.sh")
    commandLine("bash", scriptPath.asFile.absolutePath)
    inputs.file(scriptPath)
    val modelFile = rootProject.layout.projectDirectory.file(
        "composeApp/src/commonMain/composeResources/files/SmolLM2-135M-Instruct-Q8_0.gguf"
    )
    outputs.file(modelFile)
    onlyIf { !modelFile.asFile.exists() || modelFile.asFile.length() < 100L * 1024 * 1024 }
}

// Every Kotlin compile task AND every Compose-resources copy task depends on the model being on
// disk — Gradle 9's strict implicit-dependency validator fails otherwise, since the
// resource-copy task reads from a directory fetchModel writes into.
tasks.matching {
    it.name.startsWith("compileKotlin") ||
        it.name.startsWith("compileJava") ||
        it.name.startsWith("convertXmlValueResourcesFor") ||
        it.name.startsWith("copyNonXmlValueResourcesFor") ||
        it.name.startsWith("prepareComposeResourcesTaskFor") ||
        it.name.startsWith("generateResourceAccessorsFor")
}.configureEach { dependsOn(fetchModel) }

compose.desktop {
    application {
        mainClass = "sk.ainet.samples.kernelrace.MainKt"

        jvmArgs += skainetSimdJvmArgs

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "sk.ainet.samples.kernelrace"
            packageVersion = "1.0.0"
        }
    }
}
