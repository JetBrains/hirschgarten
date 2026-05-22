package org.jetbrains.bazel.languages.starlark.inspection

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.jetbrains.bazel.languages.starlark.StarlarkBundle
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4


@RunWith(JUnit4::class)
internal class StarlarkLoadPrivateSymbolInspectionTest : BasePlatformTestCase() {
  val description = StarlarkBundle.message("inspection.description.load.private.symbol")

  @Before
  fun beforeEach() {
    myFixture.enableInspections(StarlarkLoadPrivateSymbolInspection())
  }

  @Test
  fun `string load value with private symbol in BUILD file should be highlighted`() {
    myFixture.configureByText(
      "BUILD",
      """
        load("foo", <error descr="$description">"_bar"</error>)
      """.trimIndent()
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `named load value with private symbol in BUILD file should be highlighted`() {
    myFixture.configureByText(
      "BUILD",
      """
        load("foo", <error descr="$description">new_name="_bar"</error>)
      """.trimIndent()
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `string load value with private symbol in WORKSPACE file should be highlighted`() {
    myFixture.configureByText(
      "WORKSPACE",
      """
        load("foo", <error descr="$description">"_bar"</error>)
      """.trimIndent()
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `named load value with private symbol in WORKSPACE file should be highlighted`() {
    myFixture.configureByText(
      "WORKSPACE",
      """
        load("foo", <error descr="$description">new_name="_bar"</error>)
      """.trimIndent()
    )
    myFixture.checkHighlighting(true, false, false)
  }


  @Test
  fun `string load value with private symbol in bzl file should be highlighted`() {
    myFixture.configureByText(
      "file.bzl",
      """
        load("foo", <error descr="$description">"_bar"</error>)
      """.trimIndent()
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `named load value with private symbol in bzl file should be highlighted`() {
    myFixture.configureByText(
      "file.bzl",
      """
        load("foo", <error descr="$description">new_name="_bar"</error>)
      """.trimIndent()
    )
    myFixture.checkHighlighting(true, false, false)
  }


  @Test
  fun `filename load value with name started with underscore should not be highlighted`() {
    myFixture.configureByText(
      "BUILD",
      """
        load("_foo", "bar")
      """.trimIndent()
    )
    myFixture.checkHighlighting(true, false, false)
  }
}
