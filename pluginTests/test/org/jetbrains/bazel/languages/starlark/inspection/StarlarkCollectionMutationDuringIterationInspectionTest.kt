package org.jetbrains.bazel.languages.starlark.inspection

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.jetbrains.bazel.languages.starlark.StarlarkBundle
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class StarlarkCollectionMutationDuringIterationInspectionTest : BasePlatformTestCase() {
  private val description = StarlarkBundle.message("inspection.description.collection.mutation.during.iteration")

  @Before
  fun beforeEach() {
    myFixture.enableInspections(StarlarkCollectionMutationDuringIterationInspection())
  }

  @Test
  fun `append to iterated collection should be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      xs = [1, 2, 3]

      for x in xs:
        <error descr="$description">xs.append</error>(x)
      """.trimIndent(),
    )

    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `subscription assignment to iterated collection should be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      xs = [1, 2, 3]

      for x in xs:
        <error descr="$description">xs[0]</error> = x
      """.trimIndent(),
    )

    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `augmented assignment to iterated collection should be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      xs = [1, 2, 3]

      for x in xs:
        <error descr="$description">xs</error> += [x]
      """.trimIndent(),
    )

    myFixture.checkHighlighting(true, false, false)
  }


  @Test
  fun `mutation of outer iterated collection inside nested for loop should be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      xs = [1, 2, 3]
      ys = [4, 5, 6]

      for x in xs:
        for y in ys:
          <error descr="$description">xs.append</error>(y)
      """.trimIndent(),
    )

    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `mutation inside list comprehension should be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      xs = [1, 2, 3]

      result = [<error descr="$description">xs.append</error>(x) for x in xs]
      """.trimIndent(),
    )

    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `mutation inside dict comprehension should be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      xs = [1, 2, 3]

      result = {x: <error descr="$description">xs.append</error>(x) for x in xs}
      """.trimIndent(),
    )

    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `mutation inside comprehension condition should be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      xs = [1, 2, 3]

      result = [x for x in xs if <error descr="$description">xs.append</error>(x)]
      """.trimIndent(),
    )

    myFixture.checkHighlighting(true, false, false)
  }


  @Test
  fun `mutation of first collection in comprehension with multiple iterated collections should be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      xs = [1, 2, 3]
      ys = [4, 5, 6]

      result = [<error descr="$description">xs.append</error>(x) for x in xs for y in ys]
      """.trimIndent(),
    )

    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `mutation of second collection in comprehension with multiple iterated collections should be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      xs = [1, 2, 3]
      ys = [4, 5, 6]

      result = [<error descr="$description">ys.append</error>(y) for x in xs for y in ys]
      """.trimIndent(),
    )

    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `mutations of multiple iterated collections in comprehension should be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      xs = [1, 2, 3]
      ys = [4, 5, 6]

      result = [
          (<error descr="$description">xs.append</error>(x), <error descr="$description">ys.append</error>(y))
          for x in xs
          for y in ys
      ]
      """.trimIndent(),
    )

    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `mutation of non iterated collection in comprehension with multiple iterated collections should not be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      xs = [1, 2, 3]
      ys = [4, 5, 6]
      result = []

      values = [result.append((x, y)) for x in xs for y in ys]
      """.trimIndent(),
    )

    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `mutation of iterated collection in condition after second for clause should be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      xs = [1, 2, 3]
      ys = [4, 5, 6]

      result = [(x, y) for x in xs for y in ys if <error descr="$description">ys.append</error>(y)]
      """.trimIndent(),
    )

    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `mutation of another collection should not be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      xs = [1, 2, 3]
      result = []

      for x in xs:
        result.append(x)
      """.trimIndent(),
    )

    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `mutation inside nested function should not be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      xs = [1, 2, 3]

      for x in xs:
        def later():
          xs.append(x)
      """.trimIndent(),
    )

    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `unknown method should not be treated as mutation`() {
    myFixture.configureByText(
      "test.bzl",
      """
      xs = [1, 2, 3]

      for x in xs:
        xs.custom(x)
      """.trimIndent(),
    )

    myFixture.checkHighlighting(true, false, false)
  }
}
