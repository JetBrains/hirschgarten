package org.jetbrains.bazel.languages.starlark.references

import com.intellij.util.Processor
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElement
import org.jetbrains.bazel.languages.starlark.psi.StarlarkNamedElement

class StarlarkResolveProcessor(
    val result: MutableList<StarlarkElement>,
    private val referenceElement: StarlarkElement,
) : Processor<StarlarkElement> {
  override fun process(currentElement: StarlarkElement): Boolean =
      when {
        currentElement == referenceElement -> true
        currentElement.isTargetElement(referenceElement) -> !result.add(currentElement)
        else -> true
      }

  private fun StarlarkElement.isTargetElement(referenceElement: StarlarkElement): Boolean =
      this is StarlarkNamedElement && referenceElement.name == this.name
}
