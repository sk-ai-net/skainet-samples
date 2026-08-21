rootProject.name = "KernelRace"
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
// KllamaDemo / TinyTransformer examples.
includeBuild("../skainet-ui")

include(":androidApp")
include(":composeApp")
include(":shared")
include(":webWorker")
