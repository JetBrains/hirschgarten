package org.jetbrains.bazel.languages.starlark.globbing

import com.google.common.base.Splitter
import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.collect.Lists
import com.google.common.collect.Ordering
import com.google.common.collect.Sets
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.readAction
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.checkCanceled
import com.intellij.openapi.progress.runBlockingCancellable
import com.intellij.openapi.project.ProjectLocator
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.isFile
import com.intellij.platform.ide.progress.ModalTaskOwner
import com.intellij.platform.ide.progress.TaskCancellation
import com.intellij.platform.ide.progress.runWithModalProgressBlocking
import com.intellij.util.concurrency.annotations.RequiresBlockingContext
import java.util.function.Predicate
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.languages.starlark.StarlarkBundle

/**
 * Implementation of a subset of UNIX-style file globbing, expanding "*" and "?" as wildcards, but
 * not [a-z] ranges.
 *
 *
 * `**` gets special treatment in include patterns. If it is used as a complete path
 * segment it matches the filenames in subdirectories recursively.
 *
 * Largely copied from old Bazel plugin's UnixGlob.java
 */
object StarlarkGlob {
  /**
   * Checks that each pattern is valid, splits it into segments and checks that each segment
   * contains only valid wildcards.
   *
   * @return list of segment arrays
   */
  private fun checkAndSplitPatterns(patterns: Collection<String>): List<Array<String>> {
    val list: MutableList<Array<String>> = Lists.newArrayListWithCapacity(patterns.size)
    for (pattern in patterns) {
      val error = GlobPatternValidator.validate(pattern)
      require(error == null) { error!! }

      val segments: Iterable<String> = Splitter.on('/').split(pattern)
      list.add(segments.toList().toTypedArray())
    }
    return list
  }

  /** Calls [matches(pattern, str, null)][.matches]  */
  fun matches(pattern: String, str: String): Boolean =
    try {
      matches(pattern, str, null)
    } catch (_: PatternSyntaxException) {
      false
    }

  /**
   * Returns whether `str` matches the glob pattern `pattern`. This method may use the
   * `patternCache` to speed up the matching process.
   *
   * @param pattern a glob pattern
   * @param str the string to match
   * @param patternCache a cache from patterns to compiled Pattern objects, or `null` to skip
   * caching
   */
  private fun matches(
    pattern: String,
    str: String,
    patternCache: Cache<String, Pattern>?,
  ): Boolean {
    if (pattern.isEmpty() || str.isEmpty()) {
      return false
    }

    // Common case: **
    if (pattern == "**") {
      return true
    }

    // Common case: *
    if (pattern == "*") {
      return true
    }

    // If a filename starts with '.', this char must be matched explicitly.
    if (str[0] == '.' && pattern[0] != '.') {
      return false
    }

    // Common case: *.xyz
    if (pattern[0] == '*' && pattern.lastIndexOf('*') == 0) {
      return str.endsWith(pattern.substring(1))
    }
    // Common case: xyz*!!
    val lastIndex = pattern.length - 1
    // The first clause of this if statement is unnecessary, but is an
    // optimization--charAt runs faster than indexOf.!!!!
    if (pattern[lastIndex] == '*' && pattern.indexOf('*') == lastIndex) {
      return str.startsWith(pattern.substring(0, lastIndex))
    }

    var regex = patternCache?.getIfPresent(pattern)
    if (regex == null) {
      regex = makePatternFromWildcard(pattern)
      patternCache?.put(pattern, regex)
    }
    return regex.matcher(str).matches()
  }

  /**
   * Returns a regular expression implementing a matcher for "pattern", in which "*" and "?" are
   * wildcards.
   *
   *
   * e.g. "foo*bar?.java" -> "foo.*bar.\\.java"
   */
  private fun makePatternFromWildcard(pattern: String): Pattern {
    val regexp = StringBuilder()
    var i = 0
    val len = pattern.length
    while (i < len) {
      val c = pattern[i]
      when (c) {
        '*' -> {
          var toIncrement = 0
          if (len > i + 1 && pattern[i + 1] == '*') {
            // The pattern '**' is interpreted to match 0 or more directory separators, not 1 or
            // more. We skip the next * and then find a trailing/leading '/' and get rid of it.
            toIncrement = 1
            if (len > i + 2 && pattern[i + 2] == '/') {
              // We have '**/' -- skip the '/'.
              toIncrement = 2
            } else if (len == i + 2 && i > 0 && pattern[i - 1] == '/') {
              // We have '/**' -- remove the '/'.
              regexp.delete(regexp.length - 1, regexp.length)
            }
          }
          regexp.append(".*")
          i += toIncrement
        }

        '?' -> regexp.append('.')
        '^', '$', '|', '+', '{', '}', '[', ']', '\\', '.' -> {
          regexp.append('\\')
          regexp.append(c)
        }

        else -> regexp.append(c)
      }
      i++
    }
    return Pattern.compile(regexp.toString())
  }

