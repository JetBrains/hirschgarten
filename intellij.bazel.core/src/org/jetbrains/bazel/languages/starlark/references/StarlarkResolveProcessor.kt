package org.jetbrains.bazel.languages.starlark.references

import com.intellij.util.Processor
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElement
import org.jetbrains.bazel.languages.starlark.psi.StarlarkNamedElement
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkStringLoadValue

internal class StarlarkResolveProcessor(
  val result: MutableList<StarlarkElement>,
  private val referenceElement: StarlarkElement,
  private val nameToResolve: String?,
) :
  Processor<StarlarkElement> {
  override fun process(currentElement: StarlarkElement): Boolean =
    when {
      currentElement == referenceElement -> true
      currentElement.isTargetElement() -> !result.add(currentElement)
      currentElement.isLoadTargetElement() -> addLoadTarget(currentElement as StarlarkStringLoadValue)
      else -> true
    }

  private fun StarlarkElement.isTargetElement(): Boolean =
    this is StarlarkNamedElement && nameToResolve == this.name

  private fun StarlarkElement.isLoadTargetElement(): Boolean =
    this is StarlarkStringLoadValue && nameToResolve == this.getLoadValueExpressionContent()

  private fun addLoadTarget(element: StarlarkStringLoadValue): Boolean {
    val resolved = element.getLoadValueExpression()?.reference?.resolve() as? StarlarkNamedElement ?: return true
    return !result.add(resolved)
  }
}
