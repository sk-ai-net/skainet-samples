rootProject.name = "SinusApproximator"
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
        google()
        mavenCentral()
        mavenLocal {
            mavenContent {
                includeGroupAndSubgroups("sk.ainet")
            }
        }
    }
}

// Shared SKaiNET design system (theme + components) built from local source.
// SKaiNET core/transformers artifacts are consumed from Maven Central.
includeBuild("../skainet-ui")

include(":composeApp")
include(":shared")
include(":androidApp")
