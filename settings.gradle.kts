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

// Set up logical project structure
project(":platform-libs:common-domain").projectDir = file("platform-libs/common-domain")
project(":platform-libs:common-security").projectDir = file("platform-libs/common-security")
project(":platform-libs:common-messaging").projectDir = file("platform-libs/common-messaging")
project(":platform-libs:common-persistence").projectDir = file("platform-libs/common-persistence")

project(":services:customer-service").projectDir = file("services/customer-service")
project(":services:order-service").projectDir = file("services/order-service")
project(":services:inventory-service").projectDir = file("services/inventory-service")
project(":services:payment-service").projectDir = file("services/payment-service")
//project(":services:notification-service").projectDir = file("services/notification-service")
