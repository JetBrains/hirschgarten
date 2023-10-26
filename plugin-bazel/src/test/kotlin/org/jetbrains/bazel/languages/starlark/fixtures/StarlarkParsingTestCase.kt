package org.jetbrains.bazel.languages.starlark.fixtures

import com.intellij.testFramework.ParsingTestCase
import org.jetbrains.bazel.languages.starlark.parser.StarlarkParserDefinition

abstract class StarlarkParsingTestCase(baseDir: String) : ParsingTestCase(baseDir, "bzl", StarlarkParserDefinition()) {
  override fun getTestDataPath() = "src/test/testData/starlark/parser/"
}
