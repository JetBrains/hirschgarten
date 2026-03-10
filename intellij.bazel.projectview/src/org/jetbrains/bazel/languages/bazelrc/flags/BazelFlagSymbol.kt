package org.jetbrains.bazel.languages.bazelrc.flags

import com.intellij.model.Pointer
import com.intellij.openapi.project.Project
import com.intellij.platform.backend.documentation.DocumentationSymbol
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.languages.bazelrc.documentation.BazelFlagDocumentationTarget

@Suppress("UnstableApiUsage")
@ApiStatus.Internal
class BazelFlagSymbol(val flag: Flag, val project: Project) :
  DocumentationSymbol,
  Pointer<BazelFlagSymbol> {
  override fun createPointer() = this

  override fun dereference() = this

  override fun getDocumentationTarget() = BazelFlagDocumentationTarget(this)
}
