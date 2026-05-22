package org.jetbrains.bazel.languages.bazelquery.fixtures

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.jetbrains.bazel.project.BazelProjectFixtures.initializeBazelProject

abstract class BazelQueryCompletionTestCase : BasePlatformTestCase() {

  override fun setUp() {
    super.setUp()
    initializeBazelProject(project, myFixture.tempDirPath)
  }
}
