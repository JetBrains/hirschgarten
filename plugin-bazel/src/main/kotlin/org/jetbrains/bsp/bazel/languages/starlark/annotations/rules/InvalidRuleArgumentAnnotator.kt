package org.jetbrains.bsp.bazel.languages.starlark.annotations.rules

import com.intellij.psi.*
import org.jetbrains.bsp.bazel.languages.starlark.build.info.ProjectBuildLanguageInfoService
import org.jetbrains.bsp.bazel.languages.starlark.annotations.StarlarkAnnotationsBundle
import org.jetbrains.bsp.bazel.languages.starlark.build.info.calculateRuleDefinition

class InvalidRuleArgumentAnnotator : AbstractStarlarkNamedRuleArgumentsAnnotator() {

    override fun checkRule(
        rule: PsiElement,
        ruleNamedArgumentElements: Set<PsiElement>
    ): Map<PsiElement, String> {
        val currentLanguageSpec = ProjectBuildLanguageInfoService.getInstance(rule.project).info!!

        val ruleName = rule.firstChild.text
        val ruleDefinition = currentLanguageSpec.calculateRuleDefinition(ruleName) ?: return mapOf()

        val validArgumentsNames = ruleDefinition.attributeList.map { it.name }.toSet()

        val invalidNamedArguments = ruleNamedArgumentElements.filterNot { validArgumentsNames.contains(it.text) }
        return invalidNamedArguments.associateWith {
            StarlarkAnnotationsBundle.message("no.argument.named.name.found.for.built.in.rule.name", it.text, ruleName) }
    }
}
