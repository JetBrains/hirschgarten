package org.jetbrains.bazel.languages.starlark.completion.lookups

import org.jetbrains.bazel.languages.starlark.psi.StarlarkNamedElement
import javax.swing.Icon

class StarlarkNamedLookupElement(val element: StarlarkNamedElement, wrapping: StarlarkQuote = StarlarkQuote.UNQUOTED) :
  StarlarkLookupElement(element.name ?: "", wrapping) {
  override val getIcon: Icon?
    get() = element.getIcon(0)
}
