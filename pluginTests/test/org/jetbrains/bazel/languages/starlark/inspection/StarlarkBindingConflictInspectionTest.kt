package org.jetbrains.bazel.languages.starlark.inspection

import com.intellij.psi.NavigatablePsiElement
import com.intellij.psi.PsiElement
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import io.kotest.matchers.collections.shouldContainAll
import org.jetbrains.bazel.languages.starlark.inspection.StarlarkBindingConflictInspection.NavigateToConflictingDeclarationAction
import org.jetbrains.bazel.languages.starlark.StarlarkBundle
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4


@RunWith(JUnit4::class)
internal class StarlarkBindingConflictInspectionTest : BasePlatformTestCase() {
  private val descriptionCrossKindGlobal = StarlarkBundle.message("inspection.description.binding.conflict.cross.kind", "x", "global")
  private val descriptionCrossKindFileLocal = StarlarkBundle.message("inspection.description.binding.conflict.cross.kind", "x", "file-local")
  private val descriptionSameKind = StarlarkBundle.message("inspection.description.binding.conflict.same.kind", "x")
  private val descriptionOrig = StarlarkBundle.message("inspection.description.binding.conflict.original", "x")
  private val quickFixText = StarlarkBundle.message("inspection.quickfix.show.conflicting.declarations.text", "x")

  @Before
  fun beforeEach() {
    myFixture.enableInspections(StarlarkBindingConflictInspection())
  }

