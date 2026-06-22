// Copyright 2014 The Bazel Authors. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.jetbrains.bazel.languages.starlark.globbing

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.checkCanceled
import com.intellij.openapi.progress.runBlockingCancellable
import com.intellij.openapi.project.ProjectLocator
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.isFile
import com.intellij.platform.ide.progress.ModalTaskOwner
import com.intellij.platform.ide.progress.TaskCancellation
import com.intellij.platform.ide.progress.runWithModalProgressBlocking
import com.intellij.util.concurrency.annotations.RequiresBlockingContext
import com.intellij.util.containers.tail
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.languages.starlark.StarlarkBundle
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Predicate
import java.util.regex.Pattern

/**
 * Implementation of a subset of UNIX-style file globbing, expanding "*" and "?" as wildcards, but
 * not [a-z] ranges.
 *
 * `**` gets special treatment in include patterns. If it is used as a complete path
 * segment it matches the filenames in subdirectories recursively.
 *
 * Largely copied from old Bazel plugin's UnixGlob.java
 */
@ApiStatus.Internal
class StarlarkGlob private constructor(
  private val base: VirtualFile,
  private val patterns: List<List<String>>,
  private val excludes: List<List<String>>,
  private val excludeDirectories: Boolean,
  private val pathFilter: Predicate<VirtualFile>,
) {

  private val cache: Cache<String, Pattern> = createCache()

  /**
   * Checks if given file matches the glob pattern.
   */
  fun match(file: VirtualFile): Boolean {
    if (file.isDirectory && excludeDirectories) return false
    val relativePath = VfsUtil.getRelativePath(file, base)
                       ?: return false // relative path is null when file is not under base

    var subDir = base
    for (segment in relativePath.split("/").dropLast(1)) {
      subDir = subDir.findChild(segment) ?: return false
      if (!pathFilter.test(subDir))
        return false
    }

    return match(relativePath)
  }

  /**
   * Checks if the given relative path matches the glob pattern.
   */
  fun match(relativePath: String): Boolean {
    val included = patterns.any { include -> patternMatches(include, relativePath, excludeDirectories, cache) }
    if (included) {
      val excluded = excludes.any { exclude -> patternMatches(exclude, relativePath, excludeDirectories, cache) }
      return !excluded
    }
    return false
  }

  /**
   * Executes the glob.
   */
  @RequiresBlockingContext
  fun execute(): List<VirtualFile> {
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
        executeSuspend()
      }
    }

    return runBlockingCancellable {
      executeSuspend()
    }
  }

  suspend fun executeSuspend(): List<VirtualFile> {
    if (!base.exists() || patterns.isEmpty()) {
      return emptyList()
    }
    checkCanceled()

    val included = GlobVisitor(excludeDirectories, pathFilter)
      .globAsync(base, patterns)
    if (included.isEmpty()) {
      return emptyList()
    }

    val excluded = GlobVisitor(excludeDirectories, pathFilter)
      .globAsync(base, excludes)

    return (included - excluded).sortedBy { it.path }
  }

  /**
   * GlobVisitor executes a glob using parallelism, which is useful when the glob() requires many
   * readdir() calls on high latency filesystems.
   */
  private inner class GlobVisitor(
    private val excludeDirectories: Boolean,
    private val dirPred: Predicate<VirtualFile>,
  ) {
    private val results: MutableSet<VirtualFile> = ConcurrentHashMap.newKeySet()

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
      patterns: Collection<List<String>>,
    ): Set<VirtualFile> {
      val baseIsDirectory = base.isDirectory

      // We do a dumb loop, even though it will likely duplicate work
      // (e.g., readdir calls). In order to optimize, we would need
      // to keep track of which patterns shared sub-patterns and which did not
      // (for example consider the glob [*/*.java, sub/*.java, */*.txt]).
      return coroutineScope {
        patterns.forEach { splitPattern ->
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
      patternParts: List<String>,
      idx: Int,
    ) {
      checkCanceled()
      if (baseIsDirectory && !dirPred.test(base) ) {
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
        val child = base.findChild(pattern) ?: return
        val childIsDir = child.isDirectory
        val childIsFile = child.isFile
        if (!childIsDir && !childIsFile) {
          // The file is a dangling symlink, fifo, does not exist, etc.
          return
        }
        launch {
          reallyGlob(
            child,
            childIsDir,
            patternParts,
            idx + 1,
          )
        }
        return
      }

      val children = getChildren(base) ?: return

      for (child in children) {
        checkCanceled()

        val childName = child.name
        val childIsDir = child.isDirectory

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
        if (partMatches(pattern, childName, cache)) {
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
          }
          else {
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

  /** Builder class for UnixGlob.  */
  class Builder(private val base: VirtualFile) {
    private val patterns: MutableList<String> = ArrayList()
    private val excludes: MutableList<String> = ArrayList()
    private var excludeDirectories = true
    private var pathFilter: Predicate<VirtualFile> = Predicate { true }

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
     *
     * The predicate must be pure and fast, and it must not access PSI, indexes or other APIs
     * that require a read action. It should only use VFS-level information available from
     * {@link com.intellij.openapi.vfs.VirtualFile}.
     */
    fun setDirectoryFilter(pathFilter: Predicate<VirtualFile>): Builder {
      this.pathFilter = pathFilter
      return this
    }

    /**
     * Construct glob of this builder
     */
    fun build(): StarlarkGlob {
      return StarlarkGlob(base, checkAndSplitPatterns(patterns), checkAndSplitPatterns(excludes), excludeDirectories, pathFilter)
    }
  }

  companion object {
    /**
     * Checks that each pattern is valid, splits it into segments and checks that each segment
     * contains only valid wildcards.
     *
     * @return list of segment arrays
     */
    private fun checkAndSplitPatterns(patterns: Collection<String>): List<List<String>> {
      return patterns.map { checkAndSplitPattern(it) }
    }

    private fun checkAndSplitPattern(pattern: String): List<String> {
      val error = GlobPatternValidator.validate(pattern)
      if (error != null)
        throw IllegalArgumentException(error)

      return pattern.split('/')
    }

    /** Calls [matches(pattern, str, null)][.matches]  */
    fun partMatches(pattern: String, str: String): Boolean =
        partMatches(pattern, str, null)

    fun patternMatches(
      patternParts: List<String>,
      path: String,
      excludeDirectories: Boolean,
      patternCache: Cache<String, Pattern>?
    ): Boolean {
      val pathPart = path.substringBefore("/")
      val pathTail = path.substringAfter("/", "")
      val isDirectory = pathTail.isNotEmpty()

      if (patternParts.isEmpty()) { // Base case.
        if (!(excludeDirectories && isDirectory)) {
          return true
        }
        return false
      }

      //if (!isDirectory) {
      //  // Nothing to find here.
      //  return false
      //}

      val pattern = patternParts.first()

      // ** is special: it can match nothing at all.
      // For example, x/** matches x, **/y matches y, and x/**/y matches x/y.
      if ("**" == pattern) {
        if (patternMatches(patternParts.tail(), path, excludeDirectories, patternCache))
          return true
      }

      if (!pattern.contains("*") && !pattern.contains("?")) {
        if (pathPart != pattern)
          return false

        patternMatches(patternParts.tail(), pathTail, excludeDirectories, patternCache)
      }

      if ("**" == pattern && isDirectory) {
        // Recurse without shifting the pattern.
        if (patternMatches(patternParts, pathTail, excludeDirectories, patternCache))
          return true
      }

      if (partMatches(pattern, pathPart, patternCache)) {
        // Recurse and consume one segment of the pattern.
        if (isDirectory) {
          if (patternMatches(patternParts.tail(), pathTail, excludeDirectories, patternCache))
            return true
        }
        else {
          if (patternParts.size == 1)
            return true
        }
      }

      return false
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
    private fun partMatches(
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
        when (val c = pattern[i]) {
          '*' -> {
            var toIncrement = 0
            if (len > i + 1 && pattern[i + 1] == '*') {
              // The pattern '**' is interpreted to match 0 or more directory separators, not 1 or
              // more. We skip the next * and then find a trailing/leading '/' and get rid of it.
              toIncrement = 1
              if (len > i + 2 && pattern[i + 2] == '/') {
                // We have '**/' -- skip the '/'.
                toIncrement = 2
              }
              else if (len == i + 2 && i > 0 && pattern[i - 1] == '/') {
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

    fun createCache(): Cache<String, Pattern> =
      CacheBuilder
        .newBuilder()
        .build(
          object : CacheLoader<String, Pattern>() {
            override fun load(wildcard: String): Pattern = makePatternFromWildcard(wildcard)
          },
        )

  }
}
