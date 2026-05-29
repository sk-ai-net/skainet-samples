rootProject.name = "glove-embeddings"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        // mavenLocal is scoped to sk.ainet so a partial local Kotlin/Compose artifact
        // cannot shadow the real one from Central/Google (see the note below).
        mavenLocal {
            content { includeGroupByRegex("sk\\.ainet.*") }
        }
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        // SKaiNET 0.25.0 is published to Maven Central; mavenLocal lets a locally
        // published build take precedence when iterating against the source repo.
        //
        // Restrict mavenLocal to the sk.ainet group: an unrestricted mavenLocal is
        // consulted first and can contain a partial kotlin-stdlib (JVM jar + POM, no
        // Gradle module metadata, no klib variants). That POM shadows Maven Central's
        // variant-aware metadata, so the JS/wasm targets lose their stdlib klib and
        // fail with "Missing stdlib class". Scoping mavenLocal keeps the local-SKaiNET
        // override without poisoning Kotlin/Compose/AndroidX resolution.
        mavenLocal {
            content { includeGroupByRegex("sk\\.ainet.*") }
        }
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

// Shared SKaiNET design system (theme + components). Consumed as an included
// build so the example always uses the local source, matching the sibling
// SinusApproximator / KllamaDemo examples.
includeBuild("../skainet-ui")

include(":glove")
include(":app")
