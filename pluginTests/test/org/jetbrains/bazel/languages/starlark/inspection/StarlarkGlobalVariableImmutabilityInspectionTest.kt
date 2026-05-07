package org.jetbrains.bazel.languages.starlark.inspection

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4


@RunWith(JUnit4::class)
internal class StarlarkGlobalVariableImmutabilityInspectionTest : BasePlatformTestCase() {
  @Before
  fun beforeEach() {
    myFixture.enableInspections(StarlarkGlobalVariableImmutabilityInspection())
  }

  @Test
  fun `global variable reassignment in bzl file should be highlighted`() {
    myFixture.configureByText(
      "file.bzl",
      """
        a = 5
        <error descr="Global variable is immutable. It cannot be reassigned.">a = 6</error>
      """.trimIndent()
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `global variable reassignment in module file should be highlighted`() {
    myFixture.configureByText(
      "MODULE.bazel",
      """
        a = 5
        <error descr="Global variable is immutable. It cannot be reassigned.">a = 6</error>
      """.trimIndent()
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `global variable reassignment in build file should not be highlighted`() {
    myFixture.configureByText(
      "BUILD",
      """
        a = 5
        a = 6
      """.trimIndent()
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `global variable reassignment in workspace file should not be highlighted`() {
    myFixture.configureByText(
      "WORKSPACE",
      """
        a = 5
        a = 6
      """.trimIndent()
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `local variable reassignment should not be highlighted`() {
    myFixture.configureByText(
      "file.bzl",
      """
        def func(l):
            a = l + 1
            a = 2
        func(1)
      """.trimIndent())
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `global variable overwrite should not be highlighted`() {
    myFixture.configureByText(
      "file.bzl",
      """
        a = 5
        def func(l):
            a = l + 1
        func(a)
      """.trimIndent()
    )
    myFixture.checkHighlighting(true, false, false)
  }
}
