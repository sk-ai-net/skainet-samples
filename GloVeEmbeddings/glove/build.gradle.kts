import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

kotlin {
    jvmToolchain(21)

    jvm()
    iosArm64()
    iosSimulatorArm64()
    js { nodejs() }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs { nodejs() }

    sourceSets {
        commonMain.dependencies {
            implementation(project.dependencies.platform(libs.skainet.bom))
            implementation(libs.skainet.lang.core)
            implementation(libs.skainet.backend.cpu)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

// JVM CPU backend can use the incubator Vector API when available.
tasks.withType<Test>().configureEach {
    jvmArgs("--add-modules", "jdk.incubator.vector")
    systemProperty("skainet.cpu.vector.enabled", "true")
}

// Run the educational demo: ./gradlew :glove:runDemo
tasks.register<JavaExec>("runDemo") {
    group = "application"
    description = "Runs the GloVe educational demo (vocab, training, nearest words, tensor scores)."
    val mainCompilation = kotlin.jvm().compilations.getByName("main")
    classpath = files(mainCompilation.output.allOutputs, mainCompilation.runtimeDependencyFiles)
    mainClass.set("sk.ainet.samples.glove.MainKt")
    dependsOn(mainCompilation.compileTaskProvider)
    jvmArgs("--add-modules", "jdk.incubator.vector")
    systemProperty("skainet.cpu.vector.enabled", "true")
}
