package org.jetbrains.bazel.languages.starlark.completion

import com.intellij.util.Processor
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElement
import org.jetbrains.bazel.languages.starlark.psi.StarlarkNamedElement

class StarlarkCompletionProcessor(
  val results: MutableMap<String, StarlarkLookupElement>,
  private val inputElement: StarlarkElement,
) : Processor<StarlarkElement> {
  override fun process(currentElement: StarlarkElement): Boolean {
    if (currentElement is StarlarkNamedElement && currentElement != inputElement) {
      currentElement.name?.let { results[it] = StarlarkLookupElement(currentElement) }
    }
    return true
  }
}
