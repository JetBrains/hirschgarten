package org.jetbrains.bazel.languages.starlark.documentation

import com.intellij.model.Pointer
import com.intellij.platform.backend.documentation.DocumentationResult
import com.intellij.platform.backend.documentation.DocumentationTarget
import com.intellij.platform.backend.presentation.TargetPresentation

@Suppress("UnstableApiUsage")
class BazelGlobalFunctionArgumentDocumentationTarget(symbol: BazelGlobalFunctionArgumentDocumentationSymbol) :
  DocumentationTarget,
  Pointer<BazelGlobalFunctionArgumentDocumentationTarget> {
  val symbolPtr = symbol.createPointer()

  override fun createPointer() = this

  override fun dereference(): BazelGlobalFunctionArgumentDocumentationTarget? = symbolPtr.dereference().documentationTarget

  override fun computePresentation(): TargetPresentation =
    symbolPtr.dereference().run {
      TargetPresentation.builder(argument.name).presentation()
    }

  override fun computeDocumentation(): DocumentationResult? =
    symbolPtr.dereference().run {
      val html = argument.docString ?: return null
      DocumentationResult.documentation(html)
    }
}
