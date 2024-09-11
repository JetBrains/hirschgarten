package org.jetbrains.bazel.languages.starlark.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkBinaryExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkCallExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkConditionalExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkDictCompExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkDictLiteralExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkDoubleStarExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkEmptyExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkFloatLiteralExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkGeneratorExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkIntegerLiteralExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkKeyValueExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkLambdaExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkListCompExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkListLiteralExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkParenthesizedExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkPrefixExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkReferenceExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkSliceExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkSliceItem
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkStarExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkStringLiteralExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkSubscriptionExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkTargetExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkTupleExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.arguments.StarlarkArgumentExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.arguments.StarlarkNamedArgumentExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.arguments.StarlarkStarArgumentExpression
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkArgumentList
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkFunctionDeclaration
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkKeywordVariadicParameter
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkMandatoryParameter
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkOptionalParameter
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkParameterList
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkVariadicParameter
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkAssignmentStatement
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkAugAssignmentStatement
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkBreakStatement
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkContinueStatement
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkExpressionStatement
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkForStatement
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkIfStatement
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkLoadStatement
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkNamedLoadValue
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkReturnStatement
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkStatementList
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkStringLoadValue

object StarlarkElementTypes {
  val ASSIGNMENT_STATEMENT = StarlarkElementType("ASSIGNMENT_STATEMENT")
  val AUG_ASSIGNMENT_STATEMENT = StarlarkElementType("AUG_ASSIGNMENT_STATEMENT")
  val BREAK_STATEMENT = StarlarkElementType("BREAK_STATEMENT")
  val CONTINUE_STATEMENT = StarlarkElementType("CONTINUE_STATEMENT")
  val EXPRESSION_STATEMENT = StarlarkElementType("EXPRESSION_STATEMENT")
  val FOR_STATEMENT = StarlarkElementType("FOR_STATEMENT")
  val IF_STATEMENT = StarlarkElementType("IF_STATEMENT")
  val LOAD_STATEMENT = StarlarkElementType("LOAD_STATEMENT")
  val NAMED_LOAD_VALUE = StarlarkElementType("NAMED_LOAD_VALUE")
  val STRING_LOAD_VALUE = StarlarkElementType("STRING_LOAD_VALUE")
  val PASS_STATEMENT = StarlarkElementType("PASS_STATEMENT")
  val RETURN_STATEMENT = StarlarkElementType("RETURN_STATEMENT")
  val STATEMENT_LIST = StarlarkElementType("STATEMENT_LIST")

  val FUNCTION_DECLARATION = StarlarkElementType("FUNCTION_DECLARATION")
  val MANDATORY_PARAMETER = StarlarkElementType("MANDATORY_PARAMETER")
  val OPTIONAL_PARAMETER = StarlarkElementType("OPTIONAL_PARAMETER")
  val VARIADIC_PARAMETER = StarlarkElementType("VARIADIC_PARAMETER")
  val KEYWORD_VARIADIC_PARAMETER = StarlarkElementType("KEYWORD_VARIADIC_PARAMETER")
  val PARAMETER_LIST = StarlarkElementType("PARAMETER_LIST")

  val ARGUMENT_EXPRESSION = StarlarkElementType("ARGUMENT_EXPRESSION")
  val ARGUMENT_LIST = StarlarkElementType("ARGUMENT_LIST")
  val BINARY_EXPRESSION = StarlarkElementType("BINARY_EXPRESSION")
  val CALL_EXPRESSION = StarlarkElementType("CALL_EXPRESSION")
  val CONDITIONAL_EXPRESSION = StarlarkElementType("CONDITIONAL_EXPRESSION")
  val DICT_COMP_EXPRESSION = StarlarkElementType("DICT_COMP_EXPRESSION")
  val DICT_LITERAL_EXPRESSION = StarlarkElementType("DICT_LITERAL_EXPRESSION")
  val DOUBLE_STAR_EXPRESSION = StarlarkElementType("DOUBLE_STAR_EXPRESSION")
  val EMPTY_EXPRESSION = StarlarkElementType("EMPTY_EXPRESSION")
  val FLOAT_LITERAL_EXPRESSION = StarlarkElementType("FLOAT_LITERAL_EXPRESSION")
  val GENERATOR_EXPRESSION = StarlarkElementType("GENERATOR_EXPRESSION")
  val TARGET_EXPRESSION = StarlarkElementType("TARGET_EXPRESSION")
  val INTEGER_LITERAL_EXPRESSION = StarlarkElementType("INTEGER_LITERAL_EXPRESSION")
  val KEY_VALUE_EXPRESSION = StarlarkElementType("KEY_VALUE_EXPRESSION")
  val LAMBDA_EXPRESSION = StarlarkElementType("LAMBDA_EXPRESSION")
  val LIST_COMP_EXPRESSION = StarlarkElementType("LIST_COMP_EXPRESSION")
  val LIST_LITERAL_EXPRESSION = StarlarkElementType("LIST_LITERAL_EXPRESSION")
  val NAMED_ARGUMENT_EXPRESSION = StarlarkElementType("NAMED_ARGUMENT_EXPRESSION")
  val PARENTHESIZED_EXPRESSION = StarlarkElementType("PARENTHESIZED_EXPRESSION")
  val PREFIX_EXPRESSION = StarlarkElementType("PREFIX_EXPRESSION")
  val REFERENCE_EXPRESSION = StarlarkElementType("REFERENCE_EXPRESSION")
  val SLICE_EXPRESSION = StarlarkElementType("SLICE_EXPRESSION")
  val SLICE_ITEM = StarlarkElementType("SLICE_ITEM")
  val STAR_ARGUMENT_EXPRESSION = StarlarkElementType("STAR_ARGUMENT_EXPRESSION")
  val STAR_EXPRESSION = StarlarkElementType("STAR_EXPRESSION")
  val STRING_LITERAL_EXPRESSION = StarlarkElementType("STRING_LITERAL_EXPRESSION")
  val SUBSCRIPTION_EXPRESSION = StarlarkElementType("SUBSCRIPTION_EXPRESSION")
  val TUPLE_EXPRESSION = StarlarkElementType("TUPLE_EXPRESSION")

