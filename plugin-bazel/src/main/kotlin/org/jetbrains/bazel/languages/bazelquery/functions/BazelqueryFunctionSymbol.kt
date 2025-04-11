package org.jetbrains.bazel.languages.bazelquery.functions

import com.intellij.model.Pointer
import com.intellij.openapi.project.Project
import com.intellij.platform.backend.documentation.DocumentationSymbol
import org.jetbrains.bazel.languages.bazelquery.documentation.BazelqueryFunctionDocumentationTarget

@Suppress("UnstableApiUsage")
class BazelqueryFunctionSymbol(val function: BazelqueryFunction, val project: Project) :
  DocumentationSymbol,
  Pointer<BazelqueryFunctionSymbol> {
  override fun createPointer() = this

  override fun dereference() = this

  override fun getDocumentationTarget() = BazelqueryFunctionDocumentationTarget(this, project)
}
