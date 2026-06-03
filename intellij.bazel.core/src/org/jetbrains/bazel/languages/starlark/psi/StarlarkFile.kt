package org.jetbrains.bazel.languages.starlark.psi

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.fileTypes.FileType
import com.intellij.psi.FileViewProvider
import com.intellij.util.Processor
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.languages.starlark.StarlarkFileType
import org.jetbrains.bazel.languages.starlark.StarlarkLanguage
import org.jetbrains.bazel.languages.starlark.bazel.BazelFileType
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkCallExpression
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkExpressionStatement
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkLoadStatement
import org.jetbrains.bazel.languages.starlark.starlarkProjectScope

@ApiStatus.Internal
open class StarlarkFile(viewProvider: FileViewProvider) :
  PsiFileBase(viewProvider, StarlarkLanguage),
  StarlarkElement {

  override fun getUseScope() = project.starlarkProjectScope()

  override fun getFileType(): FileType = StarlarkFileType

  fun isBuildFile(): Boolean = this.getBazelFileType() == BazelFileType.BUILD

  internal fun getBazelFileType(): BazelFileType = BazelFileType.ofFileName(name)

  fun getTargetRules(): List<StarlarkCallExpression> =
    findChildrenByClass(StarlarkExpressionStatement::class.java)
      .mapNotNull { it.callExpressionOrNull() }
      .filter { it.isRuleTarget() }

  fun findTargetRule(targetName: String): StarlarkCallExpression? =
    getTargetRules().firstOrNull {
      it.getNameAttributeValue() == targetName
    }

  internal fun searchInLoads(processor: Processor<StarlarkElement>): Boolean =
    findChildrenByClass(StarlarkLoadStatement::class.java).flatMap { it.getLoadedSymbolsPsi() }.all(processor::process)

  internal fun searchInFunctionCalls(processor: Processor<StarlarkElement>): Boolean =
    findChildrenByClass(StarlarkExpressionStatement::class.java)
      .mapNotNull { it.callExpressionOrNull() }
      .all(processor::process)
}
