package org.jetbrains.bazel.languages.starlark.globbing

import com.google.common.base.Splitter
import com.google.common.base.Throwables
import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.collect.Lists
import com.google.common.collect.Ordering
import com.google.common.collect.Sets
import com.google.common.util.concurrent.ForwardingListenableFuture
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.isFile
import java.io.IOException
import java.util.concurrent.ExecutionException
import java.util.concurrent.Future
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Predicate
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException
import kotlin.concurrent.Volatile

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
  @Throws(IOException::class, InterruptedException::class)
  private fun globInternal(
    base: VirtualFile,
    patterns: MutableCollection<String>,
    excludeDirectories: Boolean,
    dirPred: Predicate<VirtualFile>,
    threadPool: ThreadPoolExecutor?,
  ): Set<VirtualFile> {
    val visitor = if (threadPool == null) GlobVisitor() else GlobVisitor(threadPool)
    return visitor.glob(base, patterns, excludeDirectories, dirPred)
  }

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
    private var threadPool: ThreadPoolExecutor? = null

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
     * Sets the threadpool to use for parallel glob evaluation. If unset, evaluation is done
     * in-thread.
     */
    @Suppress("unused")
    fun setThreadPool(pool: ThreadPoolExecutor?): Builder {
      this.threadPool = pool
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
     *
     * @throws InterruptedException if the thread is interrupted.
     */
    @Throws(IOException::class, InterruptedException::class)
    fun glob(): List<VirtualFile> {
      val included = globInternal(base, patterns, excludeDirectories, pathFilter, threadPool)
      val excluded = globInternal(base, excludes, excludeDirectories, pathFilter, threadPool)
      val files = included - excluded
      return Ordering
        .from(Comparator.comparing(VirtualFile::getPath))
        .immutableSortedCopy<VirtualFile>(files.toList())
    }
  }

  /** Adapts the result of the glob visitation as a Future.  */
  private class GlobFuture(private val visitor: GlobVisitor) : ForwardingListenableFuture<MutableSet<VirtualFile>>() {
    private val delegate: SettableFuture<MutableSet<VirtualFile>> = SettableFuture.create()

    @Throws(InterruptedException::class, ExecutionException::class)
    override fun get(): MutableSet<VirtualFile> = super.get()

    override fun delegate(): ListenableFuture<MutableSet<VirtualFile>> = delegate

    fun setException(exception: IOException) {
      delegate.setException(exception)
    }

    fun set(paths: MutableSet<VirtualFile>) {
      delegate.set(paths)
    }

    override fun cancel(mayInterruptIfRunning: Boolean): Boolean {
      // Best-effort interrupt of the in-flight visitation.
      visitor.cancel()
      return true
    }

    fun markCanceled() {
      super.cancel(true)
    }
  }

  /**
   * GlobVisitor executes a glob using parallelism, which is useful when the glob() requires many
   * readdir() calls on high latency filesystems.
   */
  private class GlobVisitor(private val executor: ThreadPoolExecutor? = null) {
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

    private val result: GlobFuture = GlobFuture(this)
    private val pendingOps = AtomicLong(0)
    private val failure = AtomicReference<IOException?>()

    @Volatile
    private var canceled = false

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
    @Throws(IOException::class, InterruptedException::class)
    fun glob(
      base: VirtualFile,
      patterns: Collection<String>,
      excludeDirectories: Boolean,
      dirPred: Predicate<VirtualFile>,
    ): Set<VirtualFile> {
      try {
        return globAsync(base, patterns, excludeDirectories, dirPred).get()!!
      } catch (e: ExecutionException) {
        val cause = e.cause
        if (cause != null) {
          Throwables.throwIfInstanceOf(cause, IOException::class.java)
        }
        throw RuntimeException(e)
      }
    }

    @Throws(IOException::class)
    fun globAsync(
      base: VirtualFile,
      patterns: Collection<String>,
      excludeDirectories: Boolean,
      dirPred: Predicate<VirtualFile>,
    ): Future<MutableSet<VirtualFile>> {
      if (!base.exists() || patterns.isEmpty()) {
        return Futures.immediateFuture(mutableSetOf())
      }
      val baseIsDirectory = base.isDirectory

      // We do a dumb loop, even though it will likely duplicate work
      // (e.g., readdir calls). In order to optimize, we would need
      // to keep track of which patterns shared sub-patterns and which did not
      // (for example consider the glob [*/*.java, sub/*.java, */*.txt]).
      pendingOps.incrementAndGet()
      try {
        for (splitPattern in checkAndSplitPatterns(patterns)) {
          queueGlob(
            base,
            baseIsDirectory,
            splitPattern,
            0,
            excludeDirectories,
            results,
            cache,
            dirPred,
          )
        }
      } finally {
        decrementAndCheckDone()
      }

      return result
    }

    @Throws(IOException::class)
    fun queueGlob(
      base: VirtualFile,
      baseIsDirectory: Boolean,
      patternParts: Array<String>,
      idx: Int,
      excludeDirectories: Boolean,
      results: MutableCollection<VirtualFile>,
      cache: Cache<String, Pattern>,
      dirPred: Predicate<VirtualFile>,
    ) {
      enqueue {
        try {
          reallyGlob(
            base,
            baseIsDirectory,
            patternParts,
            idx,
            excludeDirectories,
            results,
            cache,
            dirPred,
          )
        } catch (e: IOException) {
          failure.set(e)
        }
      }
    }

    fun enqueue(r: Runnable) {
      pendingOps.incrementAndGet()

      val wrapped =
        Runnable {
          try {
            if (!canceled && failure.get() == null) {
              r.run()
            }
          } finally {
            decrementAndCheckDone()
          }
        }

      if (executor == null) {
        wrapped.run()
      } else {
        executor.execute(wrapped)
      }
    }

    fun cancel() {
      this.canceled = true
    }

    fun decrementAndCheckDone() {
      if (pendingOps.decrementAndGet() == 0L) {
        // We get to 0 iff we are done all the relevant work. This is because we always increment
        // the pending ops count as we're enqueuing, and don't decrement until the task is complete
        // (which includes accounting for any additional tasks that one enqueues).
        if (canceled) {
          result.markCanceled()
        } else if (failure.get() != null) {
          result.setException(failure.get()!!)
        } else {
          result.set(results)
        }
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
    @Throws(IOException::class)
    fun reallyGlob(
      base: VirtualFile,
      baseIsDirectory: Boolean,
      patternParts: Array<String>,
      idx: Int,
      excludeDirectories: Boolean,
      results: MutableCollection<VirtualFile>,
      cache: Cache<String, Pattern>,
      dirPred: Predicate<VirtualFile>,
    ) {
      ProgressManager.checkCanceled()
      if (baseIsDirectory && !dirPred.test(base)) {
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
        queueGlob(
          base,
          baseIsDirectory,
          patternParts,
          idx + 1,
          excludeDirectories,
          results,
          cache,
          dirPred,
        )
      }

      if (!pattern.contains("*") && !pattern.contains("?")) {
        val child = base.findChild(pattern)
        if (child == null) return
        val childIsDir = child.isDirectory
        if (!childIsDir && !child.isFile) {
          // The file is a dangling symlink, fifo, does not exist, etc.
          return
        }

        queueGlob(
          child,
          childIsDir,
          patternParts,
          idx + 1,
          excludeDirectories,
          results,
          cache,
          dirPred,
        )
        return
      }

      val children = getChildren(base)
      if (children == null) {
        return
      }
      for (child in children) {
        val childIsDir = child.isDirectory

        if ("**" == pattern) {
          // Recurse without shifting the pattern.
          if (childIsDir) {
            queueGlob(
              child,
              childIsDir,
              patternParts,
              idx,
              excludeDirectories,
              results,
              cache,
              dirPred,
            )
          }
        }
        if (matches(pattern, child.name, cache)) {
          // Recurse and consume one segment of the pattern.
          if (childIsDir) {
            queueGlob(
              child,
              childIsDir,
              patternParts,
              idx + 1,
              excludeDirectories,
              results,
              cache,
              dirPred,
            )
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
