package org.jetbrains.bazel.languages.starlark.completion

import com.intellij.util.Processor
import org.jetbrains.bazel.languages.starlark.completion.lookups.StarlarkLoadLookupElement
import org.jetbrains.bazel.languages.starlark.completion.lookups.StarlarkLookupElement
import org.jetbrains.bazel.languages.starlark.completion.lookups.StarlarkNamedLookupElement
import org.jetbrains.bazel.languages.starlark.completion.lookups.StarlarkQuote
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElement
import org.jetbrains.bazel.languages.starlark.psi.StarlarkNamedElement
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkStringLoadValue

class StarlarkCompletionProcessor(
  val results: MutableMap<String, StarlarkLookupElement>,
  private val inputElement: StarlarkElement,
  private val inputWrapping: StarlarkQuote = StarlarkQuote.UNQUOTED,
) : Processor<StarlarkElement> {
  override fun process(currentElement: StarlarkElement): Boolean {
    if (currentElement == inputElement) return true
    processElement(currentElement)
    return true
  }

  private fun processElement(currentElement: StarlarkElement) =
    when (currentElement) {
      is StarlarkNamedElement -> processNamedElement(currentElement)
      is StarlarkStringLoadValue -> processLoadValue(currentElement)
      else -> {}
    }

  private fun processNamedElement(namedElement: StarlarkNamedElement) =
    namedElement.name?.let {
      results[it] = StarlarkNamedLookupElement(namedElement, inputWrapping)
    }

  private fun processLoadValue(stringLoadValue: StarlarkStringLoadValue) =
    stringLoadValue.getImportedSymbolName()?.let {
      results[it] = StarlarkLoadLookupElement(stringLoadValue)
    }
}
