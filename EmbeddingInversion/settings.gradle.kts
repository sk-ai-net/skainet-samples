pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    // Repositories are declared per-module (:cli, :app) so their own blocks apply; see there.
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "embedding-inversion"

// Everything is consumed from Maven Central now: SKaiNET core + SKaiNET-transformers
// (incl. the t5 / vec2text modules) are all published at 0.36.0. No composite build.

include(":cli")
include(":app")
