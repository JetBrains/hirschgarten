package org.jetbrains.bazel.languages.starlark.annotation

import com.intellij.codeInsight.daemon.SyntheticPsiFileSupport
import com.intellij.lang.annotation.HighlightSeverity
import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelCodeInsightFixture
import org.junit.jupiter.api.Test

@BazelTestApplication
class StarlarkUnusedDeclarationTest {
  private val myFixture by bazelCodeInsightFixture(testDataDir = "starlark/annotation")

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

  @Test
  fun testAugmentedAssignmentIsARead() {
    myFixture.configureByText(
      "test.bzl",
      """
      def f():
          x = 1
          x += 1

      f()
      """.trimIndent(),
    )
    myFixture.checkHighlighting(false, false, true)
  }

  @Test
  fun testUnusedVariableWarningGoesAwayWhenTheTargetBecomesQualified() {
    myFixture.configureByText(
      "test.bzl",
      """
      def f():
          foo = struct()
          <caret>x = 1
          return foo

      f()
      """.trimIndent(),
    )
    weakWarnings() shouldBe listOf("Variable \"x\" is never used")
    myFixture.type("foo.")
    weakWarnings() shouldBe emptyList()
  }

  private fun weakWarnings(): List<String?> =
    myFixture.doHighlighting(HighlightSeverity.WEAK_WARNING).map { it.description }

  @Test
  fun testQualifiedAssignmentIsNotUnusedVariable() {
    myFixture.configureByText(
      "test.bzl",
      """
      def f():
          foo = struct()
          foo.bar = 1

      f()
      """.trimIndent(),
    )
    myFixture.checkHighlighting(false, false, true)
  }
}
