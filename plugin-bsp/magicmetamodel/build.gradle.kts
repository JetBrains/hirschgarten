plugins {
  id("intellijbsp.kotlin-conventions")
}

dependencies {
  implementation(libs.bsp4j)
  testImplementation(libs.junitJupiter)
  testImplementation(libs.kotest)
  testImplementation(project(":test-utils"))
}
