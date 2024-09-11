package org.jetbrains.bazel.languages.starlark.psi

import com.intellij.psi.PsiElementVisitor
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
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkPassStatement
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkReturnStatement
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkStatementList
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkStringLoadValue

open class StarlarkElementVisitor : PsiElementVisitor() {
  fun visitArgumentExpression(node: StarlarkArgumentExpression) {
    visitElement(node)
  }

  fun visitArgumentList(node: StarlarkArgumentList) {
    visitElement(node)
  }

  fun visitAssignmentStatement(node: StarlarkAssignmentStatement) {
    visitElement(node)
  }

  fun visitAugAssignmentStatement(node: StarlarkAugAssignmentStatement) {
    visitElement(node)
  }

  fun visitBinaryExpression(node: StarlarkBinaryExpression) {
    visitElement(node)
  }

  fun visitBreakStatement(node: StarlarkBreakStatement) {
    visitElement(node)
  }

  open fun visitCallExpression(node: StarlarkCallExpression) {
    visitElement(node)
  }

  fun visitConditionalExpression(node: StarlarkConditionalExpression) {
    visitElement(node)
  }

  fun visitContinueStatement(node: StarlarkContinueStatement) {
    visitElement(node)
  }

  fun visitDictCompExpression(node: StarlarkDictCompExpression) {
    visitElement(node)
  }

  fun visitDictLiteralExpression(node: StarlarkDictLiteralExpression) {
    visitElement(node)
  }

  fun visitDoubleStarExpression(node: StarlarkDoubleStarExpression) {
    visitElement(node)
  }

  fun visitEmptyExpression(node: StarlarkEmptyExpression) {
    visitElement(node)
  }

  fun visitExpressionStatement(node: StarlarkExpressionStatement) {
    visitElement(node)
  }

  fun visitFloatLiteralExpression(node: StarlarkFloatLiteralExpression) {
    visitElement(node)
  }

  fun visitForStatement(node: StarlarkForStatement) {
    visitElement(node)
  }

  fun visitFunctionDeclaration(node: StarlarkFunctionDeclaration) {
    visitElement(node)
  }

  fun visitGeneratorExpression(node: StarlarkGeneratorExpression) {
    visitElement(node)
  }

  fun visitIfStatement(node: StarlarkIfStatement) {
    visitElement(node)
  }

  fun visitIntegerLiteralExpression(node: StarlarkIntegerLiteralExpression) {
    visitElement(node)
  }

  fun visitKeyValueExpression(node: StarlarkKeyValueExpression) {
    visitElement(node)
  }

  fun visitLambdaExpression(node: StarlarkLambdaExpression) {
    visitElement(node)
  }

  fun visitListCompExpression(node: StarlarkListCompExpression) {
    visitElement(node)
  }

  fun visitListLiteralExpression(node: StarlarkListLiteralExpression) {
    visitElement(node)
  }

  fun visitLoadStatement(node: StarlarkLoadStatement) {
    visitElement(node)
  }

  fun visitNamedArgumentExpression(node: StarlarkNamedArgumentExpression) {
    visitElement(node)
  }

  fun visitNamedLoadValue(node: StarlarkNamedLoadValue) {
    visitElement(node)
  }

  fun visitStringLoadValue(node: StarlarkStringLoadValue) {
    visitElement(node)
  }

  fun visitMandatoryParameter(node: StarlarkMandatoryParameter) {
    visitElement(node)
  }

  fun visitOptionalParameter(node: StarlarkOptionalParameter) {
    visitElement(node)
  }

  fun visitVariadicParameter(node: StarlarkVariadicParameter) {
    visitElement(node)
  }

  fun visitKeywordVariadicParameter(node: StarlarkKeywordVariadicParameter) {
    visitElement(node)
  }

  fun visitParameterList(node: StarlarkParameterList) {
    visitElement(node)
  }

  fun visitParenthesizedExpression(node: StarlarkParenthesizedExpression) {
    visitElement(node)
  }

  fun visitPassStatement(node: StarlarkPassStatement) {
    visitElement(node)
  }

  fun visitPrefixExpression(node: StarlarkPrefixExpression) {
    visitElement(node)
  }

  fun visitReferenceExpression(node: StarlarkReferenceExpression) {
    visitElement(node)
  }

  fun visitReturnStatement(node: StarlarkReturnStatement) {
    visitElement(node)
  }

  fun visitSliceExpression(node: StarlarkSliceExpression) {
    visitElement(node)
  }

  fun visitSliceItem(node: StarlarkSliceItem) {
    visitElement(node)
  }

  fun visitStarArgumentExpression(node: StarlarkStarArgumentExpression) {
    visitElement(node)
  }

  fun visitStarExpression(node: StarlarkStarExpression) {
    visitElement(node)
  }

  fun visitStatementListImpl(node: StarlarkStatementList) {
    visitElement(node)
  }

  fun visitStringLiteralExpression(node: StarlarkStringLiteralExpression) {
    visitElement(node)
  }

  fun visitSubscriptionExpression(node: StarlarkSubscriptionExpression) {
    visitElement(node)
  }

  fun visitTargetExpression(node: StarlarkTargetExpression) {
    visitElement(node)
  }

  fun visitTupleExpression(node: StarlarkTupleExpression) {
    visitElement(node)
  }
}
