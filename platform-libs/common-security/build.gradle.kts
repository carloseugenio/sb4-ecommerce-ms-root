plugins {
    id("java-library")
    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.spring.dependency.management)
}

group = "br.com.cepp.ecommerce.platform.security"
version = "1.0-SNAPSHOT"

dependencies {
    // 1. Tell this module where to look up Spring versions
    implementation(platform(libs.spring.boot.bom))

    implementation(libs.spring.boot.starter.security)
    implementation(libs.lombok)
    annotationProcessor(libs.lombok)

    // JWT Implementation
    implementation(libs.jjwt.api)
    runtimeOnly(libs.jjwt.impl)
    runtimeOnly(libs.jjwt.jackson)
}
