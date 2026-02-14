package org.jetbrains.bazel.languages.starlark.references

import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase
import org.jetbrains.bazel.languages.starlark.completion.StarlarkCompletionProcessor
import org.jetbrains.bazel.languages.starlark.completion.lookups.StarlarkLookupElement
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElement
import org.jetbrains.bazel.languages.starlark.rename.RenameUtils

interface StarlarkLocalVariableElement : StarlarkElement {
  fun getNameNode(): ASTNode?
}

class StarlarkLocalVariableReference(element: StarlarkLocalVariableElement, soft: Boolean) :
  PsiReferenceBase<StarlarkLocalVariableElement>(element, TextRange(0, element.textLength), soft) {
  override fun resolve(): PsiElement? =
    myElement?.let {
      val processor = StarlarkResolveProcessor(mutableListOf(), it)
      SearchUtils.searchInFile(it, processor)
      processor.getBestMatch()
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
