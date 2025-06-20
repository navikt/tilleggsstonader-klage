import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import java.io.ByteArrayOutputStream

val javaVersion = 21
val familieProsesseringVersion = "2.20250526085951_e212049"
val tilleggsstønaderLibsVersion = "2025.05.26-09.43.0bdd5b9aa775"
val tilleggsstønaderKontrakterVersion = "2025.06.10-12.56.ea582af44a70"
val tokenSupportVersion = "5.0.29"
val wiremockVersion = "3.13.1"
val mockkVersion = "1.14.2"
val testcontainerVersion = "1.21.1"
val springDocVersion = "2.8.9"

group = "no.nav.tilleggsstonader.klage"
version = "1.0.0"

plugins {
    application

    kotlin("jvm") version "2.1.21"
    kotlin("plugin.spring") version "2.1.21"

    id("com.diffplug.spotless") version "7.0.4"
    id("com.github.ben-manes.versions") version "0.52.0"
    id("se.patrikerdes.use-latest-versions") version "0.2.18"

    id("org.springframework.boot") version "3.4.5"
    id("io.spring.dependency-management") version "1.1.7"

    id("org.cyclonedx.bom") version "2.3.1"
}

repositories {
    mavenCentral()
    mavenLocal()

    maven {
        url = uri("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
    }
}

apply(plugin = "com.diffplug.spotless")

spotless {
    kotlin {
        ktlint("1.5.0")
    }
}

configurations.all {
    resolutionStrategy {
        failOnNonReproducibleResolution()
    }
}

dependencies {
    // Spring
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-data-jdbc")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.springframework.kafka:spring-kafka")

    implementation("org.postgresql:postgresql")
    implementation("org.flywaydb:flyway-core:11.9.1")

    implementation("net.logstash.logback:logstash-logback-encoder:8.1")
    implementation("io.micrometer:micrometer-registry-prometheus")

    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:$springDocVersion")
    implementation("org.springdoc:springdoc-openapi-starter-common:$springDocVersion")

    implementation("no.nav.familie:prosessering-core:$familieProsesseringVersion")

    implementation("no.nav.tilleggsstonader-libs:util:$tilleggsstønaderLibsVersion")
    implementation("no.nav.tilleggsstonader-libs:log:$tilleggsstønaderLibsVersion")
    implementation("no.nav.tilleggsstonader-libs:kafka:$tilleggsstønaderLibsVersion")
    implementation("no.nav.tilleggsstonader-libs:http-client:$tilleggsstønaderLibsVersion")
    implementation("no.nav.tilleggsstonader-libs:sikkerhet:$tilleggsstønaderLibsVersion")
    implementation("no.nav.tilleggsstonader-libs:unleash:$tilleggsstønaderLibsVersion")
    implementation("no.nav.tilleggsstonader-libs:spring:$tilleggsstønaderLibsVersion")

    implementation("no.nav.tilleggsstonader.kontrakter:kontrakter-felles:$tilleggsstønaderKontrakterVersion")

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(module = "mockito-core")
    }
    testImplementation("org.junit.platform:junit-platform-suite")
    testImplementation("org.wiremock:wiremock-standalone:$wiremockVersion")
    testImplementation("io.mockk:mockk:$mockkVersion")
    testImplementation("org.testcontainers:postgresql:$testcontainerVersion")
    testImplementation("no.nav.security:token-validation-spring-test:$tokenSupportVersion")
    testImplementation("no.nav.tilleggsstonader-libs:test-util:$tilleggsstønaderLibsVersion")
}

application {
    mainClass.set("no.nav.tilleggsstonader.klage.ApplicationKt")
}

kotlin {
    jvmToolchain(javaVersion)

    compilerOptions {
        freeCompilerArgs.add("-Xjsr305=strict")
    }
}

if (project.hasProperty("skipLint")) {
    gradle.startParameter.excludedTaskNames += "spotlessKotlinCheck"
}

tasks.bootJar {
    archiveFileName.set("app.jar")
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events = setOf(TestLogEvent.FAILED)
        exceptionFormat = TestExceptionFormat.FULL
        showStackTraces = false
        showCauses = false
    }
}

tasks.cyclonedxBom {
    setIncludeConfigs(listOf("runtimeClasspath", "compileClasspath"))
}

// Oppretter version.properties med git-sha som version
tasks {
    fun getCheckedOutGitCommitHash(): String {
        if (System.getenv("GITHUB_ACTIONS") == "true") {
            return System.getenv("GITHUB_SHA")
        }
        val byteOut = ByteArrayOutputStream()
        project.exec {
            commandLine = "git rev-parse --verify HEAD".split("\\s".toRegex())
            standardOutput = byteOut
        }
        return String(byteOut.toByteArray()).trim()
    }

    val projectProps by registering(WriteProperties::class) {
        destinationFile = layout.buildDirectory.file("version.properties")
        // Define property.
        property("project.version", getCheckedOutGitCommitHash())
    }

    processResources {
        // Depend on output of the task to create properties,
        // so the properties file will be part of the Java resources.
        from(projectProps)
    }
}
