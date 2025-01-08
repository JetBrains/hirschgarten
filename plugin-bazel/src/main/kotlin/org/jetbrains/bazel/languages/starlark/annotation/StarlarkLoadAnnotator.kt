package org.jetbrains.bazel.languages.starlark.annotation

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.psi.PsiElement
import org.jetbrains.bazel.languages.starlark.StarlarkBundle
import org.jetbrains.bazel.languages.starlark.elements.StarlarkElementTypes
import org.jetbrains.bazel.languages.starlark.references.StarlarkLoadReference

class StarlarkLoadAnnotator : StarlarkAnnotator() {
  override fun annotate(element: PsiElement, holder: AnnotationHolder) {
    if (isLoadedSymbol(element) && isNotResolvable(element)) {
      holder.annotateError(
        element = element,
        message = StarlarkBundle.message("annotator.unresolved.reference", element.text),
      )
    }
  }

  private fun isLoadedSymbol(element: PsiElement) =
    checkElementAndParentType(
      element = element,
      expectedElementTypes = listOf(StarlarkElementTypes.STRING_LITERAL_EXPRESSION),
      expectedParentTypes = listOf(StarlarkElementTypes.STRING_LOAD_VALUE, StarlarkElementTypes.NAMED_LOAD_VALUE),
    )

  private fun isNotResolvable(element: PsiElement): Boolean {
    val reference = element.reference ?: return true
    val loadReference = reference as? StarlarkLoadReference ?: return false
    return loadReference.loadedFileReference.resolve() != null && !reference.isSoft && reference.resolve() == null
  }
}
