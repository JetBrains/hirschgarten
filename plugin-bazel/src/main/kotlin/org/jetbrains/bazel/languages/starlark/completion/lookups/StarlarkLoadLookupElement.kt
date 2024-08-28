package org.jetbrains.bazel.languages.starlark.completion.lookups

import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkStringLoadValue
import javax.swing.Icon

class StarlarkLoadLookupElement(val element: StarlarkStringLoadValue) : StarlarkLookupElement(element.getImportedSymbolName() ?: "") {
  override val getIcon: Icon?
    get() = element.getIcon(0)
}
