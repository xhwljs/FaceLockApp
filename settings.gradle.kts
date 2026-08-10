pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // InspireFace Android SDK is published on JitPack (official deepinsight SDK path).
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "InsightFaceRecognizer"
include(":app")
