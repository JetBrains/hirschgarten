plugins {
  id("intellijbsp.kotlin-conventions")
}

dependencies {
  implementation(libs.bsp4j) {
    exclude(group = "com.google.guava", "guava")
  }
  implementation(libs.junitJupiter)
  implementation(libs.kotest)
}

intellij {
  plugins.set(Platform.plugins)
}
