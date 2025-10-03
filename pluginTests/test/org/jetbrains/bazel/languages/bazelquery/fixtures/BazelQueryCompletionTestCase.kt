package org.jetbrains.bazel.languages.bazelquery.fixtures

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.config.rootDir

abstract class BazelQueryCompletionTestCase : BasePlatformTestCase() {

  override fun setUp() {
    super.setUp()
    project.rootDir = myFixture.tempDirFixture.findOrCreateDir(".")
    project.isBazelProject = true
  }
}
