plugins {
    id("java")
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
}

group = "br.com.cepp.ecommerce"
version = "1.0-SNAPSHOT"

dependencies {
    implementation(platform(libs.spring.boot.bom))

    implementation(project(":platform-libs:common-domain"))
    implementation(project(":platform-libs:common-messaging"))

    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.liquibase)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.actuator)

    runtimeOnly(libs.postgresql)

    testImplementation(libs.spring.boot.starter.test)
}