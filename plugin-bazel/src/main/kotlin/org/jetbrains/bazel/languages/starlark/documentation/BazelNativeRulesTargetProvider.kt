package org.jetbrains.bazel.languages.starlark.documentation

import com.intellij.platform.backend.documentation.DocumentationTarget
import com.intellij.platform.backend.documentation.DocumentationTargetProvider
import com.intellij.psi.PsiFile
import org.jetbrains.bazel.languages.starlark.bazel.BazelNativeRules
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkReferenceExpression

class BazelNativeRulesTargetProvider : DocumentationTargetProvider {
  override fun documentationTargets(file: PsiFile, offset: Int): List<DocumentationTarget> {
    val element = file.findElementAt(offset) ?: return emptyList()
    val ref = element.parent as? StarlarkReferenceExpression ?: return emptyList()
    val name = ref.text
    val rule = BazelNativeRules.NATIVE_RULES_MAP[name] ?: return emptyList()
    val symbol = BazelNativeRuleDocumentationSymbol(rule, file.project)
    return listOf(symbol.documentationTarget)
  }
}
