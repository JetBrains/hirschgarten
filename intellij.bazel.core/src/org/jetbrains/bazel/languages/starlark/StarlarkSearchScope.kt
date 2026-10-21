package org.jetbrains.bazel.languages.starlark

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.DelegatingGlobalSearchScope
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.languages.starlark.bazel.BazelFileType

@ApiStatus.Internal
fun Project.starlarkProjectScope(): GlobalSearchScope = GlobalSearchScope
  .projectScope(this)
  .restrictToStarlarkFiles()

@ApiStatus.Internal
fun GlobalSearchScope.restrictToStarlarkFiles(): GlobalSearchScope {
  return GlobalSearchScope.getScopeRestrictedByFileTypes(
    this,
    StarlarkFileType,
  )
}

@ApiStatus.Internal
fun GlobalSearchScope.restrictToStarlarkFiles(
  vararg types: BazelFileType
): GlobalSearchScope = StarlarkFilesByTypeScope(restrictToStarlarkFiles(), types.toSet())

private class StarlarkFilesByTypeScope(
  base: GlobalSearchScope,
  private val types: Set<BazelFileType>,
) : DelegatingGlobalSearchScope(base) {

  override fun contains(file: VirtualFile): Boolean = super.contains(file) && BazelFileType.ofFileName(file.name) in types
}
