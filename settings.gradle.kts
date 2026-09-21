pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "ProAnimator"
include(":app")
include(":core:engine")
include(":core:timeline")
include(":core:brushes")
include(":core:fileformat")
include(":core:export")
include(":rendering")
include(":domain")
include(":data")
