package org.jetbrains.bazel.languages.starlark.inspection

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.jetbrains.bazel.languages.starlark.StarlarkBundle
import org.jetbrains.bazel.project.BazelProjectFixtures.initializeBazelProject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class StarlarkFrozenLoadedValueMutationInspectionTest : BasePlatformTestCase() {
  private val xsDescription = StarlarkBundle.message("inspection.description.loaded.value.mutation", "xs")
  private val dataDescription = StarlarkBundle.message("inspection.description.loaded.value.mutation", "data")
  private val aliasDescription = StarlarkBundle.message("inspection.description.loaded.value.mutation", "alias")

  @Before
  fun beforeEach() {
    initializeBazelProject(project, myFixture.tempDirPath)
    myFixture.addFileToProject("MODULE.bazel", "")
    myFixture.addFileToProject("BUILD", "")
    myFixture.addFileToProject(
      "defs.bzl",
      """
      xs = []
      data = {}
      make_rule = rule(implementation = lambda ctx: [])
      """.trimIndent(),
    )
    myFixture.enableInspections(StarlarkFrozenLoadedValueMutationInspection())
  }

  @Test
  fun `mutating loaded symbol by method call should be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      load(":defs.bzl", "xs")

      <error descr="$xsDescription">xs.append</error>(1)
      """.trimIndent(),
    )

    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `mutating aliased loaded symbol by method call should be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      load(":defs.bzl", alias = "xs")

      <error descr="$aliasDescription">alias.append</error>(1)
      """.trimIndent(),
    )

    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `subscription assignment to loaded symbol should be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      load(":defs.bzl", "data")

      <error descr="$dataDescription">data["x"]</error> = 1
      """.trimIndent(),
    )

    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `subscription assignment to aliased loaded symbol should be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      load(":defs.bzl", alias = "data")

      <error descr="$aliasDescription">alias["x"]</error> = 1
      """.trimIndent(),
    )

    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `augmented assignment to loaded symbol should be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      load(":defs.bzl", "xs")

      <error descr="$xsDescription">xs</error> += [1]
      """.trimIndent(),
    )

    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `augmented assignment to aliased loaded symbol should be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      load(":defs.bzl", alias = "xs")

      <error descr="$aliasDescription">alias</error> += [1]
      """.trimIndent(),
    )

    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `calling loaded function should not be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      load(":defs.bzl", "make_rule")

      make_rule(name = "x")
      """.trimIndent(),
    )

    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `non mutating access to loaded symbol should not be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      load(":defs.bzl", "xs")

      value = xs[0]
      length = len(xs)
      """.trimIndent(),
    )

    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `mutation of local symbol should not be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      load(":defs.bzl", "xs")

      local = []
      local.append(1)
      """.trimIndent(),
    )

    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `mutation of local symbol shadowing loaded symbol should not be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
    load(":defs.bzl", "xs")
    
    def func():
      xs = []
      xs.append(1)
      
    func()
    """.trimIndent(),
    )

    myFixture.checkHighlighting(true, false, false)
  }
}
