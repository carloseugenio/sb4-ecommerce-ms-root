plugins {
    java
}

// 1. Access the version catalog inside buildSrc
val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21)) // Spring Boot 4 baseline
    }
}

// 2. Automatically inject the platform BOM and common starters into any module using this plugin
dependencies {
    // Enforce Spring Boot versions globally
    implementation(platform(libs.findLibrary("spring-boot-bom").get()))

    // Add common dependencies that EVERY module needs (e.g., Validation & JSON)
    implementation(libs.findLibrary("spring-boot-starter-validation").get())
    implementation(libs.findLibrary("spring-boot-starter-json").get())

    // Common testing framework
    testImplementation(libs.findLibrary("spring-boot-starter-test").get())
}

tasks.test {
    useJUnitPlatform()
}
