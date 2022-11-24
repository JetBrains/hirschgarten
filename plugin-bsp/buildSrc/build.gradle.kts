plugins {
  `kotlin-dsl`
}

repositories {
  gradlePluginPortal() // so that external plugins can be resolved in dependencies section
  mavenCentral()
}

dependencies {
  implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.7.21")
  implementation("io.gitlab.arturbosch.detekt:detekt-gradle-plugin:1.21.0")
  implementation("org.jlleitschuh.gradle:ktlint-gradle:10.3.0")
}
