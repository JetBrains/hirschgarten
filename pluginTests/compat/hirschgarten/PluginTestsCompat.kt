package org.jetbrains.bazel.test.compat

import com.google.idea.testing.BlazeTestSystemProperties
import com.google.idea.testing.runfiles.Runfiles
import com.intellij.openapi.Disposable
import java.nio.file.Path

object PluginTestsCompat {
  const val isHirschgarten: Boolean = true

  val bazelPluginPath: Path by lazy {
    Runfiles.runfilesPath()
  }

  fun setupTestSuite(disposable: Disposable) {
    BlazeTestSystemProperties.configureSystemProperties(disposable)
  }
}
