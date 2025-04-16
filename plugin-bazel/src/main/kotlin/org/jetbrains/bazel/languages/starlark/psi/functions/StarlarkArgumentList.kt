package org.jetbrains.bazel.languages.starlark.psi.functions

import com.intellij.lang.ASTNode
import org.jetbrains.bazel.languages.starlark.psi.StarlarkBaseElement
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElementVisitor
import org.jetbrains.bazel.languages.starlark.psi.expressions.arguments.StarlarkArgumentElement
import org.jetbrains.bazel.languages.starlark.psi.expressions.arguments.StarlarkNamedArgumentExpression

class StarlarkArgumentList(node: ASTNode) : StarlarkBaseElement(node) {
  override fun acceptVisitor(visitor: StarlarkElementVisitor) = visitor.visitArgumentList(this)

  fun getArgumentNames(): Set<String> = findChildrenByClass(StarlarkNamedArgumentExpression::class.java).mapNotNull { it.name }.toSet()

  fun getNameArgumentValue(): String? = getNameArgument()?.getArgumentStringValue()

  fun getNameArgument(): StarlarkNamedArgumentExpression? =
    findChildrenByClass(StarlarkNamedArgumentExpression::class.java).find { it.isNameArgument() }

  fun getKeywordArgument(name: String): StarlarkNamedArgumentExpression? =
    findChildrenByClass(StarlarkNamedArgumentExpression::class.java).find { it.name == name }

  fun getArguments(): Array<StarlarkArgumentElement> = findChildrenByClass(StarlarkArgumentElement::class.java)

  fun getDepsArgument(): StarlarkNamedArgumentExpression? =
    findChildrenByClass(StarlarkNamedArgumentExpression::class.java).let { arguments ->
      arguments.find { it.isDepsArgument() } ?: arguments.find { it.isDependenciesArgument() }
    }
}
