package org.jetbrains.bazel.languages.starlark.annotation

import org.jetbrains.bazel.languages.starlark.StarlarkBundle
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelCodeInsightFixture
import org.junit.jupiter.api.Test

@BazelTestApplication
class StarlarkLoadAnnotatorTest {
  private val myFixture by bazelCodeInsightFixture(testDataDir = "starlark/annotation")

  @Test
  fun testResolvedLoadedSymbolIsNotHighlighted() {
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

  @Test
  fun testUnresolvedLoadedSymbolIsHighlighted() {
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

  @Test
  fun testLoadedSymbolsFromTooLargeFileAreNotHighlightedAsUnresolved() {
    val description = StarlarkBundle.message("annotator.too.large.file")
    val largeFileContent = buildString {
      appendLine("value = []")
      repeat(100_000) { appendLine("generated_value_$it = []") }
    }
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
