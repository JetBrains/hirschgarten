package org.jetbrains.bazel.languages.starlark.psi

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.util.startOffset
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.languages.starlark.elements.StarlarkTokenTypes
import org.jetbrains.bazel.languages.starlark.rename.RenameUtils

@ApiStatus.Internal
abstract class StarlarkNamedElement(node: ASTNode) :
  StarlarkBaseElement(node),
  PsiNameIdentifierOwner {
  override fun getName(): String? = getNameIdentifier()?.text

  override fun setName(name: String): PsiElement {
    val oldNode = getNameIdentifier()?.node ?: return this
    val newNode = RenameUtils.createNewName(project, name)
    node.replaceChild(oldNode, newNode)
    return this
  }

  override fun getNameIdentifier(): PsiElement? = findChildByType(StarlarkTokenTypes.IDENTIFIER)

  override fun getTextOffset(): Int = getNameIdentifier()?.startOffset ?: super.getTextOffset()

  override fun toString(): String = "${super.toString()}('${getName()}')"
}
