package org.jetbrains.bazel.languages.bazelquery.fixtures

import com.intellij.platform.testFramework.core.FileComparisonFailedError
import com.intellij.psi.PsiFile
import com.intellij.testFramework.ParsingTestCase
import com.intellij.testFramework.TestDataFile
import org.jetbrains.bazel.languages.bazelquery.parser.BazelQueryParserDefinition
import org.jetbrains.bazel.test.framework.BazelPathManager
import java.io.IOException

abstract class BazelQueryParsingTestCase(val dumpTree: Boolean = false) :
  ParsingTestCase("", "bazelquery", BazelQueryParserDefinition()) {
  private var currentSubDir: String = ""

  override fun getTestDataPath(): String =
    BazelPathManager.getTestFixture("bazelquery/parser/")

  override fun doTest(p0: Boolean, p1: Boolean) {
    if (dumpTree) {
      dumpParseTree()
    } else {
      try {
        super.doTest(p0, p1)
      } catch (e: FileComparisonFailedError) {
        assertSameLines(e.expectedStringPresentation, e.actualStringPresentation)
      }
    }
  }

  @Throws(IOException::class)
  override fun loadFile(
    @TestDataFile name: String,
  ): String = loadFileDefault(myFullDataPath + currentSubDir, name)

  @Throws(IOException::class)
  override fun checkResult(
    @TestDataFile targetDataName: String,
    file: PsiFile,
  ) {
    checkResult(myFullDataPath + currentSubDir, targetDataName, file)
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
    val name = this.testName

    parseFile(name, this.loadFile(name + "." + this.myFileExt))

    fail(toParseTreeText(this.myFile, this.skipSpaces(), this.includeRanges()))
  }
}
