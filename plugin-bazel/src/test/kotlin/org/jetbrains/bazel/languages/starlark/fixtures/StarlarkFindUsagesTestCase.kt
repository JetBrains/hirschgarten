package org.jetbrains.bazel.languages.starlark.fixtures

import com.google.idea.testing.runfiles.Runfiles
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import kotlin.io.path.pathString

abstract class StarlarkFindUsagesTestCase : BasePlatformTestCase() {
  override fun getTestDataPath(): String = Runfiles.runfilesPath("/plugin-bazel/src/test/testData/starlark/findusages/").pathString
}
