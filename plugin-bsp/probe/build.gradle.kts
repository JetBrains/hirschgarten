plugins {
  id("intellijbsp.kotlin-conventions")
  id("org.gradle.test-retry") version "1.5.2"
}

dependencies {
  testImplementation(libs.junitJupiter)
  testImplementation(libs.ideProbeDriver)
  testImplementation(libs.ideProbeRobot)
  testImplementation(project(":probe-setup"))
  testRuntimeOnly(
    files(layout.buildDirectory.file("distributions/${Plugin.name}-${Plugin.version}.zip")) {
      builtBy(":buildPlugin")
    }
  )
}

tasks {
  processResources {
    from("$rootDir/build/distributions")
  }
  test {
    // configure retry for :probe:test task
    retry {
      maxRetries.set(3) // Number of retries when a test fails
      maxFailures.set(2) // Number of failed tests allowed before stopping retries
      failOnPassedAfterRetry.set(false) // If true, the build will fail if tests pass after a retry
      filter {
        includeClasses.add("*SingleProbeTests")
      }
    }
  }
}

repositories {
  maven("https://packages.jetbrains.team/maven/p/ij/intellij-dependencies")
}