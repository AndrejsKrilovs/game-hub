plugins {
  base
}

val chessFrontendDir = file("chess-frontend")
val chessBackendStaticDir = "chess-backend/src/main/resources/static/chess"
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

project(":chess-backend") {
  plugins.withId("org.springframework.boot") {
    tasks.named("processResources") {
      dependsOn(":copyFrontend")
    }
  }
}

tasks.named("clean") {
  doLast {
    delete(layout.projectDirectory.dir(chessBackendStaticDir))
  }
}