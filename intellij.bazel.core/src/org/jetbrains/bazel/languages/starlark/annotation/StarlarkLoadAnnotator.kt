package org.jetbrains.bazel.languages.starlark.annotation

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.openapi.project.DumbAware
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType
import org.jetbrains.bazel.languages.starlark.StarlarkBundle
import org.jetbrains.bazel.languages.starlark.elements.StarlarkElementTypes
import org.jetbrains.bazel.languages.starlark.references.StarlarkLoadReference

internal class StarlarkLoadAnnotator : Annotator, DumbAware {
  override fun annotate(element: PsiElement, holder: AnnotationHolder) {
    if (element.isLoadedSymbolLiteral() && isNotResolvable(element)) {
      holder.annotateError(
        element = element,
        message = StarlarkBundle.message("annotator.unresolved.reference", element.text),
      )
    }
  }

  private fun isNotResolvable(element: PsiElement): Boolean {
    val reference = element.reference ?: return true
    val loadReference = reference as? StarlarkLoadReference ?: return false
    return loadReference.loadedFileReference.resolve() != null && !reference.isSoft && reference.resolve() == null
  }

  private fun PsiElement.isLoadedSymbolLiteral(): Boolean =
    elementType == StarlarkElementTypes.STRING_LITERAL_EXPRESSION &&
    (parent.elementType == StarlarkElementTypes.STRING_LOAD_VALUE || parent.elementType == StarlarkElementTypes.NAMED_LOAD_VALUE)
}
