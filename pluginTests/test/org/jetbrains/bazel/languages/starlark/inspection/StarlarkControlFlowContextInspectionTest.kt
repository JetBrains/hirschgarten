package org.jetbrains.bazel.languages.starlark.inspection

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.jetbrains.bazel.languages.starlark.StarlarkBundle
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
internal class StarlarkControlFlowContextInspectionTest : BasePlatformTestCase() {
  private val descriptionBreak = StarlarkBundle.message("inspection.description.break.outside.for.loop")
  private val descriptionContinue = StarlarkBundle.message("inspection.description.continue.outside.for.loop")
  private val descriptionReturn = StarlarkBundle.message("inspection.description.return.outside.function")

  @Before
  fun beforeEach() {
    myFixture.enableInspections(StarlarkControlFlowContextInspection())
  }

  @Test
  fun `break outside a for loop should be highlighted`() {
    myFixture.configureByText(
      "file.bzl",
      """
        <error descr="$descriptionBreak">break</error>
        if True:
          <error descr="$descriptionBreak">break</error>
        def func():
          <error descr="$descriptionBreak">break</error>
      """.trimIndent()
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `break inside a for loop should not be highlighted`() {
    myFixture.configureByText(
      "file.bzl",
      """
        for i in [1, 2, 3]:
          break
        for i in ["a", "b", "c"]:
          if i == "b":
            break
      """.trimIndent()
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `continue outside a for loop should be highlighted`() {
    myFixture.configureByText(
      "file.bzl",
      """
        <error descr="$descriptionContinue">continue</error>
        if True:
          <error descr="$descriptionContinue">continue</error>
        def func():
          <error descr="$descriptionContinue">continue</error>
      """.trimIndent()
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `continue inside a for loop should not be highlighted`() {
    myFixture.configureByText(
      "file.bzl",
      """
        for i in [1, 2, 3]:
          continue
        for i in ["a", "b", "c"]:
          if i == "b":
            continue
      """.trimIndent()
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `return outside a function should be highlighted`() {
    myFixture.configureByText(
      "file.bzl",
      """
        <error descr="$descriptionReturn">return</error>
        if True:
          <error descr="$descriptionReturn">return</error>
      """.trimIndent()
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `return inside a function should not be highlighted`() {
    myFixture.configureByText(
      "file.bzl",
      """
        def func():
          return
        def func2():
          if True:
            return
      """.trimIndent()
    )
    myFixture.checkHighlighting(true, false, false)
  }
}
