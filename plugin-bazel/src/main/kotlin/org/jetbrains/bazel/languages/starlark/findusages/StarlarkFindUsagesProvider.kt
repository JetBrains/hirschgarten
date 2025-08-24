package org.jetbrains.bazel.languages.starlark.findusages

import com.intellij.lang.HelpID
import com.intellij.lang.cacheBuilder.DefaultWordsScanner
import com.intellij.lang.cacheBuilder.WordsScanner
import com.intellij.lang.findUsages.FindUsagesProvider
import com.intellij.psi.PsiElement
import org.jetbrains.bazel.languages.starlark.elements.StarlarkTokenSets
import org.jetbrains.bazel.languages.starlark.lexer.StarlarkHighlightingLexer
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElement
import org.jetbrains.bazel.languages.starlark.psi.StarlarkNamedElement
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkReferenceExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkTargetExpression
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkFunctionDeclaration

class StarlarkFindUsagesProvider : FindUsagesProvider {
  override fun canFindUsagesFor(psiElement: PsiElement): Boolean =
    psiElement is StarlarkNamedElement || psiElement is StarlarkReferenceExpression

  override fun getHelpId(psiElement: PsiElement): String =
    when (psiElement) {
      is StarlarkTargetExpression, is StarlarkReferenceExpression -> "reference.dialogs.findUsages.variable"
      else -> HelpID.FIND_OTHER_USAGES
    }

  @Suppress("HardCodedStringLiteral")
  override fun getType(element: PsiElement): String =
    when (element) {
      is StarlarkFunctionDeclaration -> "function"
      is StarlarkTargetExpression, is StarlarkReferenceExpression -> "variable"
      else -> ""
    }

  @Suppress("HardCodedStringLiteral")
  override fun getDescriptiveName(element: PsiElement): String =
    when (element) {
      is StarlarkElement -> element.name ?: "<anonymous>"
      else -> element.toString()
    }

  override fun getNodeText(element: PsiElement, useFullName: Boolean): String = getDescriptiveName(element)

  override fun getWordsScanner(): WordsScanner = StarlarkWordsScanner()
}

class StarlarkWordsScanner :
  DefaultWordsScanner(
    StarlarkHighlightingLexer(),
    StarlarkTokenSets.IDENTIFIER,
    StarlarkTokenSets.COMMENT,
    StarlarkTokenSets.STRINGS,
  )