  fun createElement(node: ASTNode): PsiElement =
    when (val type = node.elementType) {
      ASSIGNMENT_STATEMENT -> StarlarkAssignmentStatement(node)
      AUG_ASSIGNMENT_STATEMENT -> StarlarkAugAssignmentStatement(node)
      BREAK_STATEMENT -> StarlarkBreakStatement(node)
      CONTINUE_STATEMENT -> StarlarkContinueStatement(node)
      EXPRESSION_STATEMENT -> StarlarkExpressionStatement(node)
      FOR_STATEMENT -> StarlarkForStatement(node)
      IF_STATEMENT -> StarlarkIfStatement(node)
      LOAD_STATEMENT -> StarlarkLoadStatement(node)
      NAMED_LOAD_VALUE -> StarlarkNamedLoadValue(node)
      STRING_LOAD_VALUE -> StarlarkStringLoadValue(node)
      PASS_STATEMENT -> StarlarkAugAssignmentStatement(node)
      RETURN_STATEMENT -> StarlarkReturnStatement(node)
      STATEMENT_LIST -> StarlarkStatementList(node)

      FUNCTION_DECLARATION -> StarlarkFunctionDeclaration(node)
      MANDATORY_PARAMETER -> StarlarkMandatoryParameter(node)
      OPTIONAL_PARAMETER -> StarlarkOptionalParameter(node)
      VARIADIC_PARAMETER -> StarlarkVariadicParameter(node)
      KEYWORD_VARIADIC_PARAMETER -> StarlarkKeywordVariadicParameter(node)
      PARAMETER_LIST -> StarlarkParameterList(node)

      ARGUMENT_EXPRESSION -> StarlarkArgumentExpression(node)
      ARGUMENT_LIST -> StarlarkArgumentList(node)
      BINARY_EXPRESSION -> StarlarkBinaryExpression(node)
      CALL_EXPRESSION -> StarlarkCallExpression(node)
      CONDITIONAL_EXPRESSION -> StarlarkConditionalExpression(node)
      DICT_COMP_EXPRESSION -> StarlarkDictCompExpression(node)
      DICT_LITERAL_EXPRESSION -> StarlarkDictLiteralExpression(node)
      DOUBLE_STAR_EXPRESSION -> StarlarkDoubleStarExpression(node)
      EMPTY_EXPRESSION -> StarlarkEmptyExpression(node)
      FLOAT_LITERAL_EXPRESSION -> StarlarkFloatLiteralExpression(node)
      GENERATOR_EXPRESSION -> StarlarkGeneratorExpression(node)
      INTEGER_LITERAL_EXPRESSION -> StarlarkIntegerLiteralExpression(node)
      KEY_VALUE_EXPRESSION -> StarlarkKeyValueExpression(node)
      LAMBDA_EXPRESSION -> StarlarkLambdaExpression(node)
      LIST_COMP_EXPRESSION -> StarlarkListCompExpression(node)
      LIST_LITERAL_EXPRESSION -> StarlarkListLiteralExpression(node)
      NAMED_ARGUMENT_EXPRESSION -> StarlarkNamedArgumentExpression(node)
      PARENTHESIZED_EXPRESSION -> StarlarkParenthesizedExpression(node)
      PREFIX_EXPRESSION -> StarlarkPrefixExpression(node)
      REFERENCE_EXPRESSION -> StarlarkReferenceExpression(node)
      SLICE_EXPRESSION -> StarlarkSliceExpression(node)
      SLICE_ITEM -> StarlarkSliceItem(node)
      STAR_ARGUMENT_EXPRESSION -> StarlarkStarArgumentExpression(node)
      STAR_EXPRESSION -> StarlarkStarExpression(node)
      STRING_LITERAL_EXPRESSION -> StarlarkStringLiteralExpression(node)
      SUBSCRIPTION_EXPRESSION -> StarlarkSubscriptionExpression(node)
      TARGET_EXPRESSION -> StarlarkTargetExpression(node)
      TUPLE_EXPRESSION -> StarlarkTupleExpression(node)

      else -> error("Unknown element type: $type")
    }
}
