plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

dependencies {
    implementation(project(":core"))

    // Spring Batch
    implementation("org.springframework.boot:spring-boot-starter-batch")

    // Scheduling
    implementation("org.springframework.boot:spring-boot-starter-quartz")

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.batch:spring-batch-test")
}

tasks.bootJar {
    archiveFileName.set("karrot-zeronine-batch.jar")
}
