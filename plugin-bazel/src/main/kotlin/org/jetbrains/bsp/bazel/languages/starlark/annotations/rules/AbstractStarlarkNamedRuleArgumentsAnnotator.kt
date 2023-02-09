package org.jetbrains.bsp.bazel.languages.starlark.annotations.rules

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiElement
import com.intellij.psi.util.childrenOfType
import com.intellij.psi.util.elementType
import org.jetbrains.bsp.bazel.languages.starlark.StarlarkTypes.*
import org.jetbrains.bsp.bazel.languages.starlark.psi.StarlarkArgument
import org.jetbrains.bsp.bazel.languages.starlark.psi.StarlarkArguments
import org.jetbrains.bsp.bazel.languages.starlark.psi.StarlarkCallSuffix
import org.jetbrains.bsp.bazel.languages.starlark.psi.StarlarkOperand

abstract class AbstractStarlarkNamedRuleArgumentsAnnotator : Annotator {
    
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        if (!element.isRuleWithArgumentsDefinition()) return

        val ruleNamedArgumentElementsWithNames =
            element.calculateRuleNamedArgumentElements()

        checkRule(element, ruleNamedArgumentElementsWithNames)
            .forEach { (psiElement, message) ->
                holder.registerError(psiElement, message)
            }
    }

    private fun AnnotationHolder.registerError(element: PsiElement, message: String) {
        newAnnotation(HighlightSeverity.ERROR, message).range(element).create()
    }

    protected abstract fun checkRule(
        rule: PsiElement,
        ruleNamedArgumentElements: Set<PsiElement>
    ): Map<PsiElement, String>
}


private fun PsiElement.isRuleWithArgumentsDefinition(): Boolean =
    elementType == PRIMARY_EXPRESSION && hasIdentifierAndCallSuffix()

private fun PsiElement.hasIdentifierAndCallSuffix(): Boolean =
    children.any { it.elementType == CALL_SUFFIX } &&
        childrenOfType<StarlarkOperand>().first().identifier != null

private fun PsiElement.calculateRuleNamedArgumentElements(): Set<PsiElement> =
        childrenOfType<StarlarkCallSuffix>().first()
            .childrenOfType<StarlarkArguments>().firstOrNull()
            ?.childrenOfType<StarlarkArgument>()
            ?.mapNotNull { it.identifier }
            ?.toSet()
            .orEmpty()
