package org.jetbrains.bazel.test.compat

import com.intellij.openapi.application.PathManager
import java.nio.file.Path

object PluginTestsCompat {
  private const val BAZEL_RELATIVE_PATH = "plugins/bazel"

  val bazelPluginPath: Path by lazy {
    Path.of(PathManager.getHomePath())
      .resolve(BAZEL_RELATIVE_PATH)
  }

  fun setupTestSuite(disposable: Disposable) {
    
  }
}
