package org.jetbrains.bazel.languages.starlark

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope

internal fun Project.starlarkProjectScope(): GlobalSearchScope = GlobalSearchScope
  .projectScope(this)
  .restrictToStarlarkFiles()

internal fun GlobalSearchScope.restrictToStarlarkFiles(): GlobalSearchScope {
  return GlobalSearchScope.getScopeRestrictedByFileTypes(
    this,
    StarlarkFileType
  )
}
