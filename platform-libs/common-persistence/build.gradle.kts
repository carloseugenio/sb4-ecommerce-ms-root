plugins {
    id("java-library")
    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.spring.dependency.management)    
}

group = "br.com.cepp.ecommerce.platform.persistence"
version = "1.0-SNAPSHOT"

dependencies {
}
