import org.gradle.api.tasks.testing.logging.TestLogEvent

val javaVersion = 21
val familieProsesseringVersion = "2.20240916095727_7fab7b4"
val tilleggsstønaderLibsVersion = "2024.09.13-14.55.90172c4319d3"
val tilleggsstønaderKontrakterVersion = "2024.10.01-15.27.c053738604a6"
val tokenSupportVersion = "5.0.5"
val wiremockVersion = "3.9.1"
val mockkVersion = "1.13.12"
val testcontainerVersion = "1.20.1"
val familieFellesVersion = "3.20240913110742_adb42f8"
val springDocVersion = "2.6.0"

group = "no.nav.tilleggsstonader.klage"
version = "1.0.0"

plugins {
    application

    kotlin("jvm") version "2.0.20"
    kotlin("plugin.spring") version "2.0.20"

    id("com.diffplug.spotless") version "6.25.0"
    id("com.github.ben-manes.versions") version "0.51.0"
    id("se.patrikerdes.use-latest-versions") version "0.2.18"

    id("org.springframework.boot") version "3.3.3"
    id("io.spring.dependency-management") version "1.1.6"

    id("org.cyclonedx.bom") version "1.10.0"
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
        ktlint("0.50.0")
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
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.17.2")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.springframework.kafka:spring-kafka")

    implementation("org.postgresql:postgresql")
    implementation("org.flywaydb:flyway-core:10.18.0")

    implementation("net.logstash.logback:logstash-logback-encoder:8.0")
    implementation("io.micrometer:micrometer-registry-prometheus")

    // For auditlogger. August 2014, men det er den som blir brukt på NAV
    implementation("com.papertrailapp:logback-syslog4j:1.0.0")

    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:$springDocVersion")
    implementation("org.springdoc:springdoc-openapi-starter-common:$springDocVersion")

    implementation("no.nav.familie.felles:util:$familieFellesVersion")
    implementation("no.nav.familie.felles:http-client:$familieFellesVersion")
    implementation("no.nav.familie.felles:log:$familieFellesVersion")
    implementation("no.nav.familie.felles:kafka:$familieFellesVersion")

    implementation("no.nav.familie:prosessering-core:$familieProsesseringVersion")

    implementation("no.nav.tilleggsstonader-libs:util:$tilleggsstønaderLibsVersion")
    implementation("no.nav.tilleggsstonader-libs:log:$tilleggsstønaderLibsVersion")
    implementation("no.nav.tilleggsstonader-libs:http-client:$tilleggsstønaderLibsVersion")
    implementation("no.nav.tilleggsstonader-libs:sikkerhet:$tilleggsstønaderLibsVersion")
    implementation("no.nav.tilleggsstonader-libs:unleash:$tilleggsstønaderLibsVersion")

    implementation("no.nav.tilleggsstonader.kontrakter:tilleggsstonader-kontrakter:$tilleggsstønaderKontrakterVersion")

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
    }
}

tasks.cyclonedxBom {
    setIncludeConfigs(listOf("runtimeClasspath", "compileClasspath"))
}
