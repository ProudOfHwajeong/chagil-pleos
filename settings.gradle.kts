pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Pleos Playground public Maven (Vehicle SDK). The SDK binary is NOT committed to this repo.
        maven("https://nexus-playground.pleos.ai/repository/maven-releases/")
    }
}
rootProject.name = "chagil"
include(":app")
