package org.jetbrains.bazel.languages.starlark.references

import com.intellij.util.Processor
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElement
import org.jetbrains.bazel.languages.starlark.psi.StarlarkNamedElement
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkStringLoadValue

class StarlarkResolveProcessor(val result: MutableList<StarlarkElement>, private val referenceElement: StarlarkElement) :
  Processor<StarlarkElement> {
  override fun process(currentElement: StarlarkElement): Boolean =
    when {
      currentElement == referenceElement -> true
      currentElement.isTargetElement(referenceElement) -> !result.add(currentElement)
      currentElement.isLoadTargetElement(referenceElement) -> addLoadTarget(currentElement as StarlarkStringLoadValue)
      else -> true
    }

  private fun StarlarkElement.isTargetElement(referenceElement: StarlarkElement): Boolean =
    this is StarlarkNamedElement && referenceElement.name == this.name

  private fun StarlarkElement.isLoadTargetElement(referenceElement: StarlarkElement): Boolean =
    this is StarlarkStringLoadValue && referenceElement.name == this.getImportedSymbolName()

  private fun addLoadTarget(element: StarlarkStringLoadValue): Boolean {
    val resolved = element.getStringExpression()?.reference?.resolve() as? StarlarkNamedElement ?: return true
    return !result.add(resolved)
  }
}
