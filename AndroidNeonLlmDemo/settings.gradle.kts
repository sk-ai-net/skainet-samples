rootProject.name = "AndroidNeonLlmDemo"

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
        mavenLocal {
            mavenContent {
                includeGroupAndSubgroups("sk.ainet")
            }
        }
    }
}

// Shared SKaiNET design system (theme, logo colors, FadingRingLoader)
includeBuild("../skainet-ui")

include(":app")
