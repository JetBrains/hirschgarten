package org.jetbrains.bazel.languages.starlark.documentation

import com.intellij.model.Pointer
import com.intellij.openapi.project.Project
import com.intellij.platform.backend.documentation.DocumentationSymbol
import org.jetbrains.bazel.languages.starlark.bazel.BazelNativeRuleArgument

@Suppress("UnstableApiUsage")
class BazelNativeRuleArgumentDocumentationSymbol(val nativeRuleArgument: BazelNativeRuleArgument, val project: Project) :
  DocumentationSymbol,
  Pointer<BazelNativeRuleArgumentDocumentationSymbol> {
  override fun createPointer() = this

  override fun dereference() = this

  override fun getDocumentationTarget() = BazelNativeRuleArgumentDocumentationTarget(this)
}
