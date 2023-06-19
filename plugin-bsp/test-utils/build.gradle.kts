plugins {
  id("intellijbsp.kotlin-conventions")
}

dependencies {
  implementation(libs.bsp4j)
  implementation(libs.junitJupiter)
  implementation(libs.kotest)
}

intellij {
  plugins.set(Platform.plugins)
}
