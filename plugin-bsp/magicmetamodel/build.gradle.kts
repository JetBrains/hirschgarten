plugins {
  id("intellijbsp.kotlin-conventions")
  alias(libs.plugins.intellij)
}

dependencies {
  implementation(libs.bsp4j)
  testImplementation(libs.junitJupiter)
  testImplementation(libs.kotest)
  testImplementation(project(":test-utils"))
}

tasks {
  test {
    classpath -= classpath.filter { it.name.contains("kotlin-compiler-embeddable") }
  }
}

intellij {
  plugins.set(Platform.plugins)
}