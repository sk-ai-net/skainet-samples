rootProject.name = "KllamaDemo"
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
        mavenLocal()
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

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

includeBuild("../skainet-ui")

includeBuild("../../SKaiNET") {
    dependencySubstitution {
        substitute(module("sk.ainet.core:skainet-lang-core")).using(project(":skainet-lang:skainet-lang-core"))
        substitute(module("sk.ainet.core:skainet-lang-models")).using(project(":skainet-lang:skainet-lang-models"))
        substitute(module("sk.ainet.core:skainet-compile-core")).using(project(":skainet-compile:skainet-compile-core"))
        substitute(module("sk.ainet.core:skainet-backend-cpu")).using(project(":skainet-backends:skainet-backend-cpu"))
        substitute(module("sk.ainet.core:skainet-io-core")).using(project(":skainet-io:skainet-io-core"))
        substitute(module("sk.ainet.core:skainet-io-gguf")).using(project(":skainet-io:skainet-io-gguf"))
        substitute(module("sk.ainet.core:skainet-io-safetensors")).using(project(":skainet-io:skainet-io-safetensors"))
    }
}

includeBuild("../../SKaiNET-transformers") {
    dependencySubstitution {
        substitute(module("sk.ainet.transformers:skainet-transformers-inference-llama")).using(project(":llm-inference:llama"))
        substitute(module("sk.ainet.transformers:skainet-transformers-runtime-kllama")).using(project(":llm-runtime:kllama"))
        substitute(module("sk.ainet.transformers:skainet-transformers-agent")).using(project(":llm-agent"))
        substitute(module("sk.ainet.transformers:skainet-transformers-core")).using(project(":llm-core"))
    }
}

include(":composeApp")
include(":server")
include(":shared")