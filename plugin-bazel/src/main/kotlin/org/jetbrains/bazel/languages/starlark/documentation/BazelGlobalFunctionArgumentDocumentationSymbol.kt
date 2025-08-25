package org.jetbrains.bazel.languages.starlark.documentation

import com.intellij.model.Pointer
import com.intellij.openapi.project.Project
import com.intellij.platform.backend.documentation.DocumentationSymbol
import org.jetbrains.bazel.languages.starlark.bazel.BazelGlobalFunctionParameter

@Suppress("UnstableApiUsage")
class BazelGlobalFunctionArgumentDocumentationSymbol(val argument: BazelGlobalFunctionParameter, val project: Project) :
  DocumentationSymbol,
  Pointer<BazelGlobalFunctionArgumentDocumentationSymbol> {
  override fun createPointer() = this

  override fun dereference() = this

  override fun getDocumentationTarget() = BazelGlobalFunctionArgumentDocumentationTarget(this)
}
