package org.jetbrains.bazel.languages.starlark

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.DelegatingGlobalSearchScope
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.bazel.languages.starlark.bazel.BazelFileType

internal fun Project.starlarkProjectScope(): GlobalSearchScope = GlobalSearchScope
  .projectScope(this)
  .restrictToStarlarkFiles()

internal fun GlobalSearchScope.restrictToStarlarkFiles(): GlobalSearchScope {
  return GlobalSearchScope.getScopeRestrictedByFileTypes(
    this,
    StarlarkFileType,
  )
}

internal fun GlobalSearchScope.restrictToStarlarkFiles(
  vararg types: BazelFileType
): GlobalSearchScope = StarlarkFilesByTypeScope(restrictToStarlarkFiles(), types.toSet())

private class StarlarkFilesByTypeScope(
  base: GlobalSearchScope,
  private val types: Set<BazelFileType>,
) : DelegatingGlobalSearchScope(base) {

  override fun contains(file: VirtualFile): Boolean = super.contains(file) && BazelFileType.ofFileName(file.name) in types
}
