rootProject.name = "MNISTDemo"
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
        // Resolve everything from Maven Central (SKaiNET is published there). No
        // mavenLocal: an unrestricted mavenLocal is consulted first and can contain a
        // partial kotlin-stdlib (JVM jar + POM, no klib variants) that shadows Central's
        // variant-aware metadata, breaking the JS/wasm targets with "Missing stdlib class".
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

includeBuild("../skainet-ui")

include(":composeApp")
include(":shared")
include(":cli")
include(":androidApp")
