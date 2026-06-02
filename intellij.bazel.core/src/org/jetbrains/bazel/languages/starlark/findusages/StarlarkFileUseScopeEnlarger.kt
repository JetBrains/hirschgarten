package org.jetbrains.bazel.languages.starlark.findusages

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.search.UseScopeEnlarger
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElement
import org.jetbrains.bazel.languages.starlark.starlarkProjectScope

/**
 * Enlarges the search scope for non-starlark files e.g. Kotlin/Java sources. It provides correct search scope for [StarlarkFileUsageSearcher].
 *
 * By default, non-starlark files are private to the package they belong to.
 * However, they can be exported with [export_files](https://bazel.build/reference/be/functions#exports_files) or
 * the default visibility of the package might be changed to e.g. public, making those files available in other packages.
 * So in practice, any non-starlark file might potentially be referred from any package. That's why [starlarkProjectScope] is used here.
 **/
internal class StarlarkFileUseScopeEnlarger : UseScopeEnlarger() {
  override fun getAdditionalUseScope(element: PsiElement) = when {
    !element.project.isBazelProject || element !is PsiFile || element is StarlarkElement -> null
    else -> element.project.starlarkProjectScope()
  }
}
