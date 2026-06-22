package org.jetbrains.bazel.ignore

import com.google.common.cache.Cache
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.project.DefaultProjectFactory
import com.intellij.psi.PsiFileFactory
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.languages.starlark.StarlarkFileType
import org.jetbrains.bazel.languages.starlark.globbing.StarlarkGlob
import org.jetbrains.bazel.languages.starlark.psi.StarlarkFile
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkListLiteralExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkStringLiteralExpression
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkExpressionStatement
import org.jetbrains.bazel.utils.isUnder
import java.nio.file.Path
import java.util.regex.Pattern

@ApiStatus.Internal
interface BazelIgnoreMatcher {
  /**
   * Checks if given path matches the ignore criteria
   *
   * @param relativePath Path **relative** to repository root
   */
  fun match(relativePath: Path): Boolean

  companion object {
    val EMPTY = object : BazelIgnoreMatcher {
      override fun match(relativePath: Path): Boolean = false
    }
  }
}

@ApiStatus.Internal
object BazelIgnoreMatcherFactory {
  /**
   * https://bazel.build/run/bazelrc#bazel-behavior-files
   *
   * You can specify directories within the workspace that you want Bazel to ignore,
   * such as related projects that use other build systems.
   * Place a file called .bazelignore at the root of the workspace and add the directories
   * you want Bazel to ignore, one per line. Entries are relative to the workspace root.
   */
  fun fromBazelIgnoreFile(contents: String): BazelIgnoreMatcher {
    val entries =
      contents.lines()
        .filter { it.isNotBlank() }
        .filter { !it.startsWith("#") }
        .map { Path.of(it) }
        .toSet()

    if (entries.isEmpty())
      return BazelIgnoreMatcher.EMPTY

    return object : BazelIgnoreMatcher {
      override fun match(relativePath: Path): Boolean {
        return relativePath.isUnder(entries)
      }
    }
  }

  /**
   * https://bazel.build/rules/lib/globals/repo
   *
   * `ignore_directories`: this function takes a list of strings and a directory is ignored
   * if any of the given strings matches its repository-relative path according
   * to the semantics of the glob() function.
   */
  fun fromRepoBazelFile(contents: String): BazelIgnoreMatcher {

    val patterns: List<List<String>> = ReadAction.nonBlocking<List<List<String>>> {
      val fileElement = PsiFileFactory.getInstance(DefaultProjectFactory.getInstance().defaultProject).createFileFromText(
        "REPO.bazel",
        StarlarkFileType,
        contents,
      ) as StarlarkFile

      val ignoreDirectoriesCall =
        fileElement.findChildrenByClass(StarlarkExpressionStatement::class.java)
          .mapNotNull { it.callExpressionOrNull() }
          .firstOrNull { it.getCalledFunctionName() == "ignore_directories" }
        ?: return@nonBlocking emptyList()

      val ignoreList: StarlarkListLiteralExpression =
        ignoreDirectoriesCall.getArgumentList()?.getArguments()?.singleOrNull()?.getValue() as? StarlarkListLiteralExpression
        ?: return@nonBlocking emptyList()

      ignoreList
        .getElements()
        .filterIsInstance<StarlarkStringLiteralExpression>()
        .map { it.getStringContents() }
        .map { it.split('/') }
    }.executeSynchronously()

    if (patterns.isEmpty())
      return BazelIgnoreMatcher.EMPTY

    return object : BazelIgnoreMatcher {
      private val cache: Cache<String, Pattern> = StarlarkGlob.createCache()
      override fun match(relativePath: Path): Boolean {
        val pathString = relativePath.toString()
        return patterns.any { pattern ->
          StarlarkGlob.patternMatches(pattern, pathString, false, cache)
        }
      }
    }
  }
}
