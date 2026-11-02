rootProject.name = "sb4-ecommerce-ms-root"
// If creating gradle plugins
pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

// Centralize repository management for all subprojects
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
    }
    // Automatically loads gradle/libs.versions.toml
    versionCatalogs {
        create("libs")
    }
}

// Include all libraries modules
include("platform-libs:common-domain")
include("platform-libs:common-security")
include("platform-libs:common-messaging")
include("platform-libs:common-persistence")
include("platform-libs:common-spring")

// Include all service modules
include("services:customer-service")
include("services:inventory-service")
include("services:order-service")
include("services:payment-service")