  fun forPath(path: VirtualFile): Builder = Builder(path)

  /** Builder class for UnixGlob.  */
  class Builder(private val base: VirtualFile) {
    private val patterns: MutableList<String> = ArrayList()
    private val excludes: MutableList<String> = ArrayList()
    private var excludeDirectories = false
    private var pathFilter: Predicate<VirtualFile>

    /** Creates a glob builder with the given base path.  */
    init {
      this.pathFilter = Predicate { file: VirtualFile? -> true }
    }

    /**
     * Adds a pattern to include to the glob builder.
     *
     *
     * For a description of the syntax of the patterns, see [StarlarkGlob].
     */
    @Suppress("unused")
    fun addPattern(pattern: String): Builder {
      this.patterns.add(pattern)
      return this
    }

    /**
     * Adds a pattern to include to the glob builder.
     *
     *
     * For a description of the syntax of the patterns, see [StarlarkGlob].
     */
    fun addPatterns(patterns: Collection<String>): Builder {
      this.patterns.addAll(patterns)
      return this
    }

    /**
     * Adds patterns to exclude from the results to the glob builder.
     *
     *
     * For a description of the syntax of the patterns, see [StarlarkGlob].
     */
    fun addExcludes(excludes: Collection<String>): Builder {
      this.excludes.addAll(excludes)
      return this
    }

    /** If set to true, directories are not returned in the glob result.  */
    fun setExcludeDirectories(excludeDirectories: Boolean): Builder {
      this.excludeDirectories = excludeDirectories
      return this
    }

    /**
     * If set, the given predicate is called for every directory encountered. If it returns false,
     * the corresponding item is not returned in the output and directories are not traversed
     * either.
     */
    fun setDirectoryFilter(pathFilter: Predicate<VirtualFile>): Builder {
      this.pathFilter = pathFilter
      return this
    }

    /**
     * Executes the glob.
     */
    @RequiresBlockingContext
    fun glob(): List<VirtualFile> {
      if (!base.exists() || patterns.isEmpty()) {
        return emptyList()
      }

      // If called on EDT, use modal progress to avoid freezing the UI completely.
      if (ApplicationManager.getApplication().isDispatchThread) {
        val project = ProjectLocator.getInstance().guessProjectForFile(base)
        val owner = project?.let { ModalTaskOwner.project(it) } ?: ModalTaskOwner.guess()
        return runWithModalProgressBlocking(
          owner,
          StarlarkBundle.message("progress.globbing"),
          TaskCancellation.cancellable(),
        ) {
          globSuspend()
        }
      }

      return runBlockingCancellable {
        globSuspend()
      }
    }

    suspend fun globSuspend(): List<VirtualFile> {
      if (!base.exists() || patterns.isEmpty()) {
        return emptyList()
      }

      return withContext(Dispatchers.IO) {
        checkCanceled()

        val included = GlobVisitor(excludeDirectories, pathFilter)
          .globAsync(base, patterns)
        val excluded = GlobVisitor(excludeDirectories, pathFilter)
          .globAsync(base, excludes)
        val files = (included - excluded).toList()

        Ordering
          .from(Comparator.comparing(VirtualFile::getPath))
          .immutableSortedCopy(files)
      }
    }
  }

