package org.jetbrains.bazel.test.framework

import com.intellij.openapi.application.PathManager
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
    testSourceRoot
      .resolve("testProjects")
  }

  val ultimateJvmRulesRoot: Path by lazy {
    PathManager
      .getHomeDir()
      .resolve("community/build/jvm-rules")
  }

  fun getTestFixture(path: String) = getTestFixturePath(path).toString()
  fun getTestFixturePath(path: String): Path = testDataRoot.resolve(path)
}
