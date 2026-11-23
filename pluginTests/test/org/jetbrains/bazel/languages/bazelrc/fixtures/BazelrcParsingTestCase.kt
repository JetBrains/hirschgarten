package org.jetbrains.bazel.languages.bazelrc.fixtures

import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.util.text.StringUtil
import com.intellij.platform.testFramework.core.FileComparisonFailedError
import com.intellij.platform.testFramework.junit5.codeInsight.fixture.ParsingTestFixture
import com.intellij.platform.testFramework.junit5.codeInsight.fixture.parsingFixture
import com.intellij.testFramework.junit5.fixture.projectFixture
import org.jetbrains.bazel.languages.bazelrc.parser.BazelrcParserDefinition
import org.jetbrains.bazel.test.framework.BazelPathManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.fail

abstract class BazelrcParsingTestCase(baseDir: String, val dumpTree: Boolean = false) {
  companion object {
    private val projectFixture = projectFixture()
  }

  private val parsing by parsingFixture(
    projectFixture,
    BazelPathManager.getTestFixture("bazelrc/parser") + "/" + baseDir,
    "bazelrc",
    definitions = arrayOf(BazelrcParserDefinition()),
  )

  protected fun doTest(checkResult: Boolean, ensureNoErrorElements: Boolean) {
    if (dumpTree) {
      dumpParseTree()
    } else {
      try {
        parsing.doTest(checkResult, ensureNoErrorElements)
      } catch (e: FileComparisonFailedError) {
        assertEquals(
          StringUtil.convertLineSeparators(e.expectedStringPresentation.trim()),
          StringUtil.convertLineSeparators(e.actualStringPresentation.trim()),
        )
      }
    }
  }

  fun dumpParseTree() {
    val name = parsing.testName

    val file = parsing.parseFile(name, parsing.loadFile(name + "." + parsing.fileExt))

    fail(
      ReadAction.computeBlocking<String, RuntimeException> {
        ParsingTestFixture.toParseTreeText(file, parsing.skipSpaces, parsing.includeRanges)
      },
    )
  }
}
