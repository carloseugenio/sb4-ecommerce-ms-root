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

    // Internal dependencies
    implementation(project(":platform-libs:common-domain"))
    implementation(project(":platform-libs:common-security"))
    implementation(project(":platform-libs:common-messaging"))
    implementation(project(":platform-libs:common-persistence"))
    
    // Spring Boot starters
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.security)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.spring.boot.starter.validation)
    
    // AWS integration
    implementation(libs.spring.cloud.starter.aws)
    implementation(libs.spring.cloud.starter.aws.messaging)
    implementation(libs.aws.sdk.s3)
    implementation(libs.aws.sdk.dynamodb)
    
    // Database
    implementation(libs.postgresql)
    implementation(libs.flyway.core)
    
    // Observability
    implementation(libs.micrometer.registry.cloudwatch)
    
    // Testing
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.localstack)

    implementation("org.springframework.boot:spring-boot-h2console")
    implementation("org.springframework.boot:spring-boot-starter-hateoas")
    implementation("org.springframework.boot:spring-boot-starter-liquibase")
    implementation("org.springframework.boot:spring-boot-starter-restclient")
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    runtimeOnly("com.h2database:h2")
    runtimeOnly("org.postgresql:postgresql")
    testImplementation("org.springframework.boot:spring-boot-starter-actuator-test")
    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
    testImplementation("org.springframework.boot:spring-boot-starter-hateoas-test")
    testImplementation("org.springframework.boot:spring-boot-starter-liquibase-test")
    testImplementation("org.springframework.boot:spring-boot-starter-restclient-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
    testImplementation("org.testcontainers:testcontainers-postgresql")

    // Lombok
    implementation(libs.lombok)
    annotationProcessor(libs.lombok)

    // # MapStruct
    implementation("org.mapstruct:mapstruct:1.5.5.Final")
    annotationProcessor("org.mapstruct:mapstruct-processor:1.5.5.Final")

}
