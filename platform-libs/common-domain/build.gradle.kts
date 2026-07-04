plugins {
    id("java-library")
    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.spring.dependency.management)
}

group = "br.com.cepp.ecommerce.platform.domain"
version = "1.0-SNAPSHOT"

dependencies {
    // Just the essentials - no Spring Boot autoconfiguration
    implementation(libs.spring.boot.starter.json)
    implementation(libs.spring.boot.starter.validation)
    
    // Lombok for reducing boilerplate
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    // Optional: Include for testing scopes
    testCompileOnly(libs.lombok)
    testAnnotationProcessor(libs.lombok)
}
