package org.jetbrains.bazel.languages.bazelquery.fixtures

import com.google.idea.testing.runfiles.Runfiles
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.config.rootDir
import kotlin.io.path.pathString

abstract class BazelQueryCompletionTestCase : BasePlatformTestCase() {
  override fun getTestDataPath(): String = Runfiles.runfilesPath("/plugin-bazel/src/test/testData/bazelquery/completion/").pathString

  override fun setUp() {
    super.setUp()
    project.rootDir = myFixture.tempDirFixture.findOrCreateDir(".")
    project.isBazelProject = true
  }
}
