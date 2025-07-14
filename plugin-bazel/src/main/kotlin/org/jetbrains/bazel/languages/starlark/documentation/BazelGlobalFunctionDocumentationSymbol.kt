package org.jetbrains.bazel.languages.starlark.documentation

import com.intellij.model.Pointer
import com.intellij.openapi.project.Project
import com.intellij.platform.backend.documentation.DocumentationSymbol
import org.jetbrains.bazel.languages.starlark.bazel.BazelGlobalFunction

@Suppress("UnstableApiUsage")
class BazelGlobalFunctionDocumentationSymbol(val nativeRule: BazelGlobalFunction, val project: Project) :
  DocumentationSymbol,
  Pointer<BazelGlobalFunctionDocumentationSymbol> {
  override fun createPointer() = this

  override fun dereference() = this

  override fun getDocumentationTarget() = BazelGlobalFunctionDocumentationTarget(this)
}
