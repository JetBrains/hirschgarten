package org.jetbrains.bazel.languages.starlark

class StarlarkParsingComplexTest : StarlarkParsingTestBase("complex") {
  fun testAspects() = doTest(false, true)
}
