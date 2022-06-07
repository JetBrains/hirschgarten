plugins {
  id("intellijbsp.kotlin-conventions")
}

dependencies {
  implementation("ch.epfl.scala:bsp4j:2.0.0-M15")
  testImplementation("org.junit.jupiter:junit-jupiter:5.8.2")
  testImplementation("io.kotest:kotest-assertions-core:5.3.0")
  testImplementation(project(":test-utils"))
}
