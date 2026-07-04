plugins {
    id ("java-library")
    // Apply the aggregation plugin at the root level
    `jacoco-report-aggregation`
    // Load plugins from TOML without applying them to the root
    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.spring.dependency.management) apply false
}

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
}
