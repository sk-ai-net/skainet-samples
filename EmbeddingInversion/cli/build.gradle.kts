plugins {
    kotlin("jvm") version "2.4.0"
    application
}

repositories {
    google()
    mavenCentral()
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    // Substituted with the local ../../SKaiNET-transformers projects via the composite build.
    implementation("sk.ainet.transformers:skainet-transformers-inference-t5:0.35.0")
    implementation("sk.ainet.transformers:skainet-transformers-inference-vec2text:0.35.0")
    // Substituted with the local ../../SKaiNET projects.
    implementation("sk.ainet.core:skainet-lang-core:0.35.0")
    implementation("sk.ainet.core:skainet-backend-cpu:0.35.0")
    implementation("sk.ainet.core:skainet-io-core:0.35.0")
    implementation("sk.ainet.core:skainet-io-safetensors:0.35.0")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
}

application {
    mainClass.set("sk.ainet.samples.vec2text.MainKt")
}
