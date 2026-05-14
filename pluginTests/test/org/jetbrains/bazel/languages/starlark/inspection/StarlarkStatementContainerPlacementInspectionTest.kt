package org.jetbrains.bazel.languages.starlark.inspection

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.jetbrains.bazel.languages.starlark.StarlarkBundle
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
internal class StarlarkStatementContainerPlacementInspectionTest : BasePlatformTestCase() {
  val ifDescription = StarlarkBundle.message("inspection.description.if.placement")
  val ifDescriptionInBuild = StarlarkBundle.message("inspection.description.if.placement.build")
  val ifDescriptionInWorkspace = StarlarkBundle.message("inspection.description.if.placement.workspace")
  val forDescription = StarlarkBundle.message("inspection.description.for.placement")
  val forDescriptionInBuild = StarlarkBundle.message("inspection.description.for.placement.build")
  val forDescriptionInWorkspace = StarlarkBundle.message("inspection.description.for.placement.workspace")

  @Before
  fun beforeEach() {
    myFixture.enableInspections(StarlarkStatementContainerPlacementInspection())
  }

  @Test
  fun `top-level if statement in bzl file should be highlighted`() {
    myFixture.configureByText(
      "file.bzl",
      """
        a = 1
        <error descr="$ifDescription">if True:
          a += 1</error>
      """.trimIndent()
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `top-level if statement in build file should be highlighted`() {
    myFixture.configureByText(
      "BUILD",
      """
        a = 1
        <error descr="$ifDescriptionInBuild">if True:
          a += 1</error>
      """.trimIndent()
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `top-level if statement in workspace file should be highlighted`() {
    myFixture.configureByText(
      "WORKSPACE",
      """
        a = 1
        <error descr="$ifDescriptionInWorkspace">if True:
          a += 1</error>
      """.trimIndent()
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `top-level for statement in bzl file should be highlighted`() {
    myFixture.configureByText(
      "file.bzl",
      """
        a = 1
        <error descr="$forDescription">for i in range(10):
          a += 1</error>
      """.trimIndent()
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `top-level for statement in build file should be highlighted`() {
    myFixture.configureByText(
      "BUILD",
      """
        a = 1
        <error descr="$forDescriptionInBuild">for i in range(10):
          a += 1</error>
      """.trimIndent()
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `top-level for statement in workspace file should be highlighted`() {
    myFixture.configureByText(
      "WORKSPACE",
      """
        a = 1
        <error descr="$forDescriptionInWorkspace">for i in range(10):
          a += 1</error>
      """.trimIndent()
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `if statement inside a function should not be highlighted`() {
    myFixture.configureByText(
      "file.bzl",
      """
        def func(a):
          if a:
            return "yes"
          return "no"
        x = func(0)
      """.trimIndent()
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `if expression should not be highlighted`() {
    myFixture.configureByText(
      "file.bzl",
      """
        a = 1
        x = "yes" if a else "no"
      """.trimIndent()
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `for statement inside a function should not be highlighted`() {
    myFixture.configureByText(
      "file.bzl",
      """
        def func(a):
          for i in range(10):
            a += i
          return a
        x = func(0)
      """.trimIndent()
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `comprehension using for or if should not be highlighted`() {
    myFixture.configureByText(
      "file.bzl",
      """
        list = [x*x for x in range(5) if x%2 == 0]
      """.trimIndent()
    )
    myFixture.checkHighlighting(true, false, false)
  }
}
