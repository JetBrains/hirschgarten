package org.jetbrains.bazel.languages.bazelquery.functions

import com.intellij.model.Pointer
import com.intellij.openapi.project.Project
import com.intellij.platform.backend.documentation.DocumentationSymbol
import org.jetbrains.bazel.languages.bazelquery.documentation.BazelQueryFunctionDocumentationTarget

@Suppress("UnstableApiUsage")
class BazelQueryFunctionSymbol(val function: BazelQueryFunction, val project: Project) :
  DocumentationSymbol,
  Pointer<BazelQueryFunctionSymbol> {
  override fun createPointer() = this

  override fun dereference() = this

  override fun getDocumentationTarget() = BazelQueryFunctionDocumentationTarget(this, project)
}
