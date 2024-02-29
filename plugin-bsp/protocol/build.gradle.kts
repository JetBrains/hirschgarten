plugins {
  id("intellijbsp.kotlin-conventions")
}

dependencies {
  implementation(libs.bsp4j) {
    exclude(group = "com.google.guava", "guava")
  }
}
