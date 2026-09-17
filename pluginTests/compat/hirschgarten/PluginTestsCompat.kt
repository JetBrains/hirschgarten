package org.jetbrains.bazel.test.compat

import com.intellij.openapi.Disposable
import java.nio.file.Path

object PluginTestsCompat {
  const val isHirschgarten: Boolean = true

  // Bazel runfiles live at $TEST_SRCDIR/$TEST_WORKSPACE. toRealPath() follows the runfile symlink back to the checkout.
  val bazelPluginPath: Path by lazy {
    Path.of(System.getenv("TEST_SRCDIR"), System.getenv("TEST_WORKSPACE"), "pluginTests/testData/projectview/empty.bazelproject")
      .toRealPath()
      .parent.parent.parent.parent
  }

  fun setupTestSuite(disposable: Disposable) {
  }
}
