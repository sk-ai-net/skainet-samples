import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

repositories {
    google()
    mavenCentral()
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    // SKaiNET core from Maven Central, version aligned by the published BOM (0.36.0).
    implementation(platform("sk.ainet:skainet-bom:0.36.0"))
    implementation("sk.ainet.core:skainet-lang-core")
    implementation("sk.ainet.core:skainet-backend-cpu")
    implementation("sk.ainet.core:skainet-io-core")
    implementation("sk.ainet.core:skainet-io-safetensors")

    // t5 / vec2text — published on Maven Central.
    implementation("sk.ainet.transformers:skainet-transformers-inference-t5:0.36.0")
    implementation("sk.ainet.transformers:skainet-transformers-inference-vec2text:0.36.0")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")

    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
}

compose.desktop {
    application {
        mainClass = "sk.ainet.samples.vec2text.ui.MainKt"
        jvmArgs += listOf("-Xmx4g")
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "EmbeddingInversion"
            packageVersion = "1.0.0"
        }
    }
}
