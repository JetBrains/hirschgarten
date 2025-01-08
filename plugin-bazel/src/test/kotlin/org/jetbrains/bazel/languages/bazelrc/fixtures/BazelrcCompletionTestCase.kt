package org.jetbrains.bazel.languages.bazelrc.fixtures

import com.google.idea.testing.runfiles.Runfiles
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import kotlin.io.path.pathString

abstract class BazelrcCompletionTestCase : BasePlatformTestCase() {
  override fun getTestDataPath(): String = Runfiles.runfilesPath("/plugin-bazel/src/test/testData/bazelrc/completion/").pathString
}
