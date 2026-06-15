package org.jetbrains.bazel.languages.starlark.highlighting

import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType
import org.jetbrains.bazel.languages.starlark.elements.StarlarkTokenTypes
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkCallExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkReferenceExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.arguments.StarlarkNamedArgumentExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.isBazelFileTopLevelCall
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkFunctionDeclaration

/**
 * Resolves the semantic highlighting color for a Starlark leaf token.
 * Returns `null` for non-leaf tokens or when the token has no special meaning
 * beyond what the lexer-based [StarlarkSyntaxHighlighter] provides.
 */
internal fun PsiElement.starlarkSemanticHighlightingColor(): TextAttributesKey? = when (elementType) {
  StarlarkTokenTypes.EQ -> when (parent) {
    is StarlarkNamedArgumentExpression -> StarlarkHighlightingColors.NAMED_ARGUMENT
    else -> null
  }
  StarlarkTokenTypes.IDENTIFIER -> when (parent) {
    is StarlarkNamedArgumentExpression -> StarlarkHighlightingColors.NAMED_ARGUMENT
    is StarlarkFunctionDeclaration -> StarlarkHighlightingColors.FUNCTION_DECLARATION
    // top-level calls in BUILD/MODULE files
    is StarlarkReferenceExpression if (parent.parent as? StarlarkCallExpression)?.isBazelFileTopLevelCall() == true -> StarlarkHighlightingColors.FUNCTION_DECLARATION
    else -> null
  }
  else -> null
}
