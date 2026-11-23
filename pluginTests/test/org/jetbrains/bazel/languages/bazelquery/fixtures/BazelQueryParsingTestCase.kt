package org.jetbrains.bazel.languages.bazelquery.fixtures

import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.util.text.StringUtil
import com.intellij.platform.testFramework.core.FileComparisonFailedError
import com.intellij.platform.testFramework.junit5.codeInsight.fixture.ParsingTestFixture
import com.intellij.platform.testFramework.junit5.codeInsight.fixture.parsingFixture
import com.intellij.psi.PsiFile
import com.intellij.testFramework.junit5.fixture.projectFixture
import org.jetbrains.bazel.languages.bazelquery.parser.BazelQueryParserDefinition
import org.jetbrains.bazel.test.framework.BazelPathManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.fail

abstract class BazelQueryParsingTestCase(val dumpTree: Boolean = false) {
  companion object {
    private val projectFixture = projectFixture()
  }

  private val parsing by parsingFixture(
    projectFixture,
    BazelPathManager.getTestFixture("bazelquery/parser/") + "/",
    "bazelquery",
    definitions = arrayOf(BazelQueryParserDefinition()),
  )

  private var currentSubDir: String = ""

  protected fun doTest(checkResult: Boolean, ensureNoErrorElements: Boolean) {
    if (dumpTree) {
      dumpParseTree()
    } else {
      try {
        doTestImpl(checkResult, ensureNoErrorElements)
      } catch (e: FileComparisonFailedError) {
        assertEquals(
          StringUtil.convertLineSeparators(e.expectedStringPresentation.trim()),
          StringUtil.convertLineSeparators(e.actualStringPresentation.trim()),
        )
      }
    }
  }

  private fun doTestImpl(checkResult: Boolean, ensureNoErrorElements: Boolean) {
    val name = parsing.testName
    val file = parseFile(name)
    if (checkResult) {
      parsing.checkResult(parsing.fullDataPath + currentSubDir, name, file)
      if (ensureNoErrorElements) {
        parsing.ensureNoErrorElements()
      }
    }
    else {
      ReadAction.computeBlocking<String, RuntimeException> {
        ParsingTestFixture.toParseTreeText(file, parsing.skipSpaces, parsing.includeRanges)
      }
    }
  }

  private fun parseFile(name: String): PsiFile {
    val text = ParsingTestFixture.loadFileDefault(parsing.fullDataPath + currentSubDir, name + "." + parsing.fileExt)
    return parsing.parseFile(name, text)
  }

  fun doTestWithDir(
    subDir: String,
    p0: Boolean,
    p1: Boolean,
  ) {
    currentSubDir = subDir
    doTest(p0, p1)
  }

  fun dumpParseTree() {
    val file = parseFile(parsing.testName)

    fail(
      ReadAction.computeBlocking<String, RuntimeException> {
        ParsingTestFixture.toParseTreeText(file, parsing.skipSpaces, parsing.includeRanges)
      },
    )
  }
}
