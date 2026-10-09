package org.jetbrains.bazel.workspace

import com.intellij.openapi.fileTypes.FileNameMatcher
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.utils.isUnder
import org.jetbrains.jps.model.fileTypes.FileNameMatcherFactory
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.io.path.relativeToOrNull

@ApiStatus.Internal
interface ProjectViewGlobSet {

  /**
   * Returns true if the path matches the glob set.
   */
  fun matches(path: Path): Boolean

  /**
   * Returns true if any pattern implies that all files under the [directory] are selected.
   * The caller then does not need to visit the directory.
   *
   * It guarantees that the patterns match the whole subtree under [directory].
   * It uses only the patterns and does not read the disk state.
   * A pattern can match all the files that are under [directory] now, but not all the possible files.
   * In this case, this function returns false.
   * For example, the `foo` directory holds only Java files.
   * This function returns `false` for the foo&#47;&#42;.java pattern and `true` for the foo&#47;&#42; pattern.
   */
  fun impliesRecursiveMatch(directory: Path): Boolean

  companion object {

    val EMPTY: ProjectViewGlobSet = object : ProjectViewGlobSet {
      override fun matches(path: Path): Boolean = false
      override fun impliesRecursiveMatch(directory: Path): Boolean = false
    }

    fun of(root: Path, vararg patterns: String): ProjectViewGlobSet = of(root, patterns.asList())

    fun of(root: Path, patterns: List<String>): ProjectViewGlobSet {
      require(root.isAbsolute)
      if (patterns.isEmpty()) return EMPTY
      if (patterns.any { it == "*" || it == "**" }) return MatchAllProjectViewGlobSet(root)
      return impl(root, patterns)
    }

    private fun impl(root: Path, patterns: List<String>): ProjectViewGlobSet {
      val factory = FileNameMatcherFactory.getInstance()
      val filenames = mutableSetOf<String>()
      val extensions = mutableSetOf<String>()
      val directories = mutableSetOf<Path>()
      val matchers = mutableListOf<FileNameMatcher>()
      for (pattern in patterns) {
        // regular filename
        if ("/" !in pattern && !pattern.containsWildcards()) {
          filenames.add(pattern)
          continue
        }
        // just *.extension pattern
        if ("/" !in pattern && pattern.startsWith("*.") && !pattern.containsWildcards(startIndex = 2)) {
          extensions.add(pattern.substring(2))
          continue
        }
        // just a dir/* or dir/
        if (pattern.isRecursiveDirectoryPattern()) {
          directories.add(Path(pattern.trimEnd('*')))
          continue
        }
        // See https://github.com/bazelbuild/intellij/blob/e7aa7f57260ac473cfa1b072b01400f81eed925d/base/src/com/google/idea/blaze/base/sync/projectview/SourceTestConfig.java#L44
        val withAsteriskAtEnd =
          pattern
            .trimEnd('*')
            .trimEnd('/')
            .plus('*')
        matchers.add(factory.createMatcher(withAsteriskAtEnd))
      }
      return ProjectViewGlobSetImpl(root, filenames, directories, extensions, matchers)
    }

    private fun String.isRecursiveDirectoryPattern(): Boolean {
      val prefix = trimEnd('*')
      return prefix.endsWith('/') && !prefix.containsWildcards()
    }

    private fun String.containsWildcards(startIndex: Int = 0): Boolean = indexOfAny("*?".toCharArray(), startIndex = startIndex) != -1
  }
}

private class MatchAllProjectViewGlobSet(root: Path) : ProjectViewGlobSet {
  private val ancestors = setOf(root)
  override fun matches(path: Path): Boolean = !path.isAbsolute || path.isUnder(ancestors)
  override fun impliesRecursiveMatch(directory: Path): Boolean = matches(directory)
}

private class ProjectViewGlobSetImpl(
  private val rootDir: Path,
  private val filenames: Set<String>,
  private val directories: Set<Path>,
  private val extensions: Set<String>,
  private val matchers: List<FileNameMatcher>,
) : ProjectViewGlobSet {

  override fun matches(path: Path): Boolean {
    val relativeFromWorkspaceRoot = path.relativeFromWorkspaceRoot() ?: return false
    if (relativeFromWorkspaceRoot.isUnder(directories)) return true
    return matchesPathString(relativeFromWorkspaceRoot.invariantSeparatorsPathString)
  }

  override fun impliesRecursiveMatch(directory: Path): Boolean =
    directory.relativeFromWorkspaceRoot()?.isUnder(directories) == true

  private fun Path.relativeFromWorkspaceRoot(): Path? = if (isAbsolute) relativeToOrNull(rootDir) else this

  private fun matchesPathString(path: String): Boolean {
    val filename = path.substringAfterLast("/")
    if (filename in filenames) return true
    if (filename.substringAfterLast('.', "") in extensions) return true
    return matchers.any { it.acceptsCharSequence(path) }
  }
}
