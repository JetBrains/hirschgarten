package org.jetbrains.bazel.languages.starlark.annotation

import org.jetbrains.bazel.languages.starlark.StarlarkBundle
import org.jetbrains.bazel.languages.starlark.fixtures.StarlarkAnnotatorTestCase

class StarlarkLoadAnnotatorTest : StarlarkAnnotatorTestCase() {
  fun testResolvedLoadedSymbolIsNotHighlighted() {
    myFixture.addFileToProject("MODULE.bazel", "")
    myFixture.addFileToProject("BUILD.bazel", "")
    myFixture.addFileToProject(
      "defs.bzl",
      """
        value = []
      """.trimIndent(),
    )
    myFixture.configureByText(
      "test.bzl",
      """
        load(":defs.bzl", "value")
      """.trimIndent(),
    )

    myFixture.checkHighlighting(true, true, true)
  }

  fun testUnresolvedLoadedSymbolIsHighlighted() {
    myFixture.addFileToProject("MODULE.bazel", "")
    myFixture.addFileToProject("BUILD.bazel", "")
    myFixture.addFileToProject(
      "defs.bzl",
      """
        value = []
      """.trimIndent(),
    )
    myFixture.configureByText(
      "test.bzl",
      """
        load(":defs.bzl", <error descr="Unresolved reference: \"missing\"">"missing"</error>)      
      """.trimIndent(),
    )

    myFixture.checkHighlighting(true, true, true)
  }

  fun testLoadedSymbolsFromTooLargeFileAreNotHighlightedAsUnresolved() {
    val description = StarlarkBundle.message("annotator.too.large.file")
    val largeFileContent = buildString {
      appendLine("value = []")
      repeat(100_000) { appendLine("generated_value_$it = []") }
    }
    myFixture.addFileToProject("MODULE.bazel", "")
    myFixture.addFileToProject("BUILD.bazel", "")
    myFixture.addFileToProject("large_defs.bzl", largeFileContent)
    myFixture.configureByText(
      "test.bzl",
      """
        load(<info descr="$description">":large_defs.bzl"</info>, "value", "missing")
      """.trimIndent(),
    )

    myFixture.checkHighlighting(true, true, true)
  }
}
