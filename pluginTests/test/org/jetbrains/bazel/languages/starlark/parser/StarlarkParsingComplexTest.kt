package org.jetbrains.bazel.languages.starlark.parser

import org.jetbrains.bazel.languages.starlark.fixtures.StarlarkParsingTestCase

class StarlarkParsingComplexTest : StarlarkParsingTestCase("complex") {
  fun testAspects() = doTest(false, true)
}
