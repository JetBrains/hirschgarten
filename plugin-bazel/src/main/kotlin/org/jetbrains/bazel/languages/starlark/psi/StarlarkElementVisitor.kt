package org.jetbrains.bazel.languages.starlark.psi

import com.intellij.psi.PsiElementVisitor

class StarlarkElementVisitor : PsiElementVisitor() {
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

  fun visitCallExpression(node: StarlarkCallExpression) {
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

  fun visitLoadValueList(node: StarlarkLoadValueList) {
    visitElement(node)
  }

  fun visitNamedArgumentExpression(node: StarlarkNamedArgumentExpression) {
    visitElement(node)
  }

  fun visitNamedLoadValue(node: StarlarkNamedLoadValue) {
    visitElement(node)
  }

  fun visitNamedParameter(node: StarlarkNamedParameter) {
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

  fun visitSingleStarParameter(node: StarlarkSingleStarParameter) {
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

  fun visitTupleParameter(node: StarlarkTupleParameter) {
    visitElement(node)
  }
}