  @Test
  fun `load then global assignment should highlight cross-kind conflict`() {
    myFixture.configureByText(
      "test.bzl",
      """
      load("//:a.bzl", <error descr="$descriptionCrossKindFileLocal">"x"</error>)
      <error descr="$descriptionCrossKindGlobal">x = 1</error>
      """.trimIndent(),
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `global assignment then load should highlight cross-kind conflict`() {
    myFixture.configureByText(
      "test.bzl",
      """
      <error descr="$descriptionCrossKindGlobal">x = 1</error>
      load("//:a.bzl", <error descr="$descriptionCrossKindFileLocal">"x"</error>)
      """.trimIndent(),
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `load then def should highlight cross-kind conflict`() {
    myFixture.configureByText(
      "test.bzl",
      """
      load("//:a.bzl", <error descr="$descriptionCrossKindFileLocal">"x"</error>)
      def <error descr="$descriptionCrossKindGlobal">x</error>():
          pass
      """.trimIndent(),
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `def then load should highlight cross-kind conflict`() {
    myFixture.configureByText(
      "test.bzl",
      """
      def <error descr="$descriptionCrossKindGlobal">x</error>():
          pass

      load("//:a.bzl", <error descr="$descriptionCrossKindFileLocal">"x"</error>)
      """.trimIndent(),
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `duplicate top-level assignments should highlight same-kind global conflict`() {
    myFixture.configureByText(
      "test.bzl",
      """
      <error descr="$descriptionOrig">x = 1</error>
      <error descr="$descriptionSameKind">x = 2</error>
      """.trimIndent(),
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `duplicate load string symbol should highlight same-kind file-local conflict`() {
    myFixture.configureByText(
      "test.bzl",
      """
      load("//:a.bzl", <error descr="$descriptionOrig">"x"</error>)
      load("//:b.bzl", <error descr="$descriptionSameKind">"x"</error>)
      """.trimIndent(),
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `duplicate load alias should highlight same-kind file-local conflict`() {
    myFixture.configureByText(
      "test.bzl",
      """
      load("//:a.bzl", <error descr="$descriptionOrig">x = "y"</error>)
      load("//:b.bzl", <error descr="$descriptionSameKind">x = "z"</error>)
      """.trimIndent(),
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `duplicate load alias nad string should highlight same-kind file-local conflict`() {
    myFixture.configureByText(
      "test.bzl",
      """
      load("//:a.bzl", <error descr="$descriptionOrig">"x"</error>)
      load("//:b.bzl", <error descr="$descriptionSameKind">x = "z"</error>)
      """.trimIndent(),
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `def then assignment should highlight same-kind global conflict`() {
    myFixture.configureByText(
      "test.bzl",
      """
      def <error descr="$descriptionOrig">x</error>():
          pass
      <error descr="$descriptionSameKind">x = 1</error>
      """.trimIndent(),
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `assignment then def should highlight same-kind global conflict`() {
    myFixture.configureByText(
      "test.bzl",
      """
      <error descr="$descriptionOrig">x = 1</error>
      def <error descr="$descriptionSameKind">x</error>():
          pass
      """.trimIndent(),
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `local variable reassignment should not be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      def func(v):
          x = v + 1
          x = 2
      """.trimIndent(),
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `assignment inside function should not conflict with top-level global`() {
    myFixture.configureByText(
      "test.bzl",
      """
      x = 1
      def func(v):
          x = v + 1
      """.trimIndent(),
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `build file should not be inspected`() {
    myFixture.configureByText(
      "BUILD",
      """
      x = 1
      x = 2
      load("//:a.bzl", "x")
      """.trimIndent(),
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `workspace file should not be inspected`() {
    myFixture.configureByText(
      "WORKSPACE",
      """
      x = 1
      x = 2
      load("//:a.bzl", "x")
      """.trimIndent(),
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `double duplicate should highlight original once`() {
    myFixture.configureByText(
      "test.bzl",
      """
      <error descr="$descriptionOrig">x = 1</error>
      <error descr="$descriptionSameKind">x = 2</error>
      <error descr="$descriptionSameKind">x = 3</error>
      """.trimIndent(),
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `show conflicting declarations quick fix should be available for same-kind conflict`() {
    myFixture.configureByText(
      "test.bzl",
      """
      <error descr="$descriptionOrig">x = 1</error>
      <error descr="$descriptionSameKind">x = 2</error>
      """.trimIndent(),
    )
    myFixture.checkHighlighting(true, false, false)

    myFixture.getAllQuickFixes().mapNotNull { it.text }.shouldContainAll(quickFixText)
  }

  @Test
  fun `show conflicting declarations quick fix should be available for cross-kind conflict`() {
    myFixture.configureByText(
      "test.bzl",
      """
      <error descr="$descriptionCrossKindGlobal">x = 1</error>
      load("//:a.bzl", <error descr="$descriptionCrossKindFileLocal">"x"</error>)
      """.trimIndent(),
    )
    myFixture.checkHighlighting(true, false, false)

    myFixture.getAllQuickFixes().mapNotNull { it.text }.shouldContainAll(quickFixText)
  }

  @Test
  fun `navigate to conflicting declaration action should move caret to original declaration`() {
    myFixture.configureByText(
      "test.bzl",
      """
      x = 1
      x<caret> = 2
      """.trimIndent(),
    )

    val originalOffset = myFixture.file.text.indexOf("x = 1")
    val navigateToOriginal = NavigateToConflictingDeclarationAction(navigatableAt(originalOffset))
    myFixture.launchAction(navigateToOriginal.asIntention())
    PlatformTestUtil.dispatchAllEventsInIdeEventQueue()

    myFixture.checkResult(
      """
      <selection><caret>x</selection> = 1
      x = 2
      """.trimIndent(),
    )
  }

  @Test
  fun `navigate to conflicting declaration action should navigate to function name identifier`() {
    myFixture.configureByText(
      "test.bzl",
      """
      def x():
          pass

      x<caret> = 1
      """.trimIndent(),
    )

    val functionNameOffset = myFixture.file.text.indexOf("x")
    val navigateToFunction = NavigateToConflictingDeclarationAction(navigatableAt(functionNameOffset))
    myFixture.launchAction(navigateToFunction.asIntention())
    PlatformTestUtil.dispatchAllEventsInIdeEventQueue()

    myFixture.checkResult(
      """
      def <selection><caret>x</selection>():
          pass

      x = 1
      """.trimIndent(),
    )
  }

  @Test
  fun `navigate to conflicting declaration action should navigate to string loaded symbol`() {
    myFixture.configureByText(
      "test.bzl",
      """
      load("//:a.bzl", "x")
      load("//:b.bzl", "x")
      """.trimIndent(),
    )

    val firstLoadedSymbolOffset = myFixture.file.text.indexOf("\"x\"")
    val navigateToFirstLoadedSymbol = NavigateToConflictingDeclarationAction(navigatableAt(firstLoadedSymbolOffset))
    myFixture.launchAction(navigateToFirstLoadedSymbol.asIntention())
    PlatformTestUtil.dispatchAllEventsInIdeEventQueue()

    myFixture.checkResult(
      """
      load("//:a.bzl", <selection><caret>"x"</selection>)
      load("//:b.bzl", "x")
      """.trimIndent(),
    )
  }

  @Test
  fun `navigate to conflicting declaration action should navigate to named loaded symbol`() {
    myFixture.configureByText(
      "test.bzl",
      """
      load("//:a.bzl", x="y")
      load("//:b.bzl", "x")
      """.trimIndent(),
    )

    val firstLoadedSymbolOffset = myFixture.file.text.indexOf("x")
    val navigateToFirstLoadedSymbol = NavigateToConflictingDeclarationAction(navigatableAt(firstLoadedSymbolOffset))
    myFixture.launchAction(navigateToFirstLoadedSymbol.asIntention())
    PlatformTestUtil.dispatchAllEventsInIdeEventQueue()

    myFixture.checkResult(
      """
      load("//:a.bzl", <selection><caret>x</selection>="y")
      load("//:b.bzl", "x")
      """.trimIndent(),
    )
  }

  private fun navigatableAt(offset: Int): NavigatablePsiElement {
    var current: PsiElement? = myFixture.file.findElementAt(offset)
    while (current != null && current !is NavigatablePsiElement) current = current.parent
    return current as NavigatablePsiElement
  }
}
