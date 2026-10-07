package org.jetbrains.bazel.languages.starlark.utils

import com.intellij.psi.PsiElement
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkDictLiteralExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkFalseLiteralExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkFloatLiteralExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkIntegerLiteralExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkListLiteralExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkNoneLiteralExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkParenthesizedExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkStringLiteralExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkTrueLiteralExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkTupleExpression

@ApiStatus.Internal
enum class StarlarkStaticValueKind(
  val typeName: String,
  private val readOnlyMethods: Set<String> = emptySet(),
  private val mutatingMethods: Set<String> = emptySet(),
) {
  LIST(
    "list",
    readOnlyMethods = setOf("index"),
    mutatingMethods = setOf("append", "clear", "extend", "insert", "pop", "remove"),
  ),

  DICT(
    "dict",
    readOnlyMethods = setOf("get", "items", "keys", "values"),
    mutatingMethods = setOf("clear", "pop", "popitem", "setdefault", "update"),
  ),

  TUPLE("tuple"),

  STRING(
    "string",
    readOnlyMethods = setOf(
      "capitalize",
      "count",
      "elems",
      "endswith",
      "find",
      "format",
      "index",
      "isalnum",
      "isalpha",
      "isdigit",
      "islower",
      "isspace",
      "istitle",
      "isupper",
      "join",
      "lower",
      "lstrip",
      "partition",
      "removeprefix",
      "removesuffix",
      "replace",
      "rfind",
      "rindex",
      "rpartition",
      "rsplit",
      "rstrip",
      "split",
      "splitlines",
      "startswith",
      "strip",
      "title",
      "upper",
    ),
  ),

  INT("int"),
  FLOAT("float"),
  BOOL("bool"),
  NONE("NoneType");

  fun isMutatedBy(methodName: String): Boolean = methodName in mutatingMethods

  fun hasMethod(methodName: String): Boolean = methodName in readOnlyMethods || methodName in mutatingMethods

  companion object {
    val KNOWN_MUTATING_METHODS: Set<String> = entries.flatMapTo(mutableSetOf()) { it.mutatingMethods }
  }
}

@ApiStatus.Internal
object StarlarkStaticValueAnalyzer {
  fun valueKind(element: PsiElement): StarlarkStaticValueKind? =
    when (element) {
      is StarlarkListLiteralExpression -> StarlarkStaticValueKind.LIST
      is StarlarkDictLiteralExpression -> StarlarkStaticValueKind.DICT
      is StarlarkTupleExpression -> StarlarkStaticValueKind.TUPLE
      is StarlarkStringLiteralExpression -> StarlarkStaticValueKind.STRING
      is StarlarkIntegerLiteralExpression -> StarlarkStaticValueKind.INT
      is StarlarkFloatLiteralExpression -> StarlarkStaticValueKind.FLOAT
      is StarlarkTrueLiteralExpression, is StarlarkFalseLiteralExpression -> StarlarkStaticValueKind.BOOL
      is StarlarkNoneLiteralExpression -> StarlarkStaticValueKind.NONE
      is StarlarkParenthesizedExpression -> parenthesizedValueKind(element)
      else -> null
    }

  private fun parenthesizedValueKind(expression: StarlarkParenthesizedExpression): StarlarkStaticValueKind? {
    expression.getTuple()?.let { return valueKind(it) }
    for (child in expression.children) valueKind(child)?.let { return it }
    return null
  }
}
