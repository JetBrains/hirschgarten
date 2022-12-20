plugins {
  id("intellijbsp.kotlin-conventions")
}

dependencies {
  testImplementation("org.junit.jupiter:junit-jupiter:5.8.2")
  testImplementation("org.virtuslab.ideprobe:driver_2.13:0.50.0")
  testImplementation("org.virtuslab.ideprobe:robot-driver_2.13:0.50.0")
  testImplementation(project(":probe-setup"))
  testRuntimeOnly(
    files(layout.buildDirectory.file("distributions/intellij-bsp-0.0.1-alpha.3.zip")) {
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