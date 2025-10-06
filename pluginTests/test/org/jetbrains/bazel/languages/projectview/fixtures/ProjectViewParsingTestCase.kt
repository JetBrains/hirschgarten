package org.jetbrains.bazel.languages.projectview.fixtures

import com.google.idea.testing.runfiles.Runfiles
import com.intellij.testFramework.ParsingTestCase
import org.jetbrains.bazel.languages.projectview.parser.ProjectViewParserDefinition
import org.jetbrains.bazel.test.framework.BazelPathManager
import kotlin.io.path.pathString

abstract class ProjectViewParsingTestCase(baseDir: String) : ParsingTestCase(baseDir, "bazelproject", ProjectViewParserDefinition()) {
  override fun getTestDataPath(): String = BazelPathManager.getTestFixture("projectview/parser")
}
