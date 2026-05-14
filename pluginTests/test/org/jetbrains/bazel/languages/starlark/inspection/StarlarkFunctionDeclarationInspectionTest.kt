package org.jetbrains.bazel.languages.starlark.inspection

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.jetbrains.bazel.languages.starlark.StarlarkBundle
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4


@RunWith(JUnit4::class)
internal class StarlarkFunctionDeclarationInspectionTest : BasePlatformTestCase() {
  val descriptionBuild = StarlarkBundle.message("inspection.description.function.declaration.in.build.file")
  val descriptionModule = StarlarkBundle.message("inspection.description.function.declaration.in.module.file")
  val descriptionWorkspace = StarlarkBundle.message("inspection.description.function.declaration.in.workspace.file")

  @Before
  fun beforeEach() {
    myFixture.enableInspections(StarlarkFunctionDeclarationInspection())
  }

  @Test
  fun `function declaration in BUILD file should be highlighted`() {
    myFixture.configureByText(
      "BUILD",
      """
        <error descr="${descriptionBuild}">def func(x):
            return x + 1</error>
        a = func(1)
      """.trimIndent()
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `function declaration in WORKSPACE file should be highlighted`() {
    myFixture.configureByText(
      "WORKSPACE",
      """
        <error descr="${descriptionWorkspace}">def func(x):
            return x + 1</error>
        a = func(1)
      """.trimIndent()
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `function declaration in module file should be highlighted`() {
    myFixture.configureByText(
      "MODULE.bazel",
      """
        <error descr="${descriptionModule}">def func(x):
            return x + 1</error>
        a = func(1)
      """.trimIndent()
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `function declaration in bzl file should not be highlighted`() {
    myFixture.configureByText(
      "file.bzl",
      """
        def func(x):
            return x + 1
        a = func(1)
      """.trimIndent()
    )
    myFixture.checkHighlighting(true, false, false)
  }
}
