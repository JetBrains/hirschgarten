package org.jetbrains.bsp.bazel.languages.starlark

import com.intellij.testFramework.ParsingTestCase

abstract class StarlarkParsingTestBase(baseDir: String) : ParsingTestCase(baseDir, "bzl", StarlarkParserDefinition()) {
    override fun getTestDataPath() = "src/test/testData/starlark/parser/"
}