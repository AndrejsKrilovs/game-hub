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
val npmCommand = if (System.getProperty("os.name").contains("Windows")) "npm.cmd" else "npm"

fun frontendSourceTree(dir: File) = fileTree(dir) {
  exclude("dist", "node_modules", ".vite")
}

fun registerFrontendModule(name: String, dir: File,
  dependsOnBuild: TaskProvider<*>? = null,
  staticSubdir: String? = null
): TaskProvider<Exec> {
  val install = tasks.register<Exec>("npm${name}Install") {
    description = "Installs libraries for registrated game"
    workingDir = dir
    commandLine(npmCommand, "install")
    dependsOnBuild?.let {
      dependsOn(it)
    }

    inputs.file(dir.resolve("package.json"))
    outputs.dir(dir.resolve("node_modules"))
  }

  val build = tasks.register<Exec>("npm${name}Build") {
    description = "Builds registered game"
    workingDir = dir
    commandLine(npmCommand, "run", "build")
    dependsOn(install)
    inputs.files(frontendSourceTree(dir))
    dependsOnBuild?.let {
      inputs.dir(sharedFrontendDir.resolve("dist"))
    }

    outputs.dir(dir.resolve("dist"))
  }

  staticSubdir?.let { sub ->
    val copy = tasks.register<Copy>("copy${name}Frontend") {
      description = "Copy game frontend to main directory"
      dependsOn(build)
      from(dir.resolve("dist"))
      into("src/main/resources/static/$sub")
      doFirst {
        delete("src/main/resources/static/$sub")
      }
    }
    tasks.named("processResources") { dependsOn(copy) }
  }

  return build
}

val sharedBuild = registerFrontendModule("Shared", sharedFrontendDir)
registerFrontendModule("Chess", chessFrontendDir, dependsOnBuild = sharedBuild, staticSubdir = "chess")

tasks.named("clean") {
  doLast {
    delete(file("src/main/resources/static"))
    delete(chessFrontendDir.resolve("dist"))
    delete(sharedFrontendDir.resolve("dist"))
  }
}