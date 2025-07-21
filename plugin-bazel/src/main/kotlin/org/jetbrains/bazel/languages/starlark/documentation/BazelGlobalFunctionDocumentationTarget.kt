package org.jetbrains.bazel.languages.starlark.documentation

import com.intellij.model.Pointer
import com.intellij.platform.backend.documentation.DocumentationResult
import com.intellij.platform.backend.documentation.DocumentationTarget
import com.intellij.platform.backend.presentation.TargetPresentation

@Suppress("UnstableApiUsage")
class BazelGlobalFunctionDocumentationTarget(symbol: BazelGlobalFunctionDocumentationSymbol) :
  DocumentationTarget,
  Pointer<BazelGlobalFunctionDocumentationTarget> {
  val symbolPtr = symbol.createPointer()

  override fun createPointer() = this

  override fun dereference(): BazelGlobalFunctionDocumentationTarget? = symbolPtr.dereference().documentationTarget

  override fun computePresentation(): TargetPresentation =
    symbolPtr.dereference().run {
      TargetPresentation.builder(function.name).presentation()
    }

  override fun computeDocumentation(): DocumentationResult? =
    symbolPtr.dereference().run {
      val html = function.doc ?: return null
      DocumentationResult.documentation(html)
    }
}
