plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    kotlin("plugin.jpa")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

tasks.bootJar { enabled = false }
tasks.jar { enabled = true }

val redissonVersion: String by project

dependencies {
    api(project(":common"))

    // Spring Boot
    api("org.springframework.boot:spring-boot-starter-data-jpa")
    api("org.springframework.boot:spring-boot-starter-data-redis")
    api("org.springframework.boot:spring-boot-starter-validation")

    // Database
    runtimeOnly("org.postgresql:postgresql")

    // Redis & Distributed Lock
    api("org.redisson:redisson-spring-boot-starter:$redissonVersion")

    // Kafka
    api("org.springframework.kafka:spring-kafka")

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("com.h2database:h2")
    testImplementation("org.testcontainers:postgresql:1.20.4")
    testImplementation("org.testcontainers:kafka:1.20.4")
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}

noArg {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}
