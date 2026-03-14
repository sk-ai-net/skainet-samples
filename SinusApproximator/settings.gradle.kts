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
        mavenLocal()
        google()
        mavenCentral()
    }
}

includeBuild("../skainet-ui")

includeBuild("../../SKaiNET") {
    dependencySubstitution {
        substitute(module("sk.ainet.core:skainet-lang-core")).using(project(":skainet-lang:skainet-lang-core"))
        substitute(module("sk.ainet.core:skainet-lang-models")).using(project(":skainet-lang:skainet-lang-models"))
        substitute(module("sk.ainet.core:skainet-lang-kan")).using(project(":skainet-lang:skainet-kan"))
        substitute(module("sk.ainet.core:skainet-lang-dag")).using(project(":skainet-lang:skainet-lang-dag"))
        substitute(module("sk.ainet.core:skainet-compile-core")).using(project(":skainet-compile:skainet-compile-core"))
        substitute(module("sk.ainet.core:skainet-compile-dag")).using(project(":skainet-compile:skainet-compile-dag"))
        substitute(module("sk.ainet.core:skainet-backend-cpu")).using(project(":skainet-backends:skainet-backend-cpu"))
        substitute(module("sk.ainet.core:skainet-data-api")).using(project(":skainet-data:skainet-data-api"))
        substitute(module("sk.ainet.core:skainet-data-basic")).using(project(":skainet-data:skainet-data-simple"))
        substitute(module("sk.ainet.core:skainet-io-core")).using(project(":skainet-io:skainet-io-core"))
        substitute(module("sk.ainet.core:skainet-io-gguf")).using(project(":skainet-io:skainet-io-gguf"))
        substitute(module("sk.ainet.core:skainet-io-onnx")).using(project(":skainet-io:skainet-io-onnx"))
        substitute(module("sk.ainet.core:skainet-model-yolo")).using(project(":skainet-models:skainet-model-yolo"))
    }
}

include(":composeApp")
include(":shared")
