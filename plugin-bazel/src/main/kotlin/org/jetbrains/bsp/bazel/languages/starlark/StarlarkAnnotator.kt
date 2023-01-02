package org.jetbrains.bsp.bazel.languages.starlark

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType
import org.jetbrains.bsp.bazel.languages.starlark.psi.impl.StarlarkArgumentImpl
import org.jetbrains.bsp.bazel.languages.starlark.psi.impl.StarlarkDefStatementImpl
import kotlin.reflect.KClass

class StarlarkAnnotator : Annotator {

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        when {
            isFunctionDeclaration(element) -> holder.mark(element, StarlarkHighlightingColors.FUNCTION_DECLARATION)
            isNamedArgument(element) -> holder.mark(element, StarlarkHighlightingColors.NAMED_ARGUMENT)
            else -> {}
        }
    }

    private fun isFunctionDeclaration(element: PsiElement): Boolean =
        checkElementAndParentType(element, listOf(StarlarkTypes.IDENTIFIER), StarlarkDefStatementImpl::class)

    private fun isNamedArgument(element: PsiElement): Boolean =
        checkElementAndParentType(
            element = element,
            expectedElementTypes = listOf(StarlarkTypes.IDENTIFIER, StarlarkTypes.EQ),
            expectedParentType = StarlarkArgumentImpl::class
        )

    private fun checkElementAndParentType(
        element: PsiElement,
        expectedElementTypes: List<IElementType>,
        expectedParentType: KClass<*>
    ): Boolean =
        expectedElementTypes.contains(element.node.elementType) && element.parent::class == expectedParentType
}

private fun AnnotationHolder.mark(element: PsiElement, attr: TextAttributesKey) {
    newSilentAnnotation(HighlightSeverity.INFORMATION).range(element).textAttributes(attr).create()
}