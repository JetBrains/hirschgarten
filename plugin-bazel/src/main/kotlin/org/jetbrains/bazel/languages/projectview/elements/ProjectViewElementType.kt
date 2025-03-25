package org.jetbrains.bazel.languages.projectview.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType
import org.jetbrains.bazel.languages.projectview.base.ProjectViewLanguage
import java.lang.reflect.Constructor

class ProjectViewElementType(name: String, psiElementClass: Class<out PsiElement?>) : IElementType(name, ProjectViewLanguage) {
  private val parameterTypes = arrayOf(ASTNode::class.java)
  private val constructor: Constructor<out PsiElement?> = psiElementClass.getConstructor(*parameterTypes)

  fun createElement(node: ASTNode): PsiElement {
    try {
      return constructor.newInstance(node)
    } catch (e: Exception) {
      throw IllegalStateException("No necessary constructor for " + node.elementType, e)
    }
  }
}
