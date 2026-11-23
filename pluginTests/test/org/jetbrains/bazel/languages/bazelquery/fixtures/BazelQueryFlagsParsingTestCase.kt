package org.jetbrains.bazel.languages.bazelquery.fixtures

import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.util.text.StringUtil
import com.intellij.platform.testFramework.core.FileComparisonFailedError
import com.intellij.platform.testFramework.junit5.codeInsight.fixture.ParsingTestFixture
import com.intellij.platform.testFramework.junit5.codeInsight.fixture.parsingFixture
import com.intellij.testFramework.junit5.fixture.projectFixture
import org.jetbrains.bazel.languages.bazelquery.parser.BazelQueryFlagsParserDefinition
import org.jetbrains.bazel.test.framework.BazelPathManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.fail

abstract class BazelQueryFlagsParsingTestCase(baseDir: String, val dumpTree: Boolean = false) {
  companion object {
    private val projectFixture = projectFixture()
  }

  private val parsing by parsingFixture(
    projectFixture,
    BazelPathManager.getTestFixture("bazelquery/parser/flags") + "/" + baseDir,
    "bazelqueryflags",
    definitions = arrayOf(BazelQueryFlagsParserDefinition()),
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
