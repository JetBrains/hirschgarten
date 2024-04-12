plugins {
  `kotlin-dsl`
}

repositories {
  maven("https://oss.sonatype.org/content/repositories/snapshots/")
  gradlePluginPortal() // so that external plugins can be resolved in dependencies section
  mavenCentral()
}

dependencies {
  implementation(libs.kotlinGradle)
  implementation(libs.detektGradle)
}
