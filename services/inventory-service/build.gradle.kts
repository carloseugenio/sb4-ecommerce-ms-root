plugins {
    id("java")
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
}

group = "br.com.cepp.ecommerce"
version = "1.0-SNAPSHOT"

dependencies {
    implementation(platform(libs.spring.boot.bom))
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.actuator)
}
