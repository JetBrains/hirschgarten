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
  open fun visitArgumentExpression(node: StarlarkArgumentExpression) {
    visitElement(node)
  }

  open fun visitArgumentList(node: StarlarkArgumentList) {
    visitElement(node)
  }

  open fun visitAssignmentStatement(node: StarlarkAssignmentStatement) {
    visitElement(node)
  }

  open fun visitAugAssignmentStatement(node: StarlarkAugAssignmentStatement) {
    visitElement(node)
  }

  open fun visitBinaryExpression(node: StarlarkBinaryExpression) {
    visitElement(node)
  }

  open fun visitBreakStatement(node: StarlarkBreakStatement) {
    visitElement(node)
  }

  open fun visitCallExpression(node: StarlarkCallExpression) {
    visitElement(node)
  }

  open fun visitConditionalExpression(node: StarlarkConditionalExpression) {
    visitElement(node)
  }

  open fun visitContinueStatement(node: StarlarkContinueStatement) {
    visitElement(node)
  }

  open fun visitDictCompExpression(node: StarlarkDictCompExpression) {
    visitElement(node)
  }

  open fun visitDictLiteralExpression(node: StarlarkDictLiteralExpression) {
    visitElement(node)
  }

  open fun visitDoubleStarExpression(node: StarlarkDoubleStarExpression) {
    visitElement(node)
  }

  open fun visitEmptyExpression(node: StarlarkEmptyExpression) {
    visitElement(node)
  }

  open fun visitExpressionStatement(node: StarlarkExpressionStatement) {
    visitElement(node)
  }

  open fun visitFalseLiteralExpression(node: StarlarkFalseLiteralExpression) {
    visitElement(node)
  }

  open fun visitFloatLiteralExpression(node: StarlarkFloatLiteralExpression) {
    visitElement(node)
  }

  open fun visitForStatement(node: StarlarkForStatement) {
    visitElement(node)
  }

  open fun visitFunctionDeclaration(node: StarlarkFunctionDeclaration) {
    visitElement(node)
  }

  open fun visitGeneratorExpression(node: StarlarkGeneratorExpression) {
    visitElement(node)
  }

  open fun visitIfStatement(node: StarlarkIfStatement) {
    visitElement(node)
  }

  open fun visitIntegerLiteralExpression(node: StarlarkIntegerLiteralExpression) {
    visitElement(node)
  }

  open fun visitKeyValueExpression(node: StarlarkKeyValueExpression) {
    visitElement(node)
  }

  open fun visitLambdaExpression(node: StarlarkLambdaExpression) {
    visitElement(node)
  }

  open fun visitListCompExpression(node: StarlarkListCompExpression) {
    visitElement(node)
  }

  open fun visitListLiteralExpression(node: StarlarkListLiteralExpression) {
    visitElement(node)
  }

  open fun visitLoadStatement(node: StarlarkLoadStatement) {
    visitElement(node)
  }

  open fun visitFilenameLoadValue(node: StarlarkFilenameLoadValue) {
    visitElement(node)
  }

  open fun visitNamedArgumentExpression(node: StarlarkNamedArgumentExpression) {
    visitElement(node)
  }

  open fun visitNamedLoadValue(node: StarlarkNamedLoadValue) {
    visitElement(node)
  }

  open fun visitNoneLiteralExpression(node: StarlarkNoneLiteralExpression) {
    visitElement(node)
  }

  open fun visitStringLoadValue(node: StarlarkStringLoadValue) {
    visitElement(node)
  }

  open fun visitMandatoryParameter(node: StarlarkMandatoryParameter) {
    visitElement(node)
  }

  open fun visitOptionalParameter(node: StarlarkOptionalParameter) {
    visitElement(node)
  }

  open fun visitVariadicParameter(node: StarlarkVariadicParameter) {
    visitElement(node)
  }

  open fun visitKeywordVariadicParameter(node: StarlarkKeywordVariadicParameter) {
    visitElement(node)
  }

  open fun visitParameterList(node: StarlarkParameterList) {
    visitElement(node)
  }

  open fun visitParenthesizedExpression(node: StarlarkParenthesizedExpression) {
    visitElement(node)
  }

  open fun visitPassStatement(node: StarlarkPassStatement) {
    visitElement(node)
  }

  open fun visitPrefixExpression(node: StarlarkPrefixExpression) {
    visitElement(node)
  }

  open fun visitReferenceExpression(node: StarlarkReferenceExpression) {
    visitElement(node)
  }

  open fun visitReturnStatement(node: StarlarkReturnStatement) {
    visitElement(node)
  }

  open fun visitSliceExpression(node: StarlarkSliceExpression) {
    visitElement(node)
  }

  open fun visitSliceItem(node: StarlarkSliceItem) {
    visitElement(node)
  }

  open fun visitStarArgumentExpression(node: StarlarkStarArgumentExpression) {
    visitElement(node)
  }

  open fun visitStarExpression(node: StarlarkStarExpression) {
    visitElement(node)
  }

  open fun visitStatementListImpl(node: StarlarkStatementList) {
    visitElement(node)
  }

  open fun visitStringLiteralExpression(node: StarlarkStringLiteralExpression) {
    visitElement(node)
  }

  open fun visitSubscriptionExpression(node: StarlarkSubscriptionExpression) {
    visitElement(node)
  }

  open fun visitTargetExpression(node: StarlarkTargetExpression) {
    visitElement(node)
  }

  open fun visitTrueLiteralExpression(node: StarlarkTrueLiteralExpression) {
    visitElement(node)
  }

  open fun visitTupleExpression(node: StarlarkTupleExpression) {
    visitElement(node)
  }

  open fun visitGlobExpression(node: StarlarkGlobExpression) {
    visitElement(node)
  }
}
