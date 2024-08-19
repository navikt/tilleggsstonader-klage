/*
 * This file was generated by the Gradle 'init' task.
 */

group = "no.nav.tilleggsstonader.klage"
version = "1.0.0"
description = "tilleggsstonader-klage"
java.sourceCompatibility = JavaVersion.VERSION_21

val tilleggsstønaderLibsVersion = "2024.05.08-08.38.544e65c0c5a6"
val tilleggsstønaderKontrakterVersion = "2024.08.14-17.17.7812164fb0d8"

plugins {
    application
    `java-library`
    `maven-publish`

    kotlin("jvm") version "1.9.24"

    id("org.springframework.boot") version "3.2.5"
    id("io.spring.dependency-management") version "1.1.6"

    kotlin("plugin.spring") version "1.9.24"

}

repositories {
    mavenCentral()
    mavenLocal()

    maven {
        url = uri("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
    }
}


dependencies {
    implementation(libs.org.jetbrains.kotlin.kotlin.stdlib)
    implementation(libs.org.eclipse.jetty.jetty.server)

    implementation(libs.org.springframework.boot.spring.boot.starter.jetty)
    implementation(libs.org.springframework.boot.spring.boot.starter.data.jdbc)
    implementation(libs.org.springframework.boot.spring.boot.starter.actuator)
    implementation(libs.org.springframework.boot.spring.boot.starter.web)
    implementation(libs.org.springframework.boot.spring.boot.starter.validation)
    implementation(libs.org.springframework.kafka.spring.kafka)

    implementation(libs.org.flywaydb.flyway.core)
    implementation(libs.org.postgresql.postgresql)
    implementation(libs.com.fasterxml.jackson.module.jackson.module.kotlin)
    implementation(libs.org.springdoc.springdoc.openapi.starter.webmvc.ui)
    implementation(libs.org.springdoc.springdoc.openapi.starter.common)
    implementation(libs.com.github.ben.manes.caffeine.caffeine)
    implementation(libs.org.slf4j.slf4j.api)
    implementation(libs.ch.qos.logback.logback.classic)
    implementation(libs.com.papertrailapp.logback.syslog4j)
    implementation(libs.io.micrometer.micrometer.registry.prometheus)

    implementation(libs.no.nav.security.token.client.core)
    implementation(libs.no.nav.security.token.client.spring)
    implementation(libs.no.nav.security.token.validation.core)

    implementation(libs.no.nav.familie.felles.util)
    implementation(libs.no.nav.familie.felles.http.client)
    implementation(libs.no.nav.familie.prosessering.core)
    implementation(libs.no.nav.familie.felles.log)
    implementation(libs.no.nav.familie.felles.kafka)

    implementation("no.nav.tilleggsstonader-libs:util:$tilleggsstønaderLibsVersion")
    implementation("no.nav.tilleggsstonader-libs:log:$tilleggsstønaderLibsVersion")
    implementation("no.nav.tilleggsstonader-libs:http-client:$tilleggsstønaderLibsVersion")
    implementation("no.nav.tilleggsstonader-libs:sikkerhet:$tilleggsstønaderLibsVersion")

    implementation("no.nav.tilleggsstonader.kontrakter:tilleggsstonader-kontrakter:$tilleggsstønaderKontrakterVersion")

    testImplementation(libs.org.springframework.boot.spring.boot.starter.test)
    testImplementation(libs.org.jetbrains.kotlin.kotlin.test.junit5)
    testImplementation(libs.io.mockk.mockk.jvm)
    testImplementation(libs.com.github.tomakehurst.wiremock.jre8.standalone)
    testImplementation(libs.no.nav.security.token.validation.spring.test)
    testImplementation(libs.org.testcontainers.postgresql)
    testImplementation(libs.org.junit.platform.junit.platform.suite)
}

application {
    mainClass.set("no.nav.tilleggsstonader.klage.AppKt")
}


publishing {
    publications.create<MavenPublication>("maven") {
        from(components["java"])
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