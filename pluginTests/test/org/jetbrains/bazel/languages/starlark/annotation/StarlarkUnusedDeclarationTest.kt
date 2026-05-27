package org.jetbrains.bazel.languages.starlark.annotation

import org.jetbrains.bazel.languages.starlark.fixtures.StarlarkAnnotatorTestCase
import org.jetbrains.bazel.project.BazelProjectFixtures.initializeBazelProject

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

  fun testTopLevelFunctionLoadedFromAnotherFileIsNotUnused() {
    myFixture.addFileToProject("MODULE.bazel", "module(name = \"test\")")
    myFixture.addFileToProject("BUILD.bazel", "")
    myFixture.copyFileToProject("TopLevelFunctionLoadedFromAnotherFileTestDataConsumer.bzl", "consumer.bzl")
    myFixture.configureByFile("TopLevelFunctionLoadedFromAnotherFileTestData.bzl")
    myFixture.checkHighlighting(false, false, true)
  }

  fun testTopLevelVariableLoadedFromAnotherFileIsNotUnused() {
    myFixture.addFileToProject("MODULE.bazel", "module(name = \"test\")")
    myFixture.addFileToProject("BUILD.bazel", "")
    myFixture.copyFileToProject("TopLevelVariableLoadedFromAnotherFileTestDataConsumer.bzl", "consumer.bzl")
    myFixture.configureByFile("TopLevelVariableLoadedFromAnotherFileTestData.bzl")
    myFixture.checkHighlighting(false, false, true)
  }

  fun testPrivateFunctionNotLoadedIsStillUnused() {
    myFixture.addFileToProject("MODULE.bazel", "module(name = \"test\")")
    myFixture.addFileToProject("BUILD.bazel", "")
    myFixture.configureByFile("PrivateFunctionNotLoadedTestData.bzl")
    myFixture.checkHighlighting(false, false, true)
  }

  fun testPublicFunctionNotLoadedIsStillUnused() {
    myFixture.addFileToProject("MODULE.bazel", "module(name = \"test\")")
    myFixture.addFileToProject("BUILD.bazel", "")
    myFixture.configureByFile("PublicFunctionNotLoadedTestData.bzl")
    myFixture.checkHighlighting(false, false, true)
  }
}
