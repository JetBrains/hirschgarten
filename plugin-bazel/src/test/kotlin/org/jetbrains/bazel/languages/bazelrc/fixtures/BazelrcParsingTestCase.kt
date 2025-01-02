package org.jetbrains.bazel.languages.bazelrc.fixtures

import com.google.idea.testing.runfiles.Runfiles
import com.intellij.platform.testFramework.core.FileComparisonFailedError
import com.intellij.testFramework.ParsingTestCase
import org.jetbrains.bazel.languages.bazelrc.parser.BazelrcParserDefinition
import kotlin.io.path.pathString

abstract class BazelrcParsingTestCase(baseDir: String, val dumpTree: Boolean = false) :
  ParsingTestCase(baseDir, "bazelrc", BazelrcParserDefinition()) {
  override fun getTestDataPath(): String = Runfiles.runfilesPath("/plugin-bazel/src/test/testData/bazelrc/parser/").pathString

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
