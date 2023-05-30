plugins {
  `kotlin-dsl`
}

repositories {
  gradlePluginPortal() // so that external plugins can be resolved in dependencies section
  mavenCentral()
}

dependencies {
  implementation(libs.kotlinGradle)
  implementation(libs.detektGradle)
}
