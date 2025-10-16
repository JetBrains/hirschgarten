package org.jetbrains.bazel.languages.starlark.fixtures

import com.google.idea.testing.runfiles.Runfiles
import com.intellij.testFramework.ParsingTestCase
import org.jetbrains.bazel.languages.starlark.parser.StarlarkParserDefinition
import org.jetbrains.bazel.test.framework.BazelPathManager
import kotlin.io.path.pathString

abstract class StarlarkParsingTestCase(baseDir: String) : ParsingTestCase(baseDir, "bzl", StarlarkParserDefinition()) {
  override fun getTestDataPath(): String = BazelPathManager.getTestFixture("starlark/parser")
}
