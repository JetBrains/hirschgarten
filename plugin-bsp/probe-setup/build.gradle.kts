plugins {
  scala
}

dependencies {
  implementation(libs.scala)
  implementation(libs.ideProbeDriver)
  implementation(libs.ideProbeRobot)
}

repositories {
  mavenCentral()
  maven("https://packages.jetbrains.team/maven/p/ij/intellij-dependencies")
}