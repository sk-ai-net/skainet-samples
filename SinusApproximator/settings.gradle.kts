rootProject.name = "SinusApproximator"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        mavenLocal()
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

includeBuild("../skainet-ui")

include(":composeApp")
include(":shared")
