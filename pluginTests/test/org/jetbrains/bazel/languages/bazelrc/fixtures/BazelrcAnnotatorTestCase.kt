package org.jetbrains.bazel.languages.bazelrc.fixtures

import com.google.idea.testing.runfiles.Runfiles
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.jetbrains.bazel.test.framework.BazelPathManager
import kotlin.io.path.pathString

abstract class BazelrcAnnotatorTestCase : BasePlatformTestCase() {
  override fun getTestDataPath(): String =
    BazelPathManager.getTestFixture("bazelrc/annotation")
}
