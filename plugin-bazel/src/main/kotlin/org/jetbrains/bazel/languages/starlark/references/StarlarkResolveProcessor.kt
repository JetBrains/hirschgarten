package org.jetbrains.bazel.languages.starlark.references

import com.intellij.util.Processor
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElement
import org.jetbrains.bazel.languages.starlark.psi.StarlarkNamedElement
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkStringLoadValue

/**
 * Processor that finds the best matching binding:
 * - Prefers the latest binding that appears before the reference position
 * - Falls back to the first binding if none appear before (forward references)
 */
class StarlarkResolveProcessor(val result: MutableList<StarlarkElement>, private val referenceElement: StarlarkElement) :
  Processor<StarlarkElement> {
  private val referenceOffset = referenceElement.textOffset

  // Latest binding before the reference (what we want)
  private var latestBeforeReference: StarlarkElement? = null

  // First binding overall (fallback for forward references)
  private var firstBinding: StarlarkElement? = null

  override fun process(currentElement: StarlarkElement): Boolean {
    when {
      currentElement == referenceElement -> {}
      currentElement.isTargetElement(referenceElement) -> updateBestMatch(currentElement)
      currentElement.isLoadTargetElement(referenceElement) -> addLoadTarget(currentElement as StarlarkStringLoadValue)
    }
    return true
  }

  private fun updateBestMatch(element: StarlarkElement) {
    val elementOffset = element.textOffset
    if (firstBinding == null || elementOffset < firstBinding!!.textOffset) {
      firstBinding = element
    }
    if (elementOffset < referenceOffset) {
      if (latestBeforeReference == null || elementOffset > latestBeforeReference!!.textOffset) {
        latestBeforeReference = element
      }
    }
  }

  fun getBestMatch(): StarlarkElement? {
    val best = latestBeforeReference ?: firstBinding ?: return result.firstOrNull()
    result.add(best)
    return best
  }

  private fun StarlarkElement.isTargetElement(referenceElement: StarlarkElement): Boolean =
    this is StarlarkNamedElement && referenceElement.name == this.name

  private fun StarlarkElement.isLoadTargetElement(referenceElement: StarlarkElement): Boolean =
    this is StarlarkStringLoadValue && referenceElement.name == this.getImportedSymbolName()

  private fun addLoadTarget(element: StarlarkStringLoadValue): Boolean {
    val resolved = element.getStringExpression()?.reference?.resolve() as? StarlarkNamedElement ?: return true
    result.add(resolved)
    return true
  }
}
