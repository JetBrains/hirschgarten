package org.jetbrains.bazel.languages.projectview.fixtures

import com.intellij.platform.testFramework.junit5.codeInsight.fixture.parsingFixture
import com.intellij.testFramework.junit5.fixture.projectFixture
import org.jetbrains.bazel.languages.projectview.parser.ProjectViewParserDefinition
import org.jetbrains.bazel.test.framework.BazelPathManager

abstract class ProjectViewParsingTestCase(baseDir: String) {
  companion object {
    private val projectFixture = projectFixture()
  }

  private val parsing by parsingFixture(
    projectFixture,
    BazelPathManager.getTestFixture("projectview/parser") + "/" + baseDir,
    "bazelproject",
    definitions = arrayOf(ProjectViewParserDefinition()),
  )

  protected fun doTest(checkResult: Boolean, ensureNoErrorElements: Boolean = false) {
    parsing.doTest(checkResult, ensureNoErrorElements)
  }
}
