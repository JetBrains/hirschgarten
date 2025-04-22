package org.jetbrains.bazel.languages.starlark.documentation

import com.intellij.model.Pointer
import com.intellij.platform.backend.documentation.DocumentationResult
import com.intellij.platform.backend.documentation.DocumentationTarget
import com.intellij.platform.backend.presentation.TargetPresentation

@Suppress("UnstableApiUsage")
class BazelNativeRuleArgumentDocumentationTarget(symbol: BazelNativeRuleArgumentDocumentationSymbol) :
  DocumentationTarget,
  Pointer<BazelNativeRuleArgumentDocumentationTarget> {
  val symbolPtr = symbol.createPointer()

  override fun createPointer() = this

  override fun dereference(): BazelNativeRuleArgumentDocumentationTarget? = symbolPtr.dereference().documentationTarget

  override fun computePresentation(): TargetPresentation =
    symbolPtr.dereference().run {
      TargetPresentation.builder(nativeRuleArgument.name).presentation()
    }

  override fun computeDocumentation(): DocumentationResult? =
    symbolPtr.dereference().run {
      val html = nativeRuleArgument.docString ?: return null
      DocumentationResult.documentation(html)
    }
}
