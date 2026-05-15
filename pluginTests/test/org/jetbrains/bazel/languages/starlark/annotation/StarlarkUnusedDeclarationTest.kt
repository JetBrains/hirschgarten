package org.jetbrains.bazel.languages.starlark.annotation

import org.jetbrains.bazel.languages.starlark.fixtures.StarlarkAnnotatorTestCase

class StarlarkUnusedDeclarationTest : StarlarkAnnotatorTestCase() {
  fun testUnusedDeclaration() {
    myFixture.configureByFile("UnusedDeclarationTestData.bzl")
    myFixture.checkHighlighting(false, false, true)
  }

  fun testUnusedComprehensionVariable() {
    myFixture.configureByFile("UnusedComprehensionVariableTestData.bzl")
    myFixture.checkHighlighting(false, false, true)
  }

  fun testUnusedForLoopVariable() {
    myFixture.configureByFile("UnusedForLoopVariableTestData.bzl")
    myFixture.checkHighlighting(false, false, true)
  }

  fun testUnusedReassignment() {
    myFixture.configureByFile("UnusedReassignmentTestData.bzl")
    myFixture.checkHighlighting(false, false, true)
  }
}
