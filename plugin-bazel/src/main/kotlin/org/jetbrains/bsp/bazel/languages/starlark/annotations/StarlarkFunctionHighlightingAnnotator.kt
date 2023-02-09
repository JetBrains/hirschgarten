package org.jetbrains.bsp.bazel.languages.starlark.annotations

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType
import com.intellij.psi.util.elementType
import org.jetbrains.bsp.bazel.languages.starlark.StarlarkHighlightingColors
import org.jetbrains.bsp.bazel.languages.starlark.StarlarkTypes

class StarlarkFunctionHighlightingAnnotator : Annotator {

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        when {
            isFunctionDeclaration(element) -> holder.mark(element, StarlarkHighlightingColors.FUNCTION_DECLARATION)
            isNamedArgument(element) -> holder.mark(element, StarlarkHighlightingColors.NAMED_ARGUMENT)
        }
    }

    private fun isFunctionDeclaration(element: PsiElement): Boolean = checkElementAndParentType(
        element = element,
        expectedElementTypes = listOf(StarlarkTypes.IDENTIFIER),
        expectedParentTypes = listOf(StarlarkTypes.DEF_STATEMENT)
    )

    private fun isNamedArgument(element: PsiElement): Boolean = checkElementAndParentType(
        element = element,
        expectedElementTypes = listOf(StarlarkTypes.IDENTIFIER, StarlarkTypes.EQ),
        expectedParentTypes = listOf(StarlarkTypes.ARGUMENT)
    )

    private fun checkElementAndParentType(
        element: PsiElement,
        expectedElementTypes: List<IElementType>,
        expectedParentTypes: List<IElementType>
    ): Boolean =
        expectedElementTypes.contains(element.elementType) && expectedParentTypes.contains(element.parent.elementType)
}

private fun AnnotationHolder.mark(element: PsiElement, attr: TextAttributesKey) {
    newSilentAnnotation(HighlightSeverity.INFORMATION).range(element).textAttributes(attr).create()
}