package org.jetbrains.bazel.languages.starlark.injection

import com.intellij.lang.Language
import com.intellij.lang.injection.MultiHostInjector
import com.intellij.lang.injection.MultiHostRegistrar
import com.intellij.openapi.fileTypes.FileTypeRegistry
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.psi.PsiElement
import org.jetbrains.bazel.languages.starlark.elements.StarlarkElementTypes
import org.jetbrains.bazel.languages.starlark.elements.StarlarkTokenTypes
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkBinaryExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkCallExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkListLiteralExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkParenthesizedExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkReferenceExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkStringLiteralExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.arguments.StarlarkNamedArgumentExpression
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkArgumentList

/**
 * Injects the Shell Script language (provided by the bundled Shell Script plugin) into the `cmd` and `cmd_bash`
 * arguments of `genrule` calls, so that these get shell highlighting, completion, inspections and ShellCheck.
 * If the Shell Script plugin is not available, nothing is injected.
 *
 * The argument value can be a string literal, a `+` concatenation, a `"sep".join([...])` call on a list literal or
 * parentheses around any of these, nested arbitrarily. All string literals reachable this way end up in a single
 * injected file, in source order. Other expressions are represented by a [MISSING_VALUE] placeholder.
 */
internal class GenruleCommandShellInjector : MultiHostInjector {
  override fun elementsToInjectIn(): List<Class<out PsiElement>> =
    listOf(
      StarlarkStringLiteralExpression::class.java,
      StarlarkBinaryExpression::class.java,
      StarlarkParenthesizedExpression::class.java,
      StarlarkCallExpression::class.java,
    )

  override fun getLanguagesToInject(registrar: MultiHostRegistrar, context: PsiElement) {
    // Injection happens once per argument value, from its outermost expression. Nested parts are skipped.
    if (!isStringExpressionPart(context) || isStringExpressionPart(context.parent)) return
    if (!isGenruleCommand(context)) return
    val parts = mutableListOf<Part>()
    collectParts(context, parts)
    val places = mutableListOf<Place>()
    val pendingText = StringBuilder()
    for (part in parts) {
      when (part) {
        is Part.Literal -> {
          places += Place(pendingText.toString(), part.host)
          pendingText.clear()
        }
        is Part.Text -> pendingText.append(part.text)
      }
    }
    if (places.isEmpty()) return
    val shellLanguage = findShellScriptLanguage() ?: return
    val injection = registrar.startInjecting(shellLanguage)
    places.forEachIndexed { index, place ->
      val suffix = if (index == places.lastIndex) pendingText.toString() else ""
      injection.addPlace(place.prefix.ifEmpty { null }, suffix.ifEmpty { null }, place.host, place.host.getStringContentsOffset())
    }
    injection.doneInjecting()
  }

  /** A piece of the command: either a string literal to inject into or fixed text (a join separator or a placeholder). */
  private sealed interface Part {
    class Literal(val host: StarlarkStringLiteralExpression) : Part

    class Text(val text: String) : Part
  }

  /** A string literal to inject into, preceded by the fixed text between it and the previous literal. */
  private class Place(val prefix: String, val host: StarlarkStringLiteralExpression)

  /** Whether [element] can be a part of a string expression, see the class documentation. */
  private fun isStringExpressionPart(element: PsiElement?): Boolean =
    when (element) {
      is StarlarkStringLiteralExpression -> true
      is StarlarkBinaryExpression -> element.getOperator() == StarlarkTokenTypes.PLUS
      is StarlarkParenthesizedExpression -> element.getTuple() == null
      is StarlarkCallExpression -> joinSeparator(element) != null
      else -> false
    }

  /** Collects the parts of a string expression in source order. */
  private fun collectParts(element: PsiElement, parts: MutableList<Part>) {
    when {
      element is StarlarkStringLiteralExpression -> parts += Part.Literal(element)
      element is StarlarkBinaryExpression && element.getOperator() == StarlarkTokenTypes.PLUS ->
        element.getOperands().forEach { collectParts(it, parts) }
      element is StarlarkParenthesizedExpression && element.getTuple() == null -> {
        val inner = element.node.findChildByType(StarlarkElementTypes.EXPRESSIONS)?.psi
        if (inner != null) collectParts(inner, parts) else addPlaceholder(parts)
      }
      element is StarlarkCallExpression && joinSeparator(element) != null -> collectJoinParts(element, parts)
      else -> addPlaceholder(parts)
    }
  }

  /** `"sep".join([a, b, ...])` contributes the parts of its elements, separated by the value of the separator. */
  private fun collectJoinParts(call: StarlarkCallExpression, parts: MutableList<Part>) {
    val separator = joinSeparator(call)
    val list = call.getArgumentList()?.getArguments()?.singleOrNull()?.firstChild as? StarlarkListLiteralExpression
    if (separator == null || list == null) {
      addPlaceholder(parts)
      return
    }
    val separatorText = decodedValue(separator)
    list.getElements().forEachIndexed { index, element ->
      if (index > 0) parts += Part.Text(separatorText)
      collectParts(element, parts)
    }
  }

  private fun addPlaceholder(parts: MutableList<Part>) {
    // Consecutive non-literal parts are represented by a single placeholder.
    val last = parts.lastOrNull()
    if (last is Part.Text && last.text == MISSING_VALUE) return
    parts += Part.Text(MISSING_VALUE)
  }

  /** If [call] has the form `"sep".join(...)`, returns the separator literal. */
  private fun joinSeparator(call: StarlarkCallExpression): StarlarkStringLiteralExpression? {
    val callee = call.getCalledExpression() as? StarlarkReferenceExpression ?: return null
    if (callee.name != JOIN_METHOD) return null
    return callee.getQualifierExpression() as? StarlarkStringLiteralExpression
  }

  private fun decodedValue(literal: StarlarkStringLiteralExpression): String {
    val decoded = StringBuilder()
    literal.createLiteralTextEscaper().decode(literal.getStringContentsOffset(), decoded)
    return decoded.toString()
  }

  private fun isGenruleCommand(value: PsiElement): Boolean {
    val argument = value.parent as? StarlarkNamedArgumentExpression ?: return false
    val argumentName = argument.name ?: return false
    if (argumentName !in SHELL_COMMAND_ARGUMENTS) return false
    val call = (argument.parent as? StarlarkArgumentList)?.parent as? StarlarkCallExpression ?: return false
    val functionName = call.getCalledFunctionName() ?: return false
    return functionName in GENRULE_FUNCTIONS
  }

  /**
   * Languages are registered when they are first instantiated, which for the Shell Script language may not have
   * happened yet. Instantiating its file type also instantiates the language.
   */
  private fun findShellScriptLanguage(): Language? =
    Language.findLanguageByID(SHELL_SCRIPT_LANGUAGE_ID)
      ?: (FileTypeRegistry.getInstance().findFileTypeByName(SHELL_SCRIPT_LANGUAGE_ID) as? LanguageFileType)?.language

  private companion object {
    /** Both the language ID and the file type name of the Shell Script plugin. */
    const val SHELL_SCRIPT_LANGUAGE_ID = "Shell Script"
    const val MISSING_VALUE = "missing_value"
    const val JOIN_METHOD = "join"
    val SHELL_COMMAND_ARGUMENTS = setOf("cmd", "cmd_bash")
    val GENRULE_FUNCTIONS = setOf("genrule", "native.genrule")
  }
}
