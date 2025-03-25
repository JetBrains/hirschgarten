package org.jetbrains.bazel.languages.starlark.psi.expressions

import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.util.getValue
import com.intellij.util.keyFMap.KeyFMap
import com.jetbrains.python.ast.findChildByClass
import com.jetbrains.python.ast.findChildByType
import com.jetbrains.python.ast.findChildrenByClass
import com.jetbrains.python.ast.findChildrenByType
import org.apache.commons.collections.ListUtils
import org.jetbrains.bazel.languages.starlark.elements.StarlarkElementType
import org.jetbrains.bazel.languages.starlark.elements.StarlarkElementTypes
import org.jetbrains.bazel.languages.starlark.psi.StarlarkBaseElement
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElement
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElementVisitor
import org.jetbrains.bazel.languages.starlark.psi.expressions.arguments.StarlarkArgumentElement
import org.jetbrains.bazel.languages.starlark.psi.expressions.arguments.StarlarkArgumentExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.arguments.StarlarkNamedArgumentExpression
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkArgumentList
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkKeywordVariadicParameter
import org.jetbrains.bazel.languages.starlark.references.StarlarkGlobReference
import kotlin.concurrent.Volatile

class StarlarkGlobExpression(node: ASTNode): StarlarkBaseElement(node) {
  override fun acceptVisitor(visitor: StarlarkElementVisitor) = visitor.visitGlobExpression(this)

  fun getArgList() : StarlarkArgumentList? {
    return node.treeParent.findChildByType(StarlarkElementTypes.ARGUMENT_LIST)?.psi as StarlarkArgumentList?
  }

  fun getArguments(): Array<StarlarkArgumentElement> {
    val argList = getArgList()
    if (argList == null) return arrayOf()
    return argList.getArguments()
  }

  fun getKeywordArgument(name: String): StarlarkNamedArgumentExpression? {
    val list = getArgList()
    return list?.getKeywordArgument(name)
  }

  fun getIncludes(): PsiElement? {
    var arg: StarlarkArgumentElement? = getKeywordArgument("include")
    if (arg == null) {
      var allArgs = getArguments()
      if (allArgs.size != 0 && allArgs[0] is StarlarkArgumentExpression) {
        arg = allArgs[0]
      }
    }
    return getArgValue(arg)
  }

  fun getExcludes(): PsiElement? {
    return getArgValue(getKeywordArgument("exclude"))
  }

  private fun getArgValue(arg: StarlarkArgumentElement?): PsiElement? {
    return arg?.getValue()
  }

  fun areDirectoriesExcluded(): Boolean {
    val arg = getKeywordArgument("exclude_directories")
    if (arg != null) {
      // '0' and '1' are the only accepted values
      val value = arg.getValue()
      return value == null || !value.text.equals("0")
    }
    return true
  }
//
//  fun matches(packageRelativePath: String?, isDirectory: Boolean): Boolean {
//    return getReference().matches(packageRelativePath, isDirectory)
//  }

  @Volatile
  private var reference: StarlarkGlobReference? = null

  override fun getReference(): StarlarkGlobReference? {
    val ref: StarlarkGlobReference? = reference
    if (ref != null) {
      return ref
    }
    synchronized(this) {
      if (reference == null) {
        reference = StarlarkGlobReference(this)
      }
      return reference
    }
  }

}
