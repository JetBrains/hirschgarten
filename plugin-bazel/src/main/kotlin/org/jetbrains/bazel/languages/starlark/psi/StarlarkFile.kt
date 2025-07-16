package org.jetbrains.bazel.languages.starlark.psi

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.fileTypes.FileType
import com.intellij.psi.FileViewProvider
import com.intellij.util.Processor
import org.jetbrains.bazel.languages.starlark.StarlarkFileType
import org.jetbrains.bazel.languages.starlark.StarlarkLanguage
import org.jetbrains.bazel.languages.starlark.bazel.BazelFileType
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkCallExpression
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

  fun searchInLoads(processor: Processor<StarlarkElement>): Boolean =
    findChildrenByClass(StarlarkLoadStatement::class.java).flatMap { it.getLoadedSymbolsPsi() }.all(processor::process)

  fun searchInFunctionCalls(processor: Processor<StarlarkElement>): Boolean =
    findChildrenByClass(StarlarkExpressionStatement::class.java)
      .mapNotNull { it.callExpressionOrNull() }
      .all(processor::process)
}
