package org.jetbrains.bazel.languages.starlark.annotation

import org.jetbrains.bazel.languages.starlark.fixtures.StarlarkAnnotatorTestCase

class StarlarkUnusedDeclarationTest : StarlarkAnnotatorTestCase() {
  fun testUnusedDeclaration() {
    myFixture.configureByFile("UnusedDeclarationTestData.bzl")
    myFixture.checkHighlighting(false, false, true)
  }

  // https://youtrack.jetbrains.com/issue/BAZEL-3039
  fun testUnusedMethodDeclaration() {
    myFixture.configureByText(
      "test.bzl",
      """
        def _compiler_target_actual(actual):
           return "@rules_kotlin_maven//:%s" % actual

        def <weak_warning descr="Function \"kt_define_compiler_targets\" is never used">kt_define_compiler_targets</weak_warning>():
           for name, actual in _COMPILER_TARGETS:
               native.alias(
                   name = name,
                   actual = _compiler_target_actual(actual),
               )
      """.trimIndent(),
    )
    myFixture.checkHighlighting(false, false, true)
  }
}
