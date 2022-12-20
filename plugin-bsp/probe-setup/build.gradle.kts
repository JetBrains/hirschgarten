plugins {
  scala
}

dependencies {
  implementation("org.scala-lang:scala-library:2.13.10")
  implementation("org.virtuslab.ideprobe:driver_2.13:0.50.0")
  implementation("org.virtuslab.ideprobe:robot-driver_2.13:0.50.0")
}

repositories {
  mavenCentral()
  maven("https://packages.jetbrains.team/maven/p/ij/intellij-dependencies")
}