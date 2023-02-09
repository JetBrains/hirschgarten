package org.jetbrains.bsp.bazel.languages.starlark.annotations.rules

import com.intellij.psi.*
import org.jetbrains.bsp.bazel.languages.starlark.annotations.StarlarkAnnotationsBundle

class DuplicatedNamedRuleArgumentAnnotator : AbstractStarlarkNamedRuleArgumentsAnnotator() {
    override fun checkRule(
        rule: PsiElement,
        ruleNamedArgumentElements: Set<PsiElement>
    ): Map<PsiElement, String> {
        val setOfKeywords: MutableSet<String> = mutableSetOf()
        val duplicatedArguments = ruleNamedArgumentElements.filter { !setOfKeywords.add(it.text) }

        return duplicatedArguments.associateWith { StarlarkAnnotationsBundle.message("an.argument.is.already.passed.for.this.parameter") }
    }
}
