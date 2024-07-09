package org.jetbrains.bazel.languages.starlark.completion

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementPresentation
import org.jetbrains.bazel.languages.starlark.psi.StarlarkNamedElement

class StarlarkLookupElement(val element: StarlarkNamedElement) : LookupElement() {
  override fun getLookupString(): String = element.name ?: ""

  override fun renderElement(presentation: LookupElementPresentation) {
    presentation.itemText = element.name
    presentation.icon = element.getIcon(0)
  }
}
