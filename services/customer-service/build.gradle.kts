plugins {
    id("java")
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
}

group = "br.com.cepp.ecommerce"
version = "1.0-SNAPSHOT"

dependencies {
    // 1. Tell this module where to look up Spring versions
    implementation(platform(libs.spring.boot.bom))

    implementation("org.springframework.boot:spring-boot-starter-web")

    // Import your internal shared module
    implementation(project(":platform-libs:common-domain"))
    implementation(project(":platform-libs:common-messaging"))
    implementation(project(":platform-libs:common-persistence"))
    implementation(project(":platform-libs:common-security"))
}