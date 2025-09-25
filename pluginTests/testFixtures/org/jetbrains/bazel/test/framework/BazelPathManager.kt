package org.jetbrains.bazel.test.framework

import com.intellij.openapi.application.PathManager
import java.nio.file.Path
import kotlin.io.path.absolutePathString

object BazelPathManager {
  private const val BAZEL_RELATIVE_PATH = "plugins/bazel"

  val testSourceRoot: Path by lazy {
    PathManager.getHomeDir()
      .resolve(BAZEL_RELATIVE_PATH)
      .resolve("pluginTests")
  }

  val testDataRoot: Path by lazy {
    testSourceRoot
      .resolve("testData")
  }

  fun getTestFixture(path: String) = getTestFixturePath(path).absolutePathString()
  fun getTestFixturePath(path: String): Path = testDataRoot.resolve(path)
}
