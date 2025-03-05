package org.jetbrains.bazel.languages.starlark.documentation

import com.intellij.model.Pointer
import com.intellij.openapi.project.Project
import com.intellij.platform.backend.documentation.DocumentationSymbol
import org.jetbrains.bazel.languages.starlark.bazel.BazelNativeRule

@Suppress("UnstableApiUsage")
class BazelNativeRuleDocumentationSymbol(val nativeRule: BazelNativeRule, val project: Project) :
  DocumentationSymbol,
  Pointer<BazelNativeRuleDocumentationSymbol> {
  override fun createPointer() = this

  override fun dereference() = this

  override fun getDocumentationTarget() = BazelNativeRulesDocumentationTarget(this)
}
