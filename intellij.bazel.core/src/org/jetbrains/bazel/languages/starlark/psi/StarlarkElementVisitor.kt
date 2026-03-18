package org.jetbrains.bazel.languages.starlark.psi

import com.intellij.psi.PsiElementVisitor
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkBinaryExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkCallExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkConditionalExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkDictCompExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkDictLiteralExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkDoubleStarExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkEmptyExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkFalseLiteralExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkFloatLiteralExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkGeneratorExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkGlobExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkIntegerLiteralExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkKeyValueExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkLambdaExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkListCompExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkListLiteralExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkNoneLiteralExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkParenthesizedExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkPrefixExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkReferenceExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkSliceExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkSliceItem
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkStarExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkStringLiteralExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkSubscriptionExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkTargetExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkTrueLiteralExpression
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
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkFilenameLoadValue
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkForStatement
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkIfStatement
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkLoadStatement
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkNamedLoadValue
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkPassStatement
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkReturnStatement
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkStatementList
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkStringLoadValue

@ApiStatus.Internal
open class StarlarkElementVisitor : PsiElementVisitor() {
  internal fun visitArgumentExpression(node: StarlarkArgumentExpression) {
    visitElement(node)
  }

  internal fun visitArgumentList(node: StarlarkArgumentList) {
    visitElement(node)
  }

  internal fun visitAssignmentStatement(node: StarlarkAssignmentStatement) {
    visitElement(node)
  }

  internal fun visitAugAssignmentStatement(node: StarlarkAugAssignmentStatement) {
    visitElement(node)
  }

  internal fun visitBinaryExpression(node: StarlarkBinaryExpression) {
    visitElement(node)
  }

  internal fun visitBreakStatement(node: StarlarkBreakStatement) {
    visitElement(node)
  }

  open fun visitCallExpression(node: StarlarkCallExpression) {
    visitElement(node)
  }

  internal fun visitConditionalExpression(node: StarlarkConditionalExpression) {
    visitElement(node)
  }

  internal fun visitContinueStatement(node: StarlarkContinueStatement) {
    visitElement(node)
  }

  internal fun visitDictCompExpression(node: StarlarkDictCompExpression) {
    visitElement(node)
  }

  internal fun visitDictLiteralExpression(node: StarlarkDictLiteralExpression) {
    visitElement(node)
  }

  internal fun visitDoubleStarExpression(node: StarlarkDoubleStarExpression) {
    visitElement(node)
  }

  internal fun visitEmptyExpression(node: StarlarkEmptyExpression) {
    visitElement(node)
  }

  internal fun visitExpressionStatement(node: StarlarkExpressionStatement) {
    visitElement(node)
  }

  internal fun visitFalseLiteralExpression(node: StarlarkFalseLiteralExpression) {
    visitElement(node)
  }

  internal fun visitFloatLiteralExpression(node: StarlarkFloatLiteralExpression) {
    visitElement(node)
  }

  internal fun visitForStatement(node: StarlarkForStatement) {
    visitElement(node)
  }

  internal fun visitFunctionDeclaration(node: StarlarkFunctionDeclaration) {
    visitElement(node)
  }

  internal fun visitGeneratorExpression(node: StarlarkGeneratorExpression) {
    visitElement(node)
  }

  internal fun visitIfStatement(node: StarlarkIfStatement) {
    visitElement(node)
  }

  internal fun visitIntegerLiteralExpression(node: StarlarkIntegerLiteralExpression) {
    visitElement(node)
  }

  internal fun visitKeyValueExpression(node: StarlarkKeyValueExpression) {
    visitElement(node)
  }

  internal fun visitLambdaExpression(node: StarlarkLambdaExpression) {
    visitElement(node)
  }

  internal fun visitListCompExpression(node: StarlarkListCompExpression) {
    visitElement(node)
  }

  internal fun visitListLiteralExpression(node: StarlarkListLiteralExpression) {
    visitElement(node)
  }

  internal fun visitLoadStatement(node: StarlarkLoadStatement) {
    visitElement(node)
  }

  internal fun visitFilenameLoadValue(node: StarlarkFilenameLoadValue) {
    visitElement(node)
  }

  internal fun visitNamedArgumentExpression(node: StarlarkNamedArgumentExpression) {
    visitElement(node)
  }

  internal fun visitNamedLoadValue(node: StarlarkNamedLoadValue) {
    visitElement(node)
  }

  internal fun visitNoneLiteralExpression(node: StarlarkNoneLiteralExpression) {
    visitElement(node)
  }

  internal fun visitStringLoadValue(node: StarlarkStringLoadValue) {
    visitElement(node)
  }

  internal fun visitMandatoryParameter(node: StarlarkMandatoryParameter) {
    visitElement(node)
  }

  internal fun visitOptionalParameter(node: StarlarkOptionalParameter) {
    visitElement(node)
  }

  internal fun visitVariadicParameter(node: StarlarkVariadicParameter) {
    visitElement(node)
  }

  internal fun visitKeywordVariadicParameter(node: StarlarkKeywordVariadicParameter) {
    visitElement(node)
  }

  internal fun visitParameterList(node: StarlarkParameterList) {
    visitElement(node)
  }

  internal fun visitParenthesizedExpression(node: StarlarkParenthesizedExpression) {
    visitElement(node)
  }

  internal fun visitPassStatement(node: StarlarkPassStatement) {
    visitElement(node)
  }

  internal fun visitPrefixExpression(node: StarlarkPrefixExpression) {
    visitElement(node)
  }

  internal fun visitReferenceExpression(node: StarlarkReferenceExpression) {
    visitElement(node)
  }

  internal fun visitReturnStatement(node: StarlarkReturnStatement) {
    visitElement(node)
  }

  internal fun visitSliceExpression(node: StarlarkSliceExpression) {
    visitElement(node)
  }

  internal fun visitSliceItem(node: StarlarkSliceItem) {
    visitElement(node)
  }

  internal fun visitStarArgumentExpression(node: StarlarkStarArgumentExpression) {
    visitElement(node)
  }

  internal fun visitStarExpression(node: StarlarkStarExpression) {
    visitElement(node)
  }

  internal fun visitStatementListImpl(node: StarlarkStatementList) {
    visitElement(node)
  }

  internal fun visitStringLiteralExpression(node: StarlarkStringLiteralExpression) {
    visitElement(node)
  }

  internal fun visitSubscriptionExpression(node: StarlarkSubscriptionExpression) {
    visitElement(node)
  }

  internal fun visitTargetExpression(node: StarlarkTargetExpression) {
    visitElement(node)
  }

  internal fun visitTrueLiteralExpression(node: StarlarkTrueLiteralExpression) {
    visitElement(node)
  }

  internal fun visitTupleExpression(node: StarlarkTupleExpression) {
    visitElement(node)
  }

  internal fun visitGlobExpression(node: StarlarkGlobExpression) {
    visitElement(node)
  }
}
