pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://cruglobal.jfrog.io/artifactory/maven-mobile/") {
            content {
                includeGroupAndSubgroups("org.ccci.gto.android")
            }
        }
    }
}

rootProject.name = "soularium"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

includeBuild("build-logic")

include(":androidApp")
include(":shared")

include(":module:db")
include(":module:model")
