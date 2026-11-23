package org.jetbrains.bazel.languages.starlark.fixtures

import com.intellij.platform.testFramework.junit5.codeInsight.fixture.parsingFixture
import com.intellij.testFramework.junit5.fixture.projectFixture
import org.jetbrains.bazel.languages.starlark.parser.StarlarkParserDefinition
import org.jetbrains.bazel.test.framework.BazelPathManager

abstract class StarlarkParsingTestCase(baseDir: String) {
  companion object {
    private val projectFixture = projectFixture()
  }

  private val parsing by parsingFixture(
    projectFixture,
    BazelPathManager.getTestFixture("starlark/parser") + "/" + baseDir,
    "bzl",
    definitions = arrayOf(StarlarkParserDefinition()),
  )

  protected fun doTest(checkResult: Boolean, ensureNoErrorElements: Boolean = false) {
    parsing.doTest(checkResult, ensureNoErrorElements)
  }
}
