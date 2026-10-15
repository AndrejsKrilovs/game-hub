import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.TaskProvider

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
  compileOnly("org.projectlombok:lombok")
  implementation(project(":chess-backend"))
  implementation(project(":checkers-backend"))
  annotationProcessor("org.projectlombok:lombok")
  developmentOnly("org.springframework.boot:spring-boot-devtools")
  implementation("org.springframework.boot:spring-boot-starter-web")
  implementation("org.springframework.boot:spring-boot-starter-security")
}

val sharedFrontendDir = file("shared-frontend")
val mainFrontendDir = file("main-frontend")
val npmCommand = if (System.getProperty("os.name").contains("Windows")) "npm.cmd"
else "/Users/andrejs.krilovs/.nvm/versions/node/v24.15.0/bin/npm"

fun frontendSourceTree(dir: File) = fileTree(dir) {
  exclude("dist", "node_modules", ".vite")
}

fun registerFrontendModule(
  name: String,
  dir: File,
  dependsOnBuild: TaskProvider<Exec>? = null
): TaskProvider<Exec> {
  val install = tasks.register<Exec>("npm${name}Install") {
    description = "Installs libraries for registrated game"
    workingDir = dir
    commandLine(npmCommand, "install")
    dependsOnBuild?.let {
      dependsOn(it)
    }

    inputs.file(dir.resolve("package.json"))
    val packageLock = dir.resolve("package-lock.json")
    if (packageLock.exists()) {
      inputs.file(packageLock)
    }

    outputs.dir(dir.resolve("node_modules"))
  }

  val build = tasks.register<Exec>("npm${name}Build") {
    description = "Builds $name frontend"
    workingDir = dir
    dependsOn(install)

    doFirst {
      delete(dir.resolve("dist"))
    }

    commandLine(npmCommand, "run", "build")
    inputs.files(frontendSourceTree(dir))
    dependsOnBuild?.let {
      inputs.dir(sharedFrontendDir.resolve("dist"))
    }

    outputs.dir(dir.resolve("dist"))
  }

  return build
}

val sharedBuild = registerFrontendModule(
  name = "Shared",
  dir = sharedFrontendDir
)

val mainBuild = registerFrontendModule(
  name = "Main",
  dir = mainFrontendDir,
  dependsOnBuild = sharedBuild
)

// Фронтенды игр: каталог "<игра>-frontend", публикуется в static/<игра>
val gameFrontends = listOf("chess", "checkers")

val gameBuilds = gameFrontends.associateWith { game ->
  registerFrontendModule(
    name = game.replaceFirstChar { it.uppercase() },
    dir = file("$game-frontend"),
    dependsOnBuild = sharedBuild
  )
}

val syncFrontendStatic = tasks.register<Sync>("syncFrontendStatic") {
  description = "Syncs built frontend files to Spring static resources"

  dependsOn(mainBuild)
  dependsOn(gameBuilds.values)

  into(file("src/main/resources/static"))
  from(mainFrontendDir.resolve("dist")) {
    into("")
  }

  gameBuilds.keys.forEach { game ->
    from(file("$game-frontend/dist")) {
      into(game)
    }
  }
}

tasks.named("processResources") {
  dependsOn(syncFrontendStatic)
}

tasks.named<Delete>("clean") {
  delete(file("src/main/resources/static"))
  delete(mainFrontendDir.resolve("dist"))
  gameFrontends.forEach { delete(file("$it-frontend/dist")) }
  delete(sharedFrontendDir.resolve("dist"))
}