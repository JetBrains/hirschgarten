package org.jetbrains.bazel.languages.starlark.documentation

import com.intellij.model.Pointer
import com.intellij.platform.backend.documentation.DocumentationResult
import com.intellij.platform.backend.documentation.DocumentationTarget
import com.intellij.platform.backend.presentation.TargetPresentation

@Suppress("UnstableApiUsage")
class BazelNativeRulesDocumentationTarget(symbol: BazelNativeRuleDocumentationSymbol) :
  DocumentationTarget,
  Pointer<BazelNativeRulesDocumentationTarget> {
  val symbolPtr = symbol.createPointer()

  override fun createPointer() = this

  override fun dereference(): BazelNativeRulesDocumentationTarget? = symbolPtr.dereference().documentationTarget

  override fun computePresentation(): TargetPresentation =
    symbolPtr.dereference().run {
      TargetPresentation.builder(nativeRule.name).presentation()
    }

  override fun computeDocumentation(): DocumentationResult? =
    symbolPtr.dereference().run {
      val html =
        nativeRule.docString
          ?: if (nativeRule.externalDocLink != null) {
            "External documentation for ${nativeRule.name}: <a href=${nativeRule.externalDocLink}>${nativeRule.externalDocLink}</a>"
          } else {
            return null
          }

      DocumentationResult.documentation(html)
    }
}
