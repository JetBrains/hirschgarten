package org.jetbrains.bazel.languages.starlark.completion.lookups

import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkStringLoadValue
import javax.swing.Icon

class StarlarkLoadLookupElement(val element: StarlarkStringLoadValue) : StarlarkLookupElement(element.getImportedSymbolName() ?: "") {
  override val icon: Icon?
    get() = element.getIcon(0)
}
