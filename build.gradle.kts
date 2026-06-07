plugins {
  java
  id("org.springframework.boot") version "3.5.13"
  id("io.spring.dependency-management") version "1.1.7"

  kotlin("jvm") version "2.3.20" apply false
  kotlin("plugin.spring") version "2.3.20" apply false
}

group = "krilovs.andrejs"
version = "1.0-SNAPSHOT"

repositories {
  mavenCentral()
}

java {
  toolchain {
    languageVersion = JavaLanguageVersion.of(21)
  }
}

dependencies {
  implementation(project(":chess-backend"))
  developmentOnly("org.springframework.boot:spring-boot-devtools")
  implementation("org.springframework.boot:spring-boot-starter-web")
  implementation("org.springframework.boot:spring-boot-starter-security")
}

val chessFrontendDir = file("chess-frontend")
val chessBackendStaticDir = "src/main/resources/static/chess"
val npmCommand = if (System.getProperty("os.name").contains("Windows")) "npm.cmd" else "npm"

tasks.register<Exec>("npmInstall") {
  workingDir = chessFrontendDir
  commandLine(npmCommand, "install")
  inputs.file("$chessFrontendDir/package.json")
  outputs.dir("$chessFrontendDir/node_modules")
}

tasks.register<Exec>("npmBuild") {
  workingDir = chessFrontendDir
  commandLine(npmCommand, "run", "build")
  dependsOn("npmInstall")
  inputs.dir("$chessFrontendDir/src")
  inputs.file("$chessFrontendDir/package.json")
  outputs.dir("$chessFrontendDir/dist")
}

tasks.register<Copy>("copyFrontend") {
  dependsOn("npmBuild")
  from("$chessFrontendDir/dist")
  into(chessBackendStaticDir)
  doFirst {
    delete(chessBackendStaticDir)
  }
}

tasks.named("processResources") {
  dependsOn("copyFrontend")
}

tasks.named("clean") {
  doLast {
    delete(layout.projectDirectory.dir(chessBackendStaticDir))
  }
}