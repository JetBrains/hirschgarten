package org.jetbrains.bazel.languages.starlark.parser

import com.intellij.testFramework.junit5.TestApplication
import org.jetbrains.bazel.languages.starlark.fixtures.StarlarkParsingTestCase
import org.junit.jupiter.api.Test

@TestApplication
class StarlarkParsingComplexTest : StarlarkParsingTestCase("complex") {
  @Test
  fun testAspects() = doTest(false, true)
}
