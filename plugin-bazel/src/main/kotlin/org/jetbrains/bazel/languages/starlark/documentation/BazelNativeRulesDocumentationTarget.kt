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

  override fun computeDocumentation(): DocumentationResult =
    symbolPtr.dereference()?.let {
      val html = buildString {
        append("<h3>").append(it.nativeRule.name).append("</h3>")
        it.nativeRule.docString?.let { doc -> append("<p>").append(doc).append("</p>") }
        it.nativeRule.externalDocLink?.let { link ->
          append("<p><a href='").append(link).append("'>External documentation</a></p>")
        }
      }
      DocumentationResult.documentation(html)
    } ?: DocumentationResult.documentation("<i>No documentation available.</i>")
}
