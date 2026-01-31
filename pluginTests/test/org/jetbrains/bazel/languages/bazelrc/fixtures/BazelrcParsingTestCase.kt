package org.jetbrains.bazel.languages.bazelrc.fixtures

import com.intellij.platform.testFramework.core.FileComparisonFailedError
import com.intellij.testFramework.ParsingTestCase
import org.jetbrains.bazel.languages.bazelrc.parser.BazelrcParserDefinition
import org.jetbrains.bazel.test.framework.BazelPathManager

abstract class BazelrcParsingTestCase(baseDir: String, val dumpTree: Boolean = false) :
  ParsingTestCase(baseDir, "bazelrc", BazelrcParserDefinition()) {
  override fun getTestDataPath(): String =
    BazelPathManager.getTestFixture("bazelrc/parser")

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

  fun dumpParseTree() {
    val name = this.testName

    parseFile(name, this.loadFile(name + "." + this.myFileExt))

    fail(toParseTreeText(this.myFile, this.skipSpaces(), this.includeRanges()))
  }
}
