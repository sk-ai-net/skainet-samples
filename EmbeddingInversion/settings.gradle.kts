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

// Composite build: consume the local SKaiNET + SKaiNET-transformers checkouts directly
// (source), so this example builds against the just-merged t5 / vec2text modules without
// waiting for a published release. Gradle substitutes every `sk.ainet.core:*` and
// `sk.ainet.transformers:*` dependency with the matching local project by coordinate.
//
// Both are included here at the root so transformers resolves `sk.ainet.core:*` from this
// same composite — do NOT also set `useLocalSkainet` in the transformers build, or SKaiNET
// would be included twice.
// SKaiNET core modules auto-substitute (project name == published artifactId).
includeBuild("../../SKaiNET")
// The t5 / vec2text modules publish as `skainet-transformers-inference-*`, which differs
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
