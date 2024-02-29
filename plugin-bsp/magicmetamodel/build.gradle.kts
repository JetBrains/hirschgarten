plugins {
  id("intellijbsp.kotlin-conventions")
  alias(libs.plugins.intellij)
}

dependencies {
  implementation(project(":jps-compilation"))
  implementation(project(":protocol"))
  implementation(libs.bsp4j) {
    exclude(group = "com.google.guava", "guava")
  }
  implementation(project(":workspacemodel"))
  testImplementation(libs.junitJupiter)
  testImplementation(libs.kotest)
  testImplementation(project(":test-utils"))
  testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks {
  test {
    classpath -= classpath.filter { it.name.contains("kotlin-compiler-embeddable") }
  }
}

intellij {
  plugins.set(Platform.plugins)
}