import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    jvmToolchain(21)

    // Android app entry point (MainActivity, KernelRaceApp, manifest) lives in :androidApp —
    // AGP 9 no longer allows 'com.android.application' directly in a KMP module. This module
    // is now a plain KMP library on the Android axis; :androidApp depends on it for App().
    android {
        namespace = "sk.ainet.samples.kernelrace.library"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

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
