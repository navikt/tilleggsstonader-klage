/*
 * This file was generated by the Gradle 'init' task.
 */

group = "no.nav.tilleggsstonader.klage"
version = "1.0.0"
val javaVersion = JavaVersion.VERSION_21

plugins {
    application
    `java-library`
    `maven-publish`

    kotlin("jvm") version "1.9.24"

    id("org.springframework.boot") version "3.2.5"
    id("io.spring.dependency-management") version "1.1.6"

    kotlin("plugin.spring") version "1.9.24"

    id("org.cyclonedx.bom") version "1.8.2"
}

repositories {
    mavenCentral()
    mavenLocal()

    maven {
        url = uri("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
    }
}

val tilleggsstonaderLibsVersion = "2024.05.27-10.56.b9a67bfd6080"
val tilleggsstonaderKontrakterVersion = "2024.08.12-08.20.e194558f350e"
val familieFellesVersion = "3.20240515152313_9dd5659"
val navSecurityVersion = "4.1.7"
val springBootVersion = "3.2.5"
val kotlinVersion = "1.9.24"
val springDocVersion = "2.5.0"

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")

    implementation("org.springframework.boot:spring-boot-starter-jetty:$springBootVersion")
    implementation("org.springframework.boot:spring-boot-starter-data-jdbc:$springBootVersion")
    implementation("org.springframework.boot:spring-boot-starter-actuator:$springBootVersion")
    implementation("org.springframework.boot:spring-boot-starter-web:$springBootVersion")
    implementation("org.springframework.boot:spring-boot-starter-validation:$springBootVersion")
    implementation("org.springframework.kafka:spring-kafka:3.1.4")

    implementation("org.flywaydb:flyway-core:9.22.3")
    implementation("org.postgresql:postgresql:42.6.2")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.17.1")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:$springDocVersion")
    implementation("org.springdoc:springdoc-openapi-starter-common:$springDocVersion")
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")
    implementation("org.slf4j:slf4j-api:2.0.13")
    implementation("ch.qos.logback:logback-classic:1.4.14")
    implementation("com.papertrailapp:logback-syslog4j:1.0.0")
    implementation("io.micrometer:micrometer-registry-prometheus:1.12.5")

    implementation("no.nav.security:token-client-core:$navSecurityVersion")
    implementation("no.nav.security:token-client-spring:$navSecurityVersion")
    implementation("no.nav.security:token-validation-core:$navSecurityVersion")

    implementation("no.nav.familie.felles:util:$familieFellesVersion")
    implementation("no.nav.familie.felles:http-client:$familieFellesVersion")
    implementation("no.nav.familie.felles:log:$familieFellesVersion")
    implementation("no.nav.familie.felles:kafka:$familieFellesVersion")

    implementation("no.nav.familie:prosessering-core:2.20240522090805_0e9c7a6")

    implementation("no.nav.tilleggsstonader-libs:util:$tilleggsstonaderLibsVersion")
    implementation("no.nav.tilleggsstonader-libs:log:$tilleggsstonaderLibsVersion")
    implementation("no.nav.tilleggsstonader-libs:http-client:$tilleggsstonaderLibsVersion")
    implementation("no.nav.tilleggsstonader-libs:sikkerhet:$tilleggsstonaderLibsVersion")

    implementation("no.nav.tilleggsstonader.kontrakter:tilleggsstonader-kontrakter:$tilleggsstonaderKontrakterVersion")

    testImplementation("org.springframework.boot:spring-boot-starter-test:$springBootVersion")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:$kotlinVersion")
    testImplementation("io.mockk:mockk-jvm:1.13.10")
    testImplementation("com.github.tomakehurst:wiremock-jre8-standalone:2.35.2")
    testImplementation("no.nav.security:token-validation-spring-test:$navSecurityVersion")
    testImplementation("org.testcontainers:postgresql:1.19.8")
    testImplementation("org.junit.platform:junit-platform-suite:1.10.2")
}

application {
    mainClass.set("no.nav.tilleggsstonader.klage.ApplicationKt")
}

java.sourceCompatibility = javaVersion

kotlin {
    jvmToolchain(javaVersion.majorVersion.toInt())

    compilerOptions {
        freeCompilerArgs.add("-Xjsr305=strict")
    }
}

tasks.withType<JavaCompile>() {
    options.encoding = "UTF-8"
}

tasks.withType<Javadoc>() {
    options.encoding = "UTF-8"
}

tasks.bootJar {
    archiveFileName.set("app.jar")
}

tasks.test {
    useJUnitPlatform()
}