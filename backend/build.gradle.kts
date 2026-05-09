plugins {
  kotlin("jvm") version "2.3.20"
  kotlin("plugin.spring") version "2.3.20"
  id("org.springframework.boot") version "3.5.13"
  id("io.spring.dependency-management") version "1.1.7"
}

group = "krilovs.andrejs.chess"
version = "1.0-SNAPSHOT"

dependencies {
  implementation(kotlin("stdlib"))
  developmentOnly("org.springframework.boot:spring-boot-devtools")
  implementation("org.springframework.boot:spring-boot-starter-web")
  implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
  implementation("org.springframework.boot:spring-boot-starter-websocket")
}

repositories {
  mavenCentral()
}

kotlin {
  jvmToolchain(21)
}