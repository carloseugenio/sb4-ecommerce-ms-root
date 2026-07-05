plugins {
    id ("java-library")
    // Load plugins from TOML without applying them to the root
    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.spring.dependency.management) apply false
}

// 📂 Import the separated validation script
project.apply { from("container-validation.gradle.kts") }

tasks.jar { enabled = false }

subprojects {
    plugins.withType<JavaPlugin> {

        tasks.withType<Test> {
            useJUnitPlatform()
        }

        dependencies {
            implementation(platform(libs.spring.boot.bom))
            add("testImplementation", libs.spring.boot.starter.test)
            add("testRuntimeOnly", "org.junit.platform:junit-platform-launcher")
        }
    }

    plugins.withType<org.springframework.boot.gradle.plugin.SpringBootPlugin> {
        dependencies {
            // "developmentOnly" must be invoked as a string in Kotlin DSL if not explicitly declared
            "developmentOnly"("org.springframework.boot:spring-boot-docker-compose")
        }
        // Configures the bootRun task if you are using Option A (Centralized root compose file)
        tasks.withType<org.springframework.boot.gradle.tasks.run.BootRun> {
            systemProperty("spring.docker.compose.file", "${project.projectDir}/docker-compose.yaml")
        }
    }
}
