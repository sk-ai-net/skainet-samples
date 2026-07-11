pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "embedding-inversion"

// SKaiNET core is consumed from Maven Central (0.36.0, pinned by the skainet BOM in
// cli/build.gradle.kts) — no local ../../SKaiNET checkout needed.
//
// The t5 / vec2text modules are not yet published, so SKaiNET-transformers stays a composite
// build. Its modules publish as `skainet-transformers-inference-*`, which differs
// from their Gradle project names (`t5`, `vec2text`), so auto-substitution can't match —
// map the coordinates to the local projects explicitly.
includeBuild("../../SKaiNET-transformers") {
    dependencySubstitution {
        substitute(module("sk.ainet.transformers:skainet-transformers-inference-t5"))
            .using(project(":llm-inference:t5"))
        substitute(module("sk.ainet.transformers:skainet-transformers-inference-vec2text"))
            .using(project(":llm-inference:vec2text"))
    }
}

include(":cli")
include(":app")