  /**
   * GlobVisitor executes a glob using parallelism, which is useful when the glob() requires many
   * readdir() calls on high latency filesystems.
   */
  private class GlobVisitor(
    private val excludeDirectories: Boolean,
    private val dirPred: Predicate<VirtualFile>
  ) {
    // These collections are used across workers and must therefore be thread-safe.
    private val results: MutableSet<VirtualFile> = Sets.newConcurrentHashSet()
    private val cache: Cache<String, Pattern> =
      CacheBuilder
        .newBuilder()
        .build(
          object : CacheLoader<String, Pattern>() {
            override fun load(wildcard: String): Pattern = makePatternFromWildcard(wildcard)
          },
        )

    /**
     * Performs wildcard globbing: returns the sorted list of filenames that match any of `patterns` relative to `base`. Directories are traversed if and only if they match
     * `dirPred`. The predicate is also called for the root of the traversal.
     *
     *
     * Patterns may include "*" and "?", but not "[a-z]".
     *
     *
     * `**` gets special treatment in include patterns. If it is used as a complete
     * path segment it matches the filenames in subdirectories recursively.
     *
     * @throws IllegalArgumentException if any glob or exclude pattern contains errors, or if any
     * exclude pattern segment contains `**` or if any include pattern segment
     * contains `**` but not equal to it.
     */
    suspend fun globAsync(
      base: VirtualFile,
      patterns: Collection<String>,
    ): Set<VirtualFile> = coroutineScope {
      val baseIsDirectory = readAction { base.isDirectory }

      // We do a dumb loop, even though it will likely duplicate work
      // (e.g., readdir calls). In order to optimize, we would need
      // to keep track of which patterns shared sub-patterns and which did not
      // (for example consider the glob [*/*.java, sub/*.java, */*.txt]).
      for (splitPattern in checkAndSplitPatterns(patterns)) {
        launch {
          reallyGlob(
            base,
            baseIsDirectory,
            splitPattern,
            0,
          )
        }
      }

      results
    }

    /**
     * Expressed in Haskell:
     *
     * <pre>
     * reallyGlob base []     = { base }
     * reallyGlob base [x:xs] = union { reallyGlob(f, xs) | f results "base/x" }
     </pre> *
     */
    suspend fun CoroutineScope.reallyGlob(
      base: VirtualFile,
      baseIsDirectory: Boolean,
      patternParts: Array<String>,
      idx: Int,
    ) {
      checkCanceled()
      if (baseIsDirectory && !readAction { dirPred.test(base) }) {
        return
      }

      if (idx == patternParts.size) { // Base case.
        if (!(excludeDirectories && baseIsDirectory)) {
          results.add(base)
        }
        return
      }

      if (!baseIsDirectory) {
        // Nothing to find here.
        return
      }

      val pattern = patternParts[idx]

      // ** is special: it can match nothing at all.
      // For example, x/** matches x, **/y matches y, and x/**/y matches x/y.
      if ("**" == pattern) {
        launch {
          reallyGlob(
            base,
            baseIsDirectory,
            patternParts,
            idx + 1,
          )
        }
      }

      if (!pattern.contains("*") && !pattern.contains("?")) {
        checkCanceled()
        val (child, childIsDir, childIsFile) = readAction {
          val child = base.findChild(pattern) ?: return@readAction null
          Triple(child, child.isDirectory, child.isFile)
        } ?: return
        if (!childIsDir && !childIsFile) {
          // The file is a dangling symlink, fifo, does not exist, etc.
          return
        }

        reallyGlob(
          child,
          childIsDir,
          patternParts,
          idx + 1,
        )
        return
      }

      val childrenSnapshots = readAction {
        ProgressManager.checkCanceled()
        val children = getChildren(base) ?: return@readAction null
        val snapshots = ArrayList<Triple<VirtualFile, String, Boolean>>(children.size)
        for (child in children) {
          snapshots.add(Triple(child, child.name, child.isDirectory))
        }
        snapshots
      } ?: return

      for ((child, childName, childIsDir) in childrenSnapshots) {
        checkCanceled()

        if ("**" == pattern && childIsDir) {
          // Recurse without shifting the pattern.
          launch {
            reallyGlob(
              child,
              childIsDir,
              patternParts,
              idx,
            )
          }
        }
        if (matches(pattern, childName, cache)) {
          // Recurse and consume one segment of the pattern.
          if (childIsDir) {
            launch {
              reallyGlob(
                child,
                childIsDir,
                patternParts,
                idx + 1,
              )
            }
          } else {
            // Instead of using an async call, just repeat the base case above.
            if (idx + 1 == patternParts.size) {
              results.add(child)
            }
          }
        }
      }
    }

    fun getChildren(file: VirtualFile?): Array<VirtualFile>? = file?.children
  }
}
