package org.jetbrains.bazel.languages.starlark.psi.functions

import com.intellij.lang.ASTNode
import com.intellij.util.PlatformIcons
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElementVisitor
import org.jetbrains.bazel.languages.starlark.psi.StarlarkFile
import org.jetbrains.bazel.languages.starlark.psi.StarlarkNamedElement
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkStatementList
import javax.swing.Icon

class StarlarkFunctionDeclaration(node: ASTNode) :
  StarlarkNamedElement(node),
  StarlarkCallable {
  override fun acceptVisitor(visitor: StarlarkElementVisitor) = visitor.visitFunctionDeclaration(this)

  override fun getIcon(flags: Int): Icon? = PlatformIcons.FUNCTION_ICON

  fun isTopLevel(): Boolean = parent is StarlarkFile

  override fun getNamedParameters(): List<StarlarkParameter> =
    getParameters().filter { it is StarlarkMandatoryParameter || it is StarlarkOptionalParameter }

  override fun getParameters(): List<StarlarkParameter> = findChildByClass(StarlarkParameterList::class.java)?.getParameters().orEmpty()

  fun getStatementList(): StarlarkStatementList = findChildByClass(StarlarkStatementList::class.java)!!
}
