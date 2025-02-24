package org.jetbrains.bazel.languages.bazelquery.fixtures

import com.google.idea.testing.runfiles.Runfiles
import com.intellij.testFramework.ParsingTestCase
import org.jetbrains.bazel.languages.bazelquery.parser.BazelqueryParserDefinition
import kotlin.io.path.pathString

abstract class BazelqueryParsingTestCase(baseDir: String, val dumpTree: Boolean = false) :
  ParsingTestCase(baseDir, "bazelquery", BazelqueryParserDefinition()) {
  override fun getTestDataPath(): String = Runfiles.runfilesPath("/plugin-bazel/src/test/testData/bazelquery/parser/").pathString

  override fun doTest(p0: Boolean, p1: Boolean) {
    if (dumpTree) {
      dumpParseTree()
    } else {
      super.doTest(p0, p1)
    }
  }

  fun dumpParseTree() {
    val name = this.testName

    parseFile(name, this.loadFile(name + "." + this.myFileExt))

    fail(toParseTreeText(this.myFile, this.skipSpaces(), this.includeRanges()))
  }
}
