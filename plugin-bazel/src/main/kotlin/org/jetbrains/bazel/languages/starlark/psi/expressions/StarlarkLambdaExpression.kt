package org.jetbrains.bazel.languages.starlark.psi.expressions

import com.intellij.lang.ASTNode
import org.jetbrains.bazel.languages.starlark.psi.StarlarkBaseElement
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElementVisitor
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkCallable
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkMandatoryParameter
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkOptionalParameter
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkParameter
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkParameterList

class StarlarkLambdaExpression(node: ASTNode) :
  StarlarkBaseElement(node),
  StarlarkCallable {
  override fun acceptVisitor(visitor: StarlarkElementVisitor) = visitor.visitLambdaExpression(this)

  override fun getNamedParameters(): List<StarlarkParameter> =
    getParameters().filter { it is StarlarkMandatoryParameter || it is StarlarkOptionalParameter }

  override fun getParameters(): List<StarlarkParameter> = findChildByClass(StarlarkParameterList::class.java)?.getParameters().orEmpty()
}
