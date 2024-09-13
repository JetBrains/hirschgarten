package org.jetbrains.bazel.languages.starlark.references

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase
import org.jetbrains.bazel.languages.starlark.completion.StarlarkCompletionProcessor
import org.jetbrains.bazel.languages.starlark.psi.StarlarkFile
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkStringLiteralExpression
import org.jetbrains.bazel.languages.starlark.rename.StarlarkElementGenerator

class StarlarkLoadReference(element: StarlarkStringLiteralExpression, val loadedFileReference: BazelLabelReference) :
  PsiReferenceBase<StarlarkStringLiteralExpression>(element, TextRange(0, element.textLength), false) {
  override fun resolve(): PsiElement? {
    val loadedFile = loadedFileReference.resolve() as? StarlarkFile ?: return null
    val name = element.getStringContents() ?: return null
    val processor = StarlarkResolveNameProcessor(mutableListOf(), name)
    loadedFile.searchInTopLevel(processor, null)
    return processor.result.firstOrNull()
  }

  override fun getVariants(): Array<LookupElement> {
    val element = myElement ?: return emptyArray()
    val loadedFile = loadedFileReference.resolve() as? StarlarkFile ?: return emptyArray()
    val processor = StarlarkCompletionProcessor(mutableMapOf(), element, element.getQuote())
    loadedFile.searchInTopLevel(processor, null)
    return processor.results.values.toTypedArray()
  }

  override fun handleElementRename(newElementName: String): PsiElement {
    val newNode = StarlarkElementGenerator(element.project).createStringLiteral(newElementName)
    element.node.replaceChild(element.node.firstChildNode, newNode)
    return element
  }
}
