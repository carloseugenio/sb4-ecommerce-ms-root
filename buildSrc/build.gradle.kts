import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.`kotlin-dsl`
import org.gradle.kotlin.dsl.repositories

plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
}

dependencies {
    // Allows buildSrc to see your version catalogs (libs.versions.toml)
    implementation(files("../gradle/libs.versions.toml"))
}