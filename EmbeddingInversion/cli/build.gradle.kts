plugins {
    kotlin("jvm")
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
    // SKaiNET core: version aligned by the published BOM (0.36.0), resolved from Maven Central.
    implementation(platform("sk.ainet:skainet-bom:0.36.0"))
    implementation("sk.ainet.core:skainet-lang-core")
    implementation("sk.ainet.core:skainet-backend-cpu")
    implementation("sk.ainet.core:skainet-io-core")
    implementation("sk.ainet.core:skainet-io-safetensors")

    // t5 / vec2text — published on Maven Central.
    implementation("sk.ainet.transformers:skainet-transformers-inference-t5:0.36.0")
    implementation("sk.ainet.transformers:skainet-transformers-inference-vec2text:0.36.0")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
}

application {
    mainClass.set("sk.ainet.samples.vec2text.MainKt")
}
