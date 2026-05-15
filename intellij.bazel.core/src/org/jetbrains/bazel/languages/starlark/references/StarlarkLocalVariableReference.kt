package org.jetbrains.bazel.languages.starlark.references

import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.bazel.languages.starlark.completion.StarlarkCompletionProcessor
import org.jetbrains.bazel.languages.starlark.completion.lookups.StarlarkLookupElement
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElement
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkCompExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkTargetExpression
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkCallable
import org.jetbrains.bazel.languages.starlark.rename.RenameUtils

internal interface StarlarkLocalVariableElement : StarlarkElement {
  fun getNameNode(): ASTNode?
}

internal class StarlarkLocalVariableReference(
  element: StarlarkLocalVariableElement,
  soft: Boolean
) : PsiReferenceBase<StarlarkLocalVariableElement>(element, TextRange(0, element.textLength), soft) {

  override fun resolve(): PsiElement? {
    val element = myElement ?: return null
    val processor = StarlarkResolveProcessor(mutableListOf(), element, element.name)
    SearchUtils.searchInFile(element, processor)
    val resolved = processor.result.firstOrNull()
    if (element !is StarlarkTargetExpression) return resolved
    return resolved?.takeIf {
      // take if it's reassignment in the same scope
      it.textOffset <= element.textOffset && innermostScope(it) == innermostScope(element)
    }
  }

  private fun innermostScope(
    element: PsiElement
  ): PsiElement? = PsiTreeUtil.findFirstParent(element, true) {
    it is StarlarkCallable || it is StarlarkCompExpression
  }

  override fun getVariants(): Array<StarlarkLookupElement> =
    myElement?.let {
      val processor = StarlarkCompletionProcessor(mutableMapOf(), it)
      SearchUtils.searchInFile(it, processor)
      processor.results.values.toTypedArray()
    } ?: emptyArray()

  override fun handleElementRename(name: String): PsiElement {
    val oldNode = myElement.getNameNode() ?: return myElement
    val newNode = RenameUtils.createNewName(myElement.project, name)
    myElement.node.replaceChild(oldNode, newNode)
    return myElement
  }
}
