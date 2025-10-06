package org.jetbrains.bazel.languages.bazelquery.fixtures

import com.intellij.platform.testFramework.core.FileComparisonFailedError
import com.intellij.testFramework.ParsingTestCase
import org.jetbrains.bazel.languages.bazelquery.parser.BazelQueryFlagsParserDefinition
import org.jetbrains.bazel.test.framework.BazelPathManager

abstract class BazelQueryFlagsParsingTestCase(baseDir: String, val dumpTree: Boolean = false) :
  ParsingTestCase(baseDir, "bazelqueryflags", BazelQueryFlagsParserDefinition()) {
  override fun getTestDataPath(): String =
    BazelPathManager.getTestFixture("bazelquery/parser/flags")

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
