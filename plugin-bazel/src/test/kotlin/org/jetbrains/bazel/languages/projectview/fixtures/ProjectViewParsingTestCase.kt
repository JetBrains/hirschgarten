package org.jetbrains.bazel.languages.projectview.fixtures

import com.google.idea.testing.runfiles.Runfiles
import com.intellij.testFramework.ParsingTestCase
import org.jetbrains.bazel.languages.projectview.parser.ProjectViewParserDefinition
import kotlin.io.path.pathString

abstract class ProjectViewParsingTestCase(baseDir: String) : ParsingTestCase(baseDir, "bazelproject", ProjectViewParserDefinition()) {
  override fun getTestDataPath(): String = Runfiles.runfilesPath("/plugin-bazel/src/test/testData/projectview/parser/").pathString
}
