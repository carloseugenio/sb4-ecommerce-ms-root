plugins {
    id("java")
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    alias(libs.plugins.liquibase.gradle)
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
    implementation(libs.spring.boot.starter.liquibase)
    implementation(libs.spring.boot.starter.security)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.hateoas)
    implementation(libs.spring.boot.starter.restclient)

    // --- Lombok & MapStruct Pipeline ---
    // 1. Lombok
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    // 2. MapStruct
    implementation(libs.mapstruct.core)
    annotationProcessor(libs.mapstruct.processor)

    // 3. Binding tool (Allows MapStruct to read Lombok getters/setters)
    annotationProcessor(libs.lombok.mapstruct.binding)

    // AWS integration
//    implementation(libs.spring.cloud.starter.aws)
//    implementation(libs.spring.cloud.starter.aws.messaging)
//    implementation(libs.aws.sdk.s3)
//    implementation(libs.aws.sdk.dynamodb)

    // Observability
//    implementation(libs.micrometer.registry.cloudwatch)

    // Testing
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.boot.starter.liquibase.test)

    developmentOnly(libs.spring.boot.devtools)
    runtimeOnly(libs.h2.database)
    runtimeOnly(libs.postgresql)
    testImplementation(libs.spring.boot.starter.data.jpa.test)
    testImplementation(libs.spring.boot.starter.hateoas.test)
    testImplementation(libs.spring.boot.starter.restclient.test)

    // Test containers
    testImplementation(libs.spring.boot.testcontainers)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.junit.jupter)
    testImplementation(libs.testcontainers.localstack)

}

// Global configuration for Docker Container Generation
tasks.bootBuildImage {
    // Generates an optimized, secure, distroless-like production image
    imageName.set("carloseugenio/${project.name}:${project.version}")

    // Configures the JVM memory and runtime options inside the container
    environment.set(mapOf(
        "BP_JVM_VERSION" to "21"
    ))
}
