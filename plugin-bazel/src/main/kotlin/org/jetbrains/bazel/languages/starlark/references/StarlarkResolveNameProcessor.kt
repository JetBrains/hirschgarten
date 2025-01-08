package org.jetbrains.bazel.languages.starlark.references

import com.intellij.util.Processor
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElement

class StarlarkResolveNameProcessor(val result: MutableList<StarlarkElement>, private val name: String) : Processor<StarlarkElement> {
  override fun process(currentElement: StarlarkElement): Boolean =
    when {
      currentElement.name == name -> !result.add(currentElement)
      else -> true
    }
}
