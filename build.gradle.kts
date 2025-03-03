plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "7.1.2"
  kotlin("jvm") version "2.1.10"
  kotlin("plugin.spring") version "2.1.10"
  id("jacoco")
  id("org.barfuin.gradle.jacocolog") version "3.1.0"
}

configurations {
  testImplementation { exclude(group = "org.junit.vintage") }
}

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("com.amazon.redshift:redshift-jdbc4-no-awssdk:1.2.45.1069")
  implementation("org.postgresql:postgresql:42.7.5")
  implementation("uk.gov.justice.service.hmpps:hmpps-digital-prison-reporting-lib:7.10.3")
  implementation("com.google.code.gson:gson:2.11.0")
  implementation("software.amazon.awssdk:redshiftdata:2.30.0")
  implementation("software.amazon.awssdk:athena:2.30.1")
  implementation("software.amazon.awssdk:sts:2.30.31")

  // Security
  implementation("org.springframework.boot:spring-boot-starter-security")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-client")

  // Swagger
  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.5")

  // Testing
  testImplementation("com.h2database:h2")
  testImplementation("io.jsonwebtoken:jjwt:0.12.6")
  testImplementation("javax.xml.bind:jaxb-api:2.4.0-b180830.0359")
}

java {
  toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

repositories {
  mavenLocal()
  mavenCentral()
  maven("https://s3.amazonaws.com/redshift-maven-repository/release")
}

kotlin {
  jvmToolchain(21)
}

tasks.test {
  finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
  dependsOn(tasks.test)
}
