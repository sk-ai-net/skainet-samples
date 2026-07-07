rootProject.name = "glove-embeddings"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
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
        // Resolve everything from Maven Central (SKaiNET 0.34.0 is published there).
        // No mavenLocal: an unrestricted mavenLocal is consulted first and can contain
        // a partial kotlin-stdlib (JVM jar + POM, no klib variants) that shadows
        // Central's variant-aware metadata, breaking JS/wasm with "Missing stdlib class".
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
// SinusApproximator example.
includeBuild("../skainet-ui")

include(":glove")
include(":app")
