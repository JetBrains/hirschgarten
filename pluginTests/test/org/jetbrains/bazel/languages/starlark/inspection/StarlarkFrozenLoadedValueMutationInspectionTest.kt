package org.jetbrains.bazel.languages.starlark.inspection

import org.jetbrains.bazel.languages.starlark.StarlarkBundle
import org.jetbrains.bazel.test.framework.BazelBasePlatformTestCase
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class StarlarkFrozenLoadedValueMutationInspectionTest : BazelBasePlatformTestCase() {
  private val xsDescription = StarlarkBundle.message("inspection.description.loaded.value.mutation", "xs")
  private val dataDescription = StarlarkBundle.message("inspection.description.loaded.value.mutation", "data")
  private val aliasDescription = StarlarkBundle.message("inspection.description.loaded.value.mutation", "alias")

  @Before
  fun beforeEach() {
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

  @Test
  fun `calling function from loaded struct field named append should not be highlighted`() {
    myFixture.addFileToProject(
      "helpers.bzl",
      """
      def append_to(target, value):
        target.append(value)
  
      helper = struct(
        append = append_to,
      )
      """.trimIndent(),
    )

    myFixture.configureByText(
      "test.bzl",
      """
      load(":helpers.bzl", "helper")
  
      result = []
      helper.append(result, "x")
      """.trimIndent(),
    )

    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `calling function from aliased loaded struct field named append should not be highlighted`() {
    myFixture.addFileToProject(
      "helpers.bzl",
      """
      def append_to(target, value):
        target.append(value)
  
      helper = struct(
        append = append_to,
      )
      """.trimIndent(),
    )

    myFixture.configureByText(
      "test.bzl",
      """
      load(":helpers.bzl", api = "helper")
  
      result = []
      api.append(result, "x")
      """.trimIndent(),
    )

    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `mutating loaded symbol from tuple unpacking with list literal value should be highlighted`() {
    myFixture.addFileToProject(
      "unpacking.bzl",
      """
      first, second = [[1], [2]]
      """.trimIndent(),
    )

    val description = StarlarkBundle.message("inspection.description.loaded.value.mutation", "first")

    myFixture.configureByText(
      "test.bzl",
      """
      load(":unpacking.bzl", "first")
  
      <error descr="$description">first.append</error>(3)
      """.trimIndent(),
    )

    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `mutating loaded symbol from nested unpacking should be highlighted`() {
    myFixture.addFileToProject(
      "unpacking.bzl",
      """
      first, (second, third) = [[1], [[2], [3]]]
      """.trimIndent(),
    )

    val description = StarlarkBundle.message("inspection.description.loaded.value.mutation", "third")

    myFixture.configureByText(
      "test.bzl",
      """
      load(":unpacking.bzl", "third")
  
      <error descr="$description">third.append</error>(4)
      """.trimIndent(),
    )

    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `mutating loaded parenthesized list literal should be highlighted`() {
    myFixture.addFileToProject(
      "collections.bzl",
      """
      items = ([])
      """.trimIndent(),
    )

    val description = StarlarkBundle.message("inspection.description.loaded.value.mutation", "items")

    myFixture.configureByText(
      "test.bzl",
      """
      load(":collections.bzl", "items")
  
      <error descr="$description">items.append</error>("x")
      """.trimIndent(),
    )

    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `mutating loaded symbol from parenthesized tuple unpacking should be highlighted`() {
    myFixture.addFileToProject(
      "unpacking.bzl",
      """
      first, second = ([1], [2])
      """.trimIndent(),
    )

    val description = StarlarkBundle.message("inspection.description.loaded.value.mutation", "first")

    myFixture.configureByText(
      "test.bzl",
      """
      load(":unpacking.bzl", "first")
  
      <error descr="$description">first.append</error>(3)
      """.trimIndent(),
    )

    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `mutating loaded symbol from list unpacking should be highlighted`() {
    myFixture.addFileToProject(
      "unpacking.bzl",
      """
      [first, second] = [[1], [2]]
      """.trimIndent(),
    )

    val description = StarlarkBundle.message("inspection.description.loaded.value.mutation", "first")

    myFixture.configureByText(
      "test.bzl",
      """
      load(":unpacking.bzl", "first")
  
      <error descr="$description">first.append</error>(3)
      """.trimIndent(),
    )

    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `mutating loaded symbol from implicit tuple rhs should be highlighted`() {
    myFixture.addFileToProject(
      "unpacking.bzl",
      """
      first, second = [1], [2]
      """.trimIndent(),
    )

    val description = StarlarkBundle.message("inspection.description.loaded.value.mutation", "first")

    myFixture.configureByText(
      "test.bzl",
      """
      load(":unpacking.bzl", "first")

      <error descr="$description">first.append</error>(3)
      """.trimIndent(),
    )

    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `method call on loaded symbol from unpacking with mismatched arity should not be highlighted`() {
    myFixture.addFileToProject(
      "unpacking.bzl",
      """
      first, second = [[1]]
      """.trimIndent(),
    )

    myFixture.configureByText(
      "test.bzl",
      """
      load(":unpacking.bzl", "first")
  
      first.append(2)
      """.trimIndent(),
    )

    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `method call on loaded symbol from dynamic assignment should not be highlighted`() {
    myFixture.addFileToProject(
      "dynamic.bzl",
      """
      def make_items():
        return []
  
      items = make_items()
      """.trimIndent(),
    )

    myFixture.configureByText(
      "test.bzl",
      """
      load(":dynamic.bzl", "items")
  
      items.append("x")
      """.trimIndent(),
    )

    myFixture.checkHighlighting(true, false, false)
  }
}
