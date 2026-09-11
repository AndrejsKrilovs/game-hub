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

val sharedFrontendDir = file("shared-frontend")
val chessFrontendDir = file("chess-frontend")
val chessStaticDir = "src/main/resources/static/chess"

val npmCommand = if (System.getProperty("os.name").contains("Windows")) "npm.cmd" else "npm"

tasks.register<Exec>("npmSharedInstall") {
  description = "Install shared frontend dependencies"
  workingDir = sharedFrontendDir
  commandLine(npmCommand, "install")
  inputs.file("$sharedFrontendDir/package.json")
  outputs.dir("$sharedFrontendDir/node_modules")
}

tasks.register<Exec>("npmSharedBuild") {
  description = "Build shared frontend"
  workingDir = sharedFrontendDir
  commandLine(npmCommand, "run", "build")
  dependsOn("npmSharedInstall")
  inputs.dir("$sharedFrontendDir/src")
  inputs.file("$sharedFrontendDir/package.json")
  inputs.file("$sharedFrontendDir/tsconfig.json")
  inputs.file("$sharedFrontendDir/vite.config.ts")
  outputs.dir("$sharedFrontendDir/dist")
}

tasks.register<Exec>("npmChessInstall") {
  description = "Install chess frontend dependencies"
  workingDir = chessFrontendDir
  commandLine(npmCommand, "install")
  dependsOn("npmSharedBuild")
  inputs.file("$chessFrontendDir/package.json")
  inputs.dir("$sharedFrontendDir/dist")
  outputs.dir("$chessFrontendDir/node_modules")
}

tasks.register<Exec>("npmChessBuild") {
  description = "Build chess frontend"
  workingDir = chessFrontendDir
  commandLine(npmCommand, "run", "build")
  dependsOn("npmChessInstall")
  inputs.dir("$chessFrontendDir/src")
  inputs.file("$chessFrontendDir/package.json")
  inputs.file("$chessFrontendDir/tsconfig.json")
  inputs.file("$chessFrontendDir/vite.config.ts")
  inputs.dir("$sharedFrontendDir/dist")
  outputs.dir("$chessFrontendDir/dist")
}

tasks.register<Copy>("copyChessFrontend") {
  description = "Copy chess frontend to Spring static resources"
  dependsOn("npmChessBuild")
  from("$chessFrontendDir/dist")
  into(chessStaticDir)

  doFirst {
    delete(chessStaticDir)
  }
}

tasks.named("processResources") {
  dependsOn("copyChessFrontend")
}

tasks.named("clean") {
  doLast {
    delete(layout.projectDirectory.dir(chessStaticDir))
    delete(layout.projectDirectory.dir("$chessFrontendDir/dist"))
    delete(layout.projectDirectory.dir("$sharedFrontendDir/dist"))
  }
}