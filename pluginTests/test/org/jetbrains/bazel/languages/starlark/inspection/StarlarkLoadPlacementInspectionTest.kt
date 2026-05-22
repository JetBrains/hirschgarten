package org.jetbrains.bazel.languages.starlark.inspection

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.jetbrains.bazel.languages.starlark.StarlarkBundle
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4


@RunWith(JUnit4::class)
internal class StarlarkLoadPlacementInspectionTest : BasePlatformTestCase() {
  val descriptionModule = StarlarkBundle.message("inspection.description.load.not.allowed.in.module.bazel")
  val descriptionTopLevel = StarlarkBundle.message("inspection.description.load.not.at.top.level")
  val descriptionAfter = StarlarkBundle.message("inspection.description.load.after.statement")

  @Before
  fun beforeEach() {
    myFixture.enableInspections(StarlarkLoadPlacementInspection())
  }

  @Test
  fun `load statement in module file should be highlighted`() {
    myFixture.configureByText(
      "MODULE.bazel",
      """
        <error descr="$descriptionModule">load("foo", "bar")</error>
      """.trimIndent()
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `load statement after other statement in bzl file should be highlighted`() {
    myFixture.configureByText(
      "example.bzl",
      """
        a = 5
        <error descr="$descriptionAfter">load("foo", "bar")</error>
      """.trimIndent()
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `load statement after other statement in build file should not be highlighted`() {
    myFixture.configureByText(
      "BUILD",
      """
        a = 5
        load("foo", "bar")
      """.trimIndent()
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `load statement after other statement in workspace file should not be highlighted`() {
    myFixture.configureByText(
      "WORKSPACE",
      """
        a = 5
        load("foo", "bar")
      """.trimIndent()
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `load statement not at the top level should be highlighted`() {
    myFixture.configureByText(
      "example.bzl",
      """
        def func(l):
            <error descr="$descriptionTopLevel">load("foo", "bar")</error>
            return l+2
        func(1)
      """.trimIndent()
    )
    myFixture.checkHighlighting(true, false, false)
  }
}
