package org.jetbrains.bazel.languages.starlark.psi.expressions

import com.intellij.lang.ASTNode
import com.intellij.openapi.components.serviceOrNull
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.languages.starlark.elements.StarlarkElementTypes
import org.jetbrains.bazel.languages.starlark.psi.StarlarkBaseElement
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElementVisitor
import org.jetbrains.bazel.languages.starlark.psi.StarlarkReferenceSupport
import org.jetbrains.bazel.languages.starlark.psi.expressions.arguments.StarlarkArgumentElement
import org.jetbrains.bazel.languages.starlark.psi.expressions.arguments.StarlarkArgumentExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.arguments.StarlarkNamedArgumentExpression
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkArgumentList

@ApiStatus.Internal
class StarlarkGlobExpression(node: ASTNode) : StarlarkBaseElement(node) {

  override fun acceptVisitor(visitor: StarlarkElementVisitor) = visitor.visitGlobExpression(this)

  fun getArgList(): StarlarkArgumentList? =
    node.treeParent.findChildByType(StarlarkElementTypes.ARGUMENT_LIST)?.psi as StarlarkArgumentList?

  fun getArguments(): Array<StarlarkArgumentElement> {
    val argList = getArgList()
    if (argList == null) return emptyArray()
    return argList.getArguments()
  }

  fun getKeywordArgument(name: String): StarlarkNamedArgumentExpression? {
    val list = getArgList()
    return list?.getKeywordArgument(name)
  }

  fun getIncludes(): PsiElement? {
    var arg: StarlarkArgumentElement? = getKeywordArgument("include")
    if (arg == null) {
      val allArgs = getArguments()
      if (allArgs.size != 0 && allArgs[0] is StarlarkArgumentExpression) {
        arg = allArgs[0]
      }
    }
    return arg?.getValue()
  }

  fun getExcludes(): PsiElement? = getKeywordArgument("exclude")?.getValue()

  fun areDirectoriesExcluded(): Boolean {
    val arg = getKeywordArgument("exclude_directories")
    if (arg != null) {
      // '0' and '1' are the only accepted values
      val value = arg.getValue()
      return value == null || !value.text.equals("0")
    }
    return true
  }

  override fun getReference(): PsiReference? = serviceOrNull<StarlarkReferenceSupport>()?.getReference(this)
}
