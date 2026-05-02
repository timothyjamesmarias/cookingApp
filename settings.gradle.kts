pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
}

rootProject.name = "cookingApp"

include(":shared:domain")
include(":backend")
include(":frontend:shared")
include(":frontend:android")
include(":frontend:ios")
include(":frontend:desktop")
// Temporarily disabled - needs JS target in shared module
// include(":frontend:web")
