package org.jetbrains.bazel.languages.starlark.psi

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.fileTypes.FileType
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.util.Processor
import org.jetbrains.bazel.languages.starlark.StarlarkFileType
import org.jetbrains.bazel.languages.starlark.StarlarkLanguage
import org.jetbrains.bazel.languages.starlark.bazel.BazelFileType
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkCallExpression
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkFunctionDeclaration
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkAssignmentStatement
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkExpressionStatement
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkLoadStatement

open class StarlarkFile(viewProvider: FileViewProvider) :
  PsiFileBase(viewProvider, StarlarkLanguage),
  StarlarkElement {
  override fun getFileType(): FileType = StarlarkFileType

  fun isBuildFile(): Boolean = this.getBazelFileType() == BazelFileType.BUILD

  fun getBazelFileType(): BazelFileType = BazelFileType.ofFileName(name)

  fun findRuleTarget(targetName: String): StarlarkCallExpression? =
    findChildrenByClass(StarlarkExpressionStatement::class.java)
      .mapNotNull { it.callExpressionOrNull() }
      .firstOrNull { it.getArgumentList()?.getNameArgumentValue() == targetName }

  fun searchInTopLevel(processor: Processor<StarlarkElement>, stopAt: PsiElement?): Boolean {
    val children = findChildrenByClass(StarlarkElement::class.java).toList()
    val namedElementSearchResult = keepSearchingWhenAllNamedElementsNotMatched(children, processor, stopAt)
    val loadSymbolSearchResult = keepSearchingWhenAllLoadSymbolsNotMatched(children, processor, stopAt)
    return namedElementSearchResult && loadSymbolSearchResult
  }

  fun searchInLoads(processor: Processor<StarlarkElement>, stopAt: PsiElement?): Boolean =
    keepSearchingWhenAllLoadSymbolsNotMatched(findChildrenByClass(StarlarkElement::class.java).toList(), processor, stopAt)

  private fun keepSearchingWhenAllNamedElementsNotMatched(
    children: List<StarlarkElement>,
    processor: Processor<StarlarkElement>,
    stopAt: PsiElement?,
  ) = children.all {
    when (it) {
      stopAt -> false
      is StarlarkAssignmentStatement -> it.check(processor)
      is StarlarkFunctionDeclaration -> processor.process(it)
      else -> true
    }
  }

  private fun keepSearchingWhenAllLoadSymbolsNotMatched(
    children: List<StarlarkElement>,
    processor: Processor<StarlarkElement>,
    stopAt: PsiElement?,
  ) = children.all {
    when (it) {
      stopAt -> false
      is StarlarkLoadStatement -> it.getLoadedSymbolsPsi().all { symbol -> processor.process(symbol) }
      else -> true
    }
  }
}
