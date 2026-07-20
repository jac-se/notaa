pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev") {
            name = "Compose for Web"
        }
    }
    
    plugins {
        id("org.jetbrains.compose") version "1.6.0" apply false
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.10.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev") {
            name = "Compose for Web"
        }
    }
}

rootProject.name = "Nota JACC"
include(":app")