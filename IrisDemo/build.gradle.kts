plugins {
    kotlin("jvm") version "2.4.10"
    id("application")
}

group = "org.example"
version = "1.0-SNAPSHOT"

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(libs.skainet.lang.core)
    implementation(libs.skainet.lang.models)
    implementation(libs.skainet.lang.dag)
    implementation(libs.skainet.backend.cpu)
    implementation(libs.skainet.compile.dag)

    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlinx.coroutines)
    implementation(libs.kotlinx.io.core)

    testImplementation(libs.junit)
}

application {
    mainClass.set("org.example.MainKt")
}

tasks.withType<JavaExec>().configureEach {
    jvmArgs("--enable-preview", "--add-modules", "jdk.incubator.vector")
    systemProperty("skainet.cpu.vector.enabled", "true")
}

tasks.withType<Test>().configureEach {
    jvmArgs("--enable-preview", "--add-modules", "jdk.incubator.vector")
    systemProperty("skainet.cpu.vector.enabled", "true")
    useJUnitPlatform()
}
