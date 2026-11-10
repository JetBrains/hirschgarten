package org.jetbrains.bazel.languages.starlark.globbing

import com.intellij.openapi.components.service
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.languages.bazelversion.psi.toSemVer
import org.jetbrains.bazel.languages.bazelversion.service.BazelVersionCheckerService
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkCallExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkFalseLiteralExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkListLiteralExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkReferenceExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkStringLiteralExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkTrueLiteralExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.arguments.StarlarkArgumentExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.arguments.StarlarkNamedArgumentExpression
import org.jetbrains.bazel.languages.starlark.references.findBuildFile

@ApiStatus.Internal
fun StarlarkCallExpression.asGlobCallOrNull(): StarlarkGlobCall? = when {
    getCalledFunctionName() == "glob" -> StarlarkGlobCall(this)
    else -> null
}

@ApiStatus.Internal
@JvmInline
value class StarlarkGlobCall internal constructor(val call: StarlarkCallExpression) {

  val calledExpression: StarlarkReferenceExpression
    get() = call.getCalledExpression() as StarlarkReferenceExpression

  val allowEmptyArgument: StarlarkNamedArgumentExpression?
    get() = keywordArgument(ALLOW_EMPTY)

  fun isEmptyAllowed(): Boolean = when (allowEmptyArgument?.getValue()) {
    is StarlarkTrueLiteralExpression -> true
    is StarlarkFalseLiteralExpression -> false
    else -> call.project.service<BazelVersionCheckerService>().currentBazelVersion?.toSemVer()?.major?.let { it < 8 } ?: false
  }

  fun matcher(): StarlarkGlob? = CachedValuesManager.getCachedValue(call) {
    CachedValueProvider.Result.create(buildMatcher(), PsiModificationTracker.MODIFICATION_COUNT)
  }

  private fun buildMatcher(): StarlarkGlob? {
    val containingDirectory = call.containingFile?.parent?.virtualFile ?: return null

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
      .addPatterns(listContents(includes()))
      .addExcludes(listContents(excludes()))
      .setExcludeDirectories(areDirectoriesExcluded())
      .setDirectoryFilter(directoryFilter(containingDirectory.path))
      .build()
  }

  private fun includes(): PsiElement? {
    val include = keywordArgument(INCLUDE) ?: call.getArgumentList()?.getArguments()?.firstOrNull() as? StarlarkArgumentExpression
    return include?.getValue()
  }

  private fun excludes(): PsiElement? = keywordArgument(EXCLUDE)?.getValue()

  private fun areDirectoriesExcluded(): Boolean {
    // '0' and '1' are the only accepted values
    val value = keywordArgument(EXCLUDE_DIRECTORIES)?.getValue() ?: return true
    return value.text != "0"
  }

  private fun keywordArgument(name: String): StarlarkNamedArgumentExpression? = call.getArgumentList()?.getKeywordArgument(name)

  private companion object {
    const val INCLUDE = "include"
    const val EXCLUDE = "exclude"
    const val EXCLUDE_DIRECTORIES = "exclude_directories"
    const val ALLOW_EMPTY = "allow_empty"
  }
}
