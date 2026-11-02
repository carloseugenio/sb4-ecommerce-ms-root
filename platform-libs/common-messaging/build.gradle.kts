plugins {
    id("java-library")
    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.spring.dependency.management)
}

group = "br.com.cepp.ecommerce.platform.messaging"
version = "1.0-SNAPSHOT"

dependencies {
    implementation(platform(libs.spring.boot.bom))

    api(project(":platform-libs:common-domain"))
    api(libs.spring.boot.starter.kafka)
    api(libs.spring.boot.starter.data.jdbc)
    api(libs.spring.boot.starter.json)

    testImplementation(libs.spring.boot.starter.test)
}
