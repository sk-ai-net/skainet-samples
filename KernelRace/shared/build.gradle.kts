import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
}

kotlin {
    jvmToolchain(21)

    android {
        namespace = "sk.ainet.samples.kernelrace.shared"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }

        // Runs commonTest on the JVM against Android stubs (testDebugUnitTest) — CI relies on
        // this; the new AGP-KMP-library plugin doesn't wire it up by default like com.android.library did.
        withHostTest {}
    }

    jvm()

    iosArm64()
    iosSimulatorArm64()

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines)
            implementation(libs.kotlinx.io.core)
            implementation(libs.androidx.lifecycle.viewmodel)

            // SKaiNET core. BOM applied via api() so its version constraints also reach
            // composeApp's classpath — implementation-scoped constraints stay internal to
            // this module and leave downstream consumers with unversioned coordinates.
            api(project.dependencies.platform(libs.skainet.bom))
            // api, not implementation: composeApp constructs DirectCpuExecutionContext (and
            // reads ExecutionContext/FP32/etc.) directly, so these need to be on its classpath.
            api(libs.skainet.lang.core)
            api(libs.skainet.backend.cpu)
            api(libs.skainet.io.core)
            api(libs.skainet.io.gguf)

            // SKaiNET-transformers: Llama inference + generation. agent and
            // runtime-kllama both publish wasmJs targets as of 0.39.1, so the
            // whole engine (unlike KllamaDemo's 0.34.1-era jvmMain-only split)
            // lives in commonMain.
            api(project.dependencies.platform(libs.skainet.transformers.bom))
            api(libs.skainet.transformers.inference.llama)
            api(libs.skainet.transformers.core)
            api(libs.skainet.transformers.runtime.kllama)
            api(libs.skainet.transformers.agent)
        }
        androidMain.dependencies {
            // hf:// / https:// model download from the Hugging Face Hub
            implementation(libs.skainet.data.source)
            // Hand-written ARM NEON kernels, dispatched at runtime (armv8-a / armv8.2-a+fp16+dotprod)
            implementation(libs.skainet.backend.jni.cpu)
        }
        jvmMain.dependencies {
            implementation(libs.skainet.data.source)
            // Transitive-only: lets Platform.jvm.kt resolve PanamaVectorKernelProvider's
            // isAvailable() (declared on the KernelProvider interface from this module).
            implementation(libs.skainet.backend.api)
        }
        iosMain.dependencies {
            // skainet-data-source is JVM-only (skainet.targets=jvm in its gradle.properties) —
            // KtorRemoteDataSourceFetcher isn't reachable from iosMain, so IosModelProvider
            // talks to Ktor's Darwin engine directly instead.
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.darwin)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

tasks.withType<Test>().configureEach {
    // SIMD-accelerated CPU ops via JDK Vector API (incubator). Same flags the
    // composeApp desktop run uses, so jvmTest exercises the same code path.
    jvmArgs(
        "--add-modules", "jdk.incubator.vector",
        "--enable-preview",
        "-Dskainet.cpu.vector.enabled=true",
    )
}
