import io.gitlab.arturbosch.detekt.Detekt
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

fun properties(key: String) = project.findProperty(key).toString()

plugins {
  // Kotlin
  kotlin("jvm")
  // detekt linter - read more: https://detekt.github.io/detekt/gradle.html
  id("io.gitlab.arturbosch.detekt")
  // ktlint linter - read more: https://github.com/JLLeitschuh/ktlint-gradle
  id("org.jlleitschuh.gradle.ktlint")
}

// Configure project's dependencies
repositories {
  mavenCentral()
}

kotlin {
  explicitApi()
}

// Configure detekt plugin.
// Read more: https://detekt.github.io/detekt/kotlindsl.html
detekt {
  autoCorrect = true
  ignoreFailures = false
  buildUponDefaultConfig = true
  config = files("$rootDir/detekt.yml")
  parallel = true
}

dependencies {
  // unfortunately I have not found a way to reuse the version from the build.gradle.kts in buildSrc
  implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.7.10")
  detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.21.0")
  implementation(kotlin("stdlib-jdk8"))
  implementation(kotlin("reflect"))
}

tasks {
  // Set the JVM compatibility versions
  properties("javaVersion").let {
    withType<JavaCompile> {
      sourceCompatibility = it
      targetCompatibility = it
    }
    withType<KotlinCompile> {
      kotlinOptions.jvmTarget = it
    }
    withType<Detekt> {
      jvmTarget = it
    }
  }

  properties("kotlinVersion").let {
    withType<KotlinCompile> {
      kotlinOptions.languageVersion = it
      kotlinOptions.apiVersion = it
    }
  }

  test {
    useJUnitPlatform()
    testLogging {
      events("PASSED", "SKIPPED", "FAILED")
    }
  }
}
