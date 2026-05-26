package org.jetbrains.bazel.test.framework

import org.jetbrains.bazel.test.compat.PluginTestsCompat
import java.nio.file.Path

object BazelPathManager {
  val pluginSourceRoot: Path = PluginTestsCompat.bazelPluginPath

  val testSourceRoot: Path by lazy {
    pluginSourceRoot.resolve("pluginTests")
  }

  val testDataRoot: Path by lazy {
    testSourceRoot
      .resolve("testData")
  }

  val testProjectsRoot: Path by lazy {
    testDataRoot
      .resolve("testProjects")
  }

  val integrationTestProjectRoot: Path by lazy {
    pluginSourceRoot.resolve("integrationTests")
  }

  val integrationTestDataRoot: Path by lazy {
    integrationTestProjectRoot.resolve("testData")
  }

  fun getTestFixture(path: String) = getTestFixturePath(path).toString()
  fun getTestFixturePath(path: String): Path = testDataRoot.resolve(path)
  fun getIntegrationTestFixturePath(path: String): Path = integrationTestDataRoot.resolve(path)
}
