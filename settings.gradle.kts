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
include(":domain")
include(":data")
include(":core:engine")
include(":core:timeline")
include(":core:brushes")
include(":core:export")
include(":core:fileformat")
include(":core:ink")
include(":core:lottie")
include(":rendering")
