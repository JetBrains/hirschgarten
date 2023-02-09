package org.jetbrains.bsp.bazel.languages.starlark.annotations.rules

import com.google.devtools.build.lib.query2.proto.proto2api.Build
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.psi.PsiElement
import org.jetbrains.bsp.bazel.languages.starlark.build.info.ProjectBuildLanguageInfoService
import org.jetbrains.bsp.bazel.languages.starlark.annotations.StarlarkAnnotationsBundle
import org.jetbrains.bsp.bazel.languages.starlark.build.info.calculateRuleDefinition

class MissingRequiredNamedRuleArgumentsAnnotator : AbstractStarlarkNamedRuleArgumentsAnnotator() {

    override fun checkRule(
        rule: PsiElement,
        ruleNamedArgumentElements: Set<PsiElement>
    ): Map<PsiElement, String> {
        val languageSpec = ProjectBuildLanguageInfoService.getInstance(rule.project).info

        return if (languageSpec != null ) {
            doCheckRule(rule, ruleNamedArgumentElements, languageSpec)
        } else {
            thisLogger().warn("Cannot execute Starlark annotator due to the empty language spec.")
            emptyMap()
        }
    }

    private fun doCheckRule(
        rule: PsiElement,
        ruleNamedArgumentElements: Set<PsiElement>,
        languageSpec: Build.BuildLanguage,
    ): Map<PsiElement, String> {
        val ruleName = rule.firstChild.text
        val ruleDefinition = languageSpec.calculateRuleDefinition(ruleName)

        return ruleDefinition
            ?.let { calculateMissingArgumentsAndCreateDescription(ruleNamedArgumentElements, ruleDefinition) }
            ?.let { mapOf (rule to it) }
            .orEmpty()
    }

    private fun calculateMissingArgumentsAndCreateDescription(ruleNamedArgumentElements: Set<PsiElement>, ruleDefinition: Build.RuleDefinition): String? {
        val requiredArgumentsNames = calculateRequiredArgumentsNames(ruleDefinition)
        val missingArguments = requiredArgumentsNames - ruleNamedArgumentElements.map {it.text }.toSet()

        return when {
            missingArguments.isNotEmpty() -> createDescription(missingArguments)
            else -> null
        }
    }

    private fun calculateRequiredArgumentsNames(ruleDefinition: Build.RuleDefinition) =
        ruleDefinition.attributeList
            .filter { it.mandatory }
            .map { it.name }

    private fun createDescription(missingArguments: Collection<String>): String =
         missingArguments.joinToString(
            separator = ", ",
            prefix = StarlarkAnnotationsBundle.message("missing.required.argument") + "${if (missingArguments.size > 1) "s" else ""}: ",
            postfix = "."
         )
}
