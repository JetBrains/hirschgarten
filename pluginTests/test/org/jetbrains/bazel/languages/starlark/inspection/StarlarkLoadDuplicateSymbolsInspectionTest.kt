package org.jetbrains.bazel.languages.starlark.inspection

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.jetbrains.bazel.languages.starlark.StarlarkBundle
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
internal class StarlarkLoadDuplicateSymbolsInspectionTest : BasePlatformTestCase() {
  private val description = StarlarkBundle.message("inspection.description.load.name.defined.more.than.once")

  @Before
  fun beforeEach() {
    myFixture.enableInspections(StarlarkLoadDuplicateSymbolsInspection())
  }

  @Test
  fun `duplicate local name in load should be highlighted`() {
    myFixture.configureByText(
      "example.bzl",
      """
        load("//pkg:file.bzl", "foo", <error descr="$description">foo</error> = "bar")
      """.trimIndent()
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `duplicate loaded name without alias should be highlighted`() {
    myFixture.configureByText(
      "example.bzl",
      """
        load("//pkg:file.bzl", "foo", <error descr="$description">"foo"</error>)
      """.trimIndent()
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `duplicate alias name in load should be highlighted`() {
    myFixture.configureByText(
      "example.bzl",
      """
        load("//pkg:file.bzl", foo = "bar", <error descr="$description">foo</error> = "baz")
      """.trimIndent()
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `same loaded symbol with different aliases should not be highlighted`() {
    myFixture.configureByText(
      "example.bzl",
      """
        load("//pkg:file.bzl", first = "foo", second = "foo")
      """.trimIndent()
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `duplicate loaded symbol with new alias should not be highlighted`() {
    myFixture.configureByText(
      "example.bzl",
      """
        load("//pkg:file.bzl", "foo", bar = "foo")
      """.trimIndent()
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `unique loaded names should not be highlighted`() {
    myFixture.configureByText(
      "example.bzl",
      """
        load("//pkg:file.bzl", "foo", bar = "baz")
      """.trimIndent()
    )
    myFixture.checkHighlighting(true, false, false)
  }
}
