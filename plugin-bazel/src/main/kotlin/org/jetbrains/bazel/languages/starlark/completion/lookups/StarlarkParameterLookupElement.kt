package org.jetbrains.bazel.languages.starlark.completion.lookups

import com.intellij.codeInsight.lookup.LookupElementPresentation
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkParameter

class StarlarkParameterLookupElement(element: StarlarkParameter) : StarlarkNamedLookupElement(element, StarlarkQuote.UNQUOTED) {
  override fun getLookupString(): String = "$name = "

  override fun renderElement(presentation: LookupElementPresentation) {
    presentation.itemText = "$name ="
    presentation.icon = icon
  }
}
