package org.jetbrains.bazel.languages.starlark.inspection

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import com.intellij.psi.util.elementType
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.languages.starlark.StarlarkBundle
import org.jetbrains.bazel.languages.starlark.StarlarkFileType
import org.jetbrains.bazel.languages.starlark.elements.StarlarkElementTypes
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElementVisitor
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkDictLiteralExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkFalseLiteralExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkFloatLiteralExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkIntegerLiteralExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkKeyValueExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkListLiteralExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkNoneLiteralExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkParenthesizedExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkPrefixExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkStringLiteralExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkTrueLiteralExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkTupleExpression
import org.jetbrains.bazel.languages.starlark.utils.StarlarkStringDecoder
import java.math.BigDecimal
import java.math.BigInteger

@ApiStatus.Internal
class StarlarkInvalidDictKeyInspection : LocalInspectionTool() {
  override fun isAvailableForFile(file: PsiFile): Boolean = file.fileType is StarlarkFileType

  override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor = InvalidDictKeyVisitor(holder)

  private class InvalidDictKeyVisitor(private val holder: ProblemsHolder) : StarlarkElementVisitor() {
    override fun visitKeyValueExpression(node: StarlarkKeyValueExpression) {
      val key = node.children.firstOrNull { isExpression(it) } ?: return
      if (isUnhashableLiteral(key)) {
        holder.registerProblem(
          key,
          StarlarkBundle.message("inspection.description.dict.key.not.hashable")
        )
      }
    }

    override fun visitDictLiteralExpression(node: StarlarkDictLiteralExpression) {
      val seenKeys = mutableListOf<StaticDictKey>()
      for (keyValue in node.children.filterIsInstance<StarlarkKeyValueExpression>()) {
        val key = keyValue.children.firstOrNull { isExpression(it) } ?: continue
        val staticKey = staticDictKey(key) ?: continue
        if (seenKeys.any { it.isSameKey(staticKey) }) {
          holder.registerProblem(
            key,
            StarlarkBundle.message("inspection.description.dict.key.duplicate"),
          )
        } else {
          seenKeys.add(staticKey)
        }
      }
    }

    private fun isUnhashableLiteral(element: PsiElement): Boolean =
      when (val expression = unwrapParenthesized(element)) {
        is StarlarkListLiteralExpression,
        is StarlarkDictLiteralExpression -> true
        is StarlarkTupleExpression -> tupleElements(expression).any(::isUnhashableLiteral)
        else -> false
      }

    private fun unwrapParenthesized(element: PsiElement): PsiElement =
      when (element) {
        is StarlarkParenthesizedExpression -> element.getTuple()
          ?: element.children.firstOrNull { isExpression(it) }?.let(::unwrapParenthesized)
          ?: element
        else -> element
      }

    private fun tupleElements(tuple: StarlarkTupleExpression): Sequence<PsiElement> =
      tuple.children.asSequence().filter { isExpression(it) }

    private fun isExpression(element: PsiElement): Boolean =
      StarlarkElementTypes.EXPRESSIONS.contains(element.elementType)

    private fun staticDictKey(element: PsiElement): StaticDictKey? =
      when (val expression = unwrapParenthesized(element)) {
        is StarlarkStringLiteralExpression -> StarlarkStringDecoder.decodeLiteral(expression.text)?.let(StaticDictKey::StringKey)
        is StarlarkIntegerLiteralExpression -> parseIntegerNumberLiteral(expression.text)?.let(StaticDictKey::NumberKey)
        is StarlarkFloatLiteralExpression -> parseFloatNumberLiteral(expression.text)?.let(StaticDictKey::NumberKey)
        is StarlarkTrueLiteralExpression -> StaticDictKey.BooleanKey(true)
        is StarlarkFalseLiteralExpression -> StaticDictKey.BooleanKey(false)
        is StarlarkNoneLiteralExpression -> StaticDictKey.NoneKey
        is StarlarkPrefixExpression -> staticPrefixedNumberKey(expression)
        is StarlarkTupleExpression -> staticTupleKey(expression)
        else -> null
      }

    private fun parseIntegerNumberLiteral(text: String): Number? =
      runCatching {
        val value = text.lowercase()
        val integer = when {
          value.startsWith("0x") -> BigInteger(value.removePrefix("0x"), 16)
          value.startsWith("0o") -> BigInteger(value.removePrefix("0o"), 8)
          else -> BigInteger(value, 10)
        }
        Number.Integer(integer)
      }.getOrNull()

    private fun parseFloatNumberLiteral(text: String): Number? =
      runCatching {
        Number.Float(text.lowercase().toDouble())
      }.getOrNull()

    private fun staticPrefixedNumberKey(expression: StarlarkPrefixExpression): StaticDictKey? {
      val operator = expression.firstChild?.text?.takeIf { it == "+" || it == "-" } ?: return null
      val operand = expression.children.firstOrNull { isExpression(it) && it != expression.firstChild } ?: return null
      val key = staticDictKey(operand) as? StaticDictKey.NumberKey ?: return null

      val value = if (operator == "-") key.value.negate() else key.value
      return StaticDictKey.NumberKey(value)
    }

    private fun staticTupleKey(tuple: StarlarkTupleExpression): StaticDictKey? {
      val keys = mutableListOf<StaticDictKey>()
      for (element in tupleElements(tuple)) {
        keys.add(staticDictKey(element) ?: return null)
      }
      return StaticDictKey.Tuple(keys)
    }
  }

  private sealed interface StaticDictKey {
    fun isSameKey(other: StaticDictKey): Boolean

    data class StringKey(val value: String) : StaticDictKey {
      override fun isSameKey(other: StaticDictKey): Boolean = other is StringKey && value == other.value
    }

    data class NumberKey(val value: Number) : StaticDictKey {
      override fun isSameKey(other: StaticDictKey): Boolean = other is NumberKey && value.isSameNumber(other.value)
    }

    data class BooleanKey(val value: Boolean) : StaticDictKey {
      override fun isSameKey(other: StaticDictKey): Boolean = other is BooleanKey && value == other.value
    }

    data object NoneKey : StaticDictKey {
      override fun isSameKey(other: StaticDictKey): Boolean = other is NoneKey
    }

    data class Tuple(val elements: List<StaticDictKey>) : StaticDictKey {
      override fun isSameKey(other: StaticDictKey): Boolean =
        other is Tuple &&
        elements.size == other.elements.size &&
        elements.zip(other.elements).all { (left, right) -> left.isSameKey(right) }
    }
  }

  private sealed interface Number {
    fun negate(): Number
    fun isSameNumber(other: Number): Boolean

    data class Integer(val value: BigInteger) : Number {
      override fun negate(): Number = Integer(value.negate())
      override fun isSameNumber(other: Number): Boolean =
        when (other) {
          is Integer -> value == other.value
          is Float -> other.value.toExactIntegerOrNull() == value
        }
    }

    data class Float(val value: Double) : Number {
      override fun negate(): Number = Float(-value)
      override fun isSameNumber(other: Number): Boolean =
        when (other) {
          is Integer -> value.toExactIntegerOrNull() == other.value
          is Float -> value == other.value
        }
    }

    companion object {
      private fun Double.toExactIntegerOrNull(): BigInteger? =
        runCatching {
          if (!isFinite()) return null
          BigDecimal(this).toBigIntegerExact()
        }.getOrNull()
    }
  }
}
