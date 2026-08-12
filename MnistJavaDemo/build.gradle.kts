plugins {
    java
    application
    kotlin("jvm") version "2.4.10"
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

application {
    mainClass = "sk.ainet.examples.mnist.MnistDetector"
}

dependencies {
    // SKaiNET
    implementation(libs.skainet.lang.core)
    implementation(libs.skainet.lang.models)
    implementation(libs.skainet.backend.cpu)
    implementation(libs.skainet.data.api)
    implementation(libs.skainet.data.simple)
    implementation(libs.skainet.data.transform)
    implementation(libs.skainet.io.core)
    implementation(libs.skainet.io.image)
    implementation(libs.skainet.io.gguf)
    implementation(libs.skainet.compile.dag)

    // Kotlin runtime (required for SKaiNET interop)
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlinx.coroutines)
    implementation(libs.kotlinx.io.core)

    // Testing
    testImplementation(libs.junit)
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.addAll(listOf("--enable-preview"))
}

tasks.withType<JavaExec>().configureEach {
    jvmArgs("--enable-preview", "--add-modules", "jdk.incubator.vector")
    systemProperty("skainet.cpu.vector.enabled", "true")
}

tasks.withType<Test>().configureEach {
    jvmArgs("--enable-preview", "--add-modules", "jdk.incubator.vector")
    systemProperty("skainet.cpu.vector.enabled", "true")
}
