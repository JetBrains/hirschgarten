package org.jetbrains.bazel.languages.starlark.annotation

import com.intellij.codeInsight.daemon.SyntheticPsiFileSupport
import org.jetbrains.bazel.languages.starlark.fixtures.StarlarkAnnotatorTestCase
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.junit.jupiter.api.Test

@BazelTestApplication
class StarlarkUnusedDeclarationTest : StarlarkAnnotatorTestCase() {

  @Test
  fun testUnusedDeclaration() {
    myFixture.configureByFile("UnusedDeclarationTestData.bzl")
    myFixture.checkHighlighting(false, false, true)
  }

  @Test
  fun testUnusedComprehensionVariable() {
    myFixture.configureByFile("UnusedComprehensionVariableTestData.bzl")
    myFixture.checkHighlighting(false, false, true)
  }

  @Test
  fun testUnusedForLoopVariable() {
    myFixture.configureByFile("UnusedForLoopVariableTestData.bzl")
    myFixture.checkHighlighting(false, false, true)
  }

  @Test
  fun testUnusedReassignment() {
    myFixture.configureByFile("UnusedReassignmentTestData.bzl")
    myFixture.checkHighlighting(false, false, true)
  }

  @Test
  fun testTopLevelFunctionLoadedFromAnotherFileIsNotUnused() {
    myFixture.addFileToProject("MODULE.bazel", "module(name = \"test\")")
    myFixture.addFileToProject("BUILD.bazel", "")
    myFixture.copyFileToProject("TopLevelFunctionLoadedFromAnotherFileTestDataConsumer.bzl", "consumer.bzl")
    myFixture.configureByFile("TopLevelFunctionLoadedFromAnotherFileTestData.bzl")
    myFixture.checkHighlighting(false, false, true)
  }

  @Test
  fun testTopLevelVariableLoadedFromAnotherFileIsNotUnused() {
    myFixture.addFileToProject("MODULE.bazel", "module(name = \"test\")")
    myFixture.addFileToProject("BUILD.bazel", "")
    myFixture.copyFileToProject("TopLevelVariableLoadedFromAnotherFileTestDataConsumer.bzl", "consumer.bzl")
    myFixture.configureByFile("TopLevelVariableLoadedFromAnotherFileTestData.bzl")
    myFixture.checkHighlighting(false, false, true)
  }

  @Test
  fun testPrivateFunctionNotLoadedIsStillUnused() {
    myFixture.addFileToProject("MODULE.bazel", "module(name = \"test\")")
    myFixture.addFileToProject("BUILD.bazel", "")
    myFixture.configureByFile("PrivateFunctionNotLoadedTestData.bzl")
    myFixture.checkHighlighting(false, false, true)
  }

  @Test
  fun testPublicFunctionNotLoadedIsStillUnused() {
    myFixture.addFileToProject("MODULE.bazel", "module(name = \"test\")")
    myFixture.addFileToProject("BUILD.bazel", "")
    myFixture.configureByFile("PublicFunctionNotLoadedTestData.bzl")
    myFixture.checkHighlighting(false, false, true)
  }

  @Test
  fun testPublicDeclarationsInSyntheticFilesAreNotUnused() {
    val psiFile = myFixture.configureByFile("UnusedPublicDeclarationSyntheticFileTestData.bzl")
    SyntheticPsiFileSupport.markFile(psiFile.virtualFile)
    myFixture.checkHighlighting(false, false, true)
  }


  @Test
  fun testLocalDeclarationsInSyntheticFilesAreUnused() {
    val psiFile = myFixture.configureByFile("UnusedLocalDeclarationSyntheticFileTestData.bzl")
    SyntheticPsiFileSupport.markFile(psiFile.virtualFile)
    myFixture.checkHighlighting(false, false, true)
  }
}
