import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm")
    kotlin("plugin.spring") apply false
    kotlin("plugin.jpa") apply false
    id("org.springframework.boot") apply false
    id("io.spring.dependency-management") apply false
}

val kotestVersion: String by project
val mockkVersion: String by project

allprojects {
    group = property("group") as String
    version = property("version") as String

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "kotlin")
    apply(plugin = "io.spring.dependency-management")

    dependencies {
        // Kotlin
        implementation(kotlin("stdlib"))
        implementation(kotlin("reflect"))

        // Coroutines
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.10.1")

        // Logging
        implementation("io.github.oshai:kotlin-logging-jvm:7.0.3")

        // Testing
        testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
        testImplementation("io.kotest:kotest-assertions-core:$kotestVersion")
        testImplementation("io.kotest:kotest-property:$kotestVersion")
        testImplementation("io.kotest.extensions:kotest-extensions-spring:1.3.0")
        testImplementation("io.mockk:mockk:$mockkVersion")
    }

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
            freeCompilerArgs.addAll(
                "-Xjsr305=strict",
                "-Xcontext-receivers"
            )
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}
