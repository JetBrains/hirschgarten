package org.jetbrains.bazel.languages.projectview.fixtures

import com.google.idea.testing.runfiles.Runfiles
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import kotlin.io.path.pathString

abstract class ProjectViewAnnotatorTestCase : BasePlatformTestCase() {
  override fun getTestDataPath(): String = Runfiles.runfilesPath("/plugin-bazel/src/test/testData/projectview/annotation/").pathString
}
