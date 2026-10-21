import org.jetbrains.intellij.platform.gradle.extensions.intellijPlatform

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

plugins {
    // Registers the `intellijPlatform { ... }` repositories helper for
    // dependencyResolutionManagement (IntelliJ Platform Gradle Plugin 2.x).
    id("org.jetbrains.intellij.platform.settings") version "2.9.0"
}

rootProject.name = "slang-idea-plugin"

dependencyResolutionManagement {
    // FAIL_ON_PROJECT_REPOS also stops the Grammar-Kit plugin from adding its
    // own project repositories (which would shadow the ones below);
    // `defaultRepositories()` includes intellij-dependencies, where
    // Grammar-Kit and the IntelliJ JFlex fork are published.
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        mavenCentral()
        intellijPlatform {
            defaultRepositories()
        }
    }
}
