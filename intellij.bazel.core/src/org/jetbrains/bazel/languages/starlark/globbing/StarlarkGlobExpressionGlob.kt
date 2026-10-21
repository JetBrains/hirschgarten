package org.jetbrains.bazel.languages.starlark.globbing

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkGlobExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkListLiteralExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkStringLiteralExpression
import org.jetbrains.bazel.languages.starlark.references.findBuildFile

private fun StarlarkGlobExpression.buildGlob(): StarlarkGlob? {
  val containingDirectory = containingFile?.parent?.virtualFile ?: return null

  fun listContents(expr: PsiElement?): List<String> {
    if (expr !is StarlarkListLiteralExpression) {
      return emptyList()
    }
    return expr.getElements().filterIsInstance<StarlarkStringLiteralExpression>().map {
      it.getStringContents()
    }
  }

  fun directoryFilter(base: String): (VirtualFile) -> Boolean = { dir ->
    dir.path == base || findBuildFile(dir) == null
  }

  return StarlarkGlob
    .forPath(containingDirectory)
    .addPatterns(listContents(getIncludes()))
    .addExcludes(listContents(getExcludes()))
    .setExcludeDirectories(areDirectoriesExcluded())
    .setDirectoryFilter(directoryFilter(containingDirectory.path))
    .build()
}

@ApiStatus.Internal
fun StarlarkGlobExpression.getGlob(): StarlarkGlob? = CachedValuesManager.getCachedValue(this) {
  CachedValueProvider.Result.create(buildGlob(), PsiModificationTracker.MODIFICATION_COUNT)
}
