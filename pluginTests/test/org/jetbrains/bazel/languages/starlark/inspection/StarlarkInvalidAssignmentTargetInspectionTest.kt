package org.jetbrains.bazel.languages.starlark.inspection

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.jetbrains.bazel.languages.starlark.StarlarkBundle
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class StarlarkInvalidAssignmentTargetInspectionTest : BasePlatformTestCase() {
  private val invalid = StarlarkBundle.message("inspection.description.assignment.target.invalid", "1")
  private val augmentedSeq = StarlarkBundle.message("inspection.description.assignment.target.augmented.sequence")
  private val invalidCall = StarlarkBundle.message("inspection.description.assignment.target.invalid", "f()")
  private val invalidBinary = StarlarkBundle.message("inspection.description.assignment.target.invalid", "a + b")

  @Before
  fun beforeEach() {
    myFixture.enableInspections(StarlarkInvalidAssignmentTargetInspection())
  }

  @Test
  fun `literal on lhs should be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      <error descr="$invalid">1</error> = x
      """.trimIndent(),
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `call on lhs should be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      <error descr="$invalidCall">f()</error> = 1
      """.trimIndent(),
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `binary expr on lhs should be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      <error descr="$invalidBinary">a + b</error> = 1
      """.trimIndent(),
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `nested sequence with invalid element should be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      [a, <error descr="$invalid">1</error>] = [1, 2]
      """.trimIndent(),
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `identifier lhs should not be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      x = 1
      """.trimIndent(),
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `index lhs should not be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      a = [1]
      a[0] = 2
      """.trimIndent(),
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `dot lhs should not be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      x.f = 1
      """.trimIndent(),
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `augmented assignment on sequence lhs should be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      <error descr="$augmentedSeq">[a, b]</error> += [1, 2]
      """.trimIndent(),
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `augmented assignment on identifier lhs should not be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      x += 1
      y += [1]
      """.trimIndent(),
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `for loop invalid target should be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      for <error descr="$invalid">1</error> in [1,2]:
        pass
      """.trimIndent(),
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `for loop unpacking target should not be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      for [a, b] in [[1,2], [3,4]]:
        pass
      """.trimIndent(),
    )
    myFixture.checkHighlighting(true, false, false)
  }
}
