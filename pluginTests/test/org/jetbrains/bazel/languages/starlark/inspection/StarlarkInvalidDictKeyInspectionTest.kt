package org.jetbrains.bazel.languages.starlark.inspection

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.jetbrains.bazel.languages.starlark.StarlarkBundle
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class StarlarkInvalidDictKeyInspectionTest : BasePlatformTestCase() {
  private val description = StarlarkBundle.message("inspection.description.dict.key.not.hashable")

  @Before
  fun beforeEach() {
    myFixture.enableInspections(StarlarkInvalidDictKeyInspection())
  }

  @Test
  fun `list literal key should be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      d = {
        <error descr="$description">[1, 2]</error>: "value",
      }
      """.trimIndent(),
    )

    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `dict literal key should be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      d = {
        <error descr="$description">{"a": 1}</error>: "value",
      }
      """.trimIndent(),
    )

    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `tuple containing list literal key should be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      d = {
        <error descr="$description">([1],)</error>: "value",
      }
      """.trimIndent(),
    )

    myFixture.checkHighlighting(true, false, false)
  }


  @Test
  fun `nested tuple containing list literal key should be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      d = {
        <error descr="$description">(([],),)</error>: "value",
      }
      """.trimIndent(),
    )

    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `string and integer keys should not be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      d = {
        "a": 1,
        1: "a",
        True: "yes",
        None: "none",
      }
      """.trimIndent(),
    )

    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `tuple of hashable literals should not be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      d = {
        ("a", 1, True): "value",
      }
      """.trimIndent(),
    )

    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `nested tuple of hashable literals should not be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      d = {
        ((1, 2),): "value",
      }
      """.trimIndent(),
    )

    myFixture.checkHighlighting(true, false, false)
  }
}
