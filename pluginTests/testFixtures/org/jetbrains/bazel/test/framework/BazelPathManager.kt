package org.jetbrains.bazel.test.framework

import org.jetbrains.bazel.test.compat.PluginTestsCompat
import java.nio.file.Path
import kotlin.io.path.absolutePathString

object BazelPathManager {
  val pluginSourceRoot: Path by lazy {
    //Path.of(PathManager.getHomePath())
    PluginTestsCompat.bazelPluginPath
  }

  val testSourceRoot: Path by lazy {
    pluginSourceRoot.resolve("pluginTests")
  }

  val testDataRoot: Path by lazy {
    testSourceRoot
      .resolve("testData")
  }

  fun getTestFixture(path: String) = getTestFixturePath(path).absolutePathString()
  fun getTestFixturePath(path: String): Path = testDataRoot.resolve(path)
}
