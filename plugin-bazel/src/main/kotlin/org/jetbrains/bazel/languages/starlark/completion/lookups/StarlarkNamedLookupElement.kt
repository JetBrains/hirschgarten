package org.jetbrains.bazel.languages.starlark.completion.lookups

import org.jetbrains.bazel.languages.starlark.psi.StarlarkNamedElement
import javax.swing.Icon

open class StarlarkNamedLookupElement(val element: StarlarkNamedElement, wrapping: StarlarkQuote = StarlarkQuote.UNQUOTED) :
  StarlarkLookupElement(element.name ?: "", wrapping) {
  override val icon: Icon?
    get() = element.getIcon(0)
}
