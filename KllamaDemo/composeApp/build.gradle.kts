import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
}

kotlin {
    jvmToolchain(21)

    applyDefaultHierarchyTemplate {
        common {
            group("web") {
                withJs()
                withWasmJs()
            }
        }
    }

    // Android app entry point (MainActivity, SetupAndroidFilePicker, manifest, launcher icons)
    // lives in :androidApp — AGP 9 no longer allows 'com.android.application' directly in a KMP
    // module. This module is now a plain KMP library on the Android axis; :androidApp depends on
    // it for App(). QwenRuntimeBuilder.android.kt's expect/actual stays here since it's platform
    // logic, not entry-point code.
    android {
        namespace = "sk.ainet.apps.kllama.chat.library"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }
    
    jvm()
    
    js {
        browser()
        binaries.executable()
    }
    
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        binaries.executable()
    }
    
    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.kotlinx.io.core)
            implementation(libs.kotlinx.datetime)
            implementation(projects.shared)
            implementation("sk.ainet.ui:skainet-ui")
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
        }
        named("webMain").configure {
            dependencies {
                implementation(libs.kotlinx.browser)
            }
        }
    }
}

// Single source-of-truth for the JVM flags SKaiNET needs:
// - jdk.incubator.vector + --enable-preview engage the JDK Vector API
//   for SIMD-accelerated CPU ops
// - --enable-native-access=ALL-UNNAMED lets MemorySegment Arena (Panama
//   FFM API) open without "restricted access" warnings on JDK 21+;
//   DecoderGgufMemSegConverter uses Arena.ofShared() to back Q4_0/Q8_0
//   tensors with off-heap memory for SIMD-packed matmul kernels.
// - skainet.cpu.vector.enabled=true is the explicit opt-in switch the
//   library inspects to confirm the host wants the SIMD code path.
val skainetSimdJvmArgs = listOf(
    "--add-modules", "jdk.incubator.vector",
    "--enable-preview",
    "--enable-native-access=ALL-UNNAMED",
    "-Dskainet.cpu.vector.enabled=true",
)

tasks.withType<JavaExec>().configureEach {
    jvmArgs(skainetSimdJvmArgs)
    maxHeapSize = "16g"
}

tasks.withType<Test>().configureEach {
    jvmArgs(skainetSimdJvmArgs)
}

val fetchQwenModel by tasks.registering(Exec::class) {
    description = "Downloads the Qwen3-0.6B-Q4_K_M GGUF into composeResources/files. Skips if present."
    group = "build setup"
    val scriptPath = rootProject.layout.projectDirectory.file("scripts/fetch-qwen-model.sh")
    commandLine("bash", scriptPath.asFile.absolutePath)
    inputs.file(scriptPath)
    val modelFile = rootProject.layout.projectDirectory.file(
        "composeApp/src/commonMain/composeResources/files/qwen3-0.6b-Q4_0.gguf"
    )
    outputs.file(modelFile)
    onlyIf { !modelFile.asFile.exists() || modelFile.asFile.length() < 300L * 1024 * 1024 }
}

// Make every Kotlin compile task AND every Compose-resources copy task
// depend on the model being on disk. Without this, Gradle 9's strict
// implicit-dependency validator fails because the resource-copy task
// reads from a directory that fetchQwenModel writes into.
tasks.matching {
    it.name.startsWith("compileKotlin") ||
        it.name.startsWith("compileJava") ||
        it.name.startsWith("convertXmlValueResourcesFor") ||
        it.name.startsWith("copyNonXmlValueResourcesFor") ||
        it.name.startsWith("prepareComposeResourcesTaskFor") ||
        it.name.startsWith("generateResourceAccessorsFor")
}.configureEach { dependsOn(fetchQwenModel) }

compose.desktop {
    application {
        mainClass = "sk.ainet.apps.kllama.chat.MainKt"

        jvmArgs += listOf(
            "-Xmx32G",                              // Increased heap for large models
            "-XX:+UseG1GC",                         // Better GC for large heaps
            "-XX:MaxGCPauseMillis=100",             // Reduce GC pauses
        ) + skainetSimdJvmArgs

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "sk.ainet.apps.kllama.chat"
            packageVersion = "1.0.0"
        }
    }
}
