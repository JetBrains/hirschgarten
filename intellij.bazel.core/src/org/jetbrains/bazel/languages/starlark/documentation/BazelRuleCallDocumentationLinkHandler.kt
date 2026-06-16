package org.jetbrains.bazel.languages.starlark.documentation

import com.intellij.platform.backend.documentation.DocumentationLinkHandler
import com.intellij.platform.backend.documentation.DocumentationTarget
import com.intellij.platform.backend.documentation.LinkResolveResult
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkCallExpression
import org.jetbrains.bazel.languages.starlark.references.resolveLabel

internal class BazelRuleCallDocumentationLinkHandler : DocumentationLinkHandler {
  override fun resolveLink(target: DocumentationTarget, url: String): LinkResolveResult? {
    val bazelTargetDoc = target as? BazelRuleCallDocumentationTarget ?: return null
    val label = StarlarkDocumentationLinks.labelFrom(url) ?: return null
    val containingFile = bazelTargetDoc.targetExpression.containingFile?.virtualFile ?: return null
    val resolved = resolveLabel(bazelTargetDoc.targetExpression.project, label, containingFile) ?: return null
    val resolvedCall = resolved as? StarlarkCallExpression ?: return null
    return LinkResolveResult.resolvedTarget(BazelRuleCallDocumentationTarget(resolvedCall))
  }
}
