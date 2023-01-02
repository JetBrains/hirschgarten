plugins {
  id("intellijbsp.kotlin-conventions")
}

dependencies {
  testImplementation(libs.junitJupiter)
  testImplementation(libs.ideProbeDriver)
  testImplementation(libs.ideProbeRobot)
  testImplementation(project(":probe-setup"))
  testRuntimeOnly(
    files(layout.buildDirectory.file("distributions/${Plugin.name}-${Plugin.version}.zip")) {
      builtBy(":buildPlugin")
    }
  )
}

tasks {
  processResources {
    from("$rootDir/build/distributions")
  }
}

repositories {
  maven("https://packages.jetbrains.team/maven/p/ij/intellij-dependencies")
}