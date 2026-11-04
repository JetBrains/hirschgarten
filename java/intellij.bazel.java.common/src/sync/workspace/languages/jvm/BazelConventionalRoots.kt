package org.jetbrains.bazel.sync.workspace.languages.jvm

import org.jetbrains.annotations.ApiStatus

/**
 * Finds the conventional roots that `rules_java` uses when a target sets no explicit root.
 * The segments below a root give the Java class name or the resource path.
 *
 * Each function takes the segments of a path relative to the root of its repository.
 */
@ApiStatus.Internal
object BazelConventionalRoots {
  /**
   * Returns the index of the Java root in [segments], or `null` when [segments] have no Java root.
   *
   * Some examples:
   * - `java/com/foo/Foo.java` gives the index of `java`.
   * - `svc/src/main/java/com/foo/Foo.java` gives the index of `java`.
   * - `app/src/com/foo/Foo.java` gives the index of `src`.
   *
   * This is a copy of [_java_segments](https://github.com/bazelbuild/rules_java/blob/9.9.0/java/common/rules/java_helper.bzl#L22-L52)
   * in `rules_java`.
   */
  fun javaRootIndex(segments: List<String>): Int? {
    val firstRootIndex = segments.indexOfFirst { it in JAVA_ROOTS }.takeIf { it >= 0 } ?: return null
    val isFirstRootSrc = segments[firstRootIndex] == "src"
    // a `java`, `javatests` or `testsrc` root below other directories is final
    if (firstRootIndex != 0 && !isFirstRootSrc) return firstRootIndex

    // `src` and a root at the start can hold the real root deeper, so Bazel checks the first nested root below them
    val nestedRootIndex = (firstRootIndex + 1 until segments.lastIndex).firstOrNull { index ->
      when (segments[index]) {
        "src" -> true
        in NESTED_JAVA_ROOTS -> isFirstRootSrc
        else -> false
      }
    }
    // a Maven layout at a `src` root wins over a nested root
    val firstMavenRoot = when {
      isFirstRootSrc -> segments.mavenRootIndex(firstRootIndex)
      else -> null
    }
    if (nestedRootIndex == null) return firstMavenRoot ?: firstRootIndex

    return when {
      segments[nestedRootIndex + 1] in TOP_LEVEL_PACKAGES -> firstMavenRoot ?: nestedRootIndex
      segments[nestedRootIndex] == "src" -> segments.mavenRootIndex(nestedRootIndex) ?: firstRootIndex
      else -> firstMavenRoot ?: firstRootIndex
    }
  }

  /**
   * Returns the index of the resource root in [segments], or `null` when [segments] have no resource root.
   * The first `src/<dir>/resources` is the root. When no such directory exists, the Java root from [javaRootIndex] is the root.
   *
   * Some examples:
   * - `svc/src/main/resources/foo/a.txt` gives the index of `resources`.
   * - `svc/java/com/foo/a.txt` gives the index of `java`.
   *
   * This is a copy of `get_default_resource_path` in `rules_java`.
   * [_resource_mapper](https://github.com/bazelbuild/rules_java/blob/9.9.0/java/common/rules/java_helper.bzl) calls it.
   */
  fun javaResourceRootIndex(segments: List<String>): Int? {
    val srcResourcesIndex = (0 until segments.size - 2).firstOrNull { segments[it] == "src" && segments[it + 2] == "resources" }
    return srcResourcesIndex?.let { it + 2 } ?: javaRootIndex(segments)
  }

  /**
   * Returns the index of `java` or `resources` when [src] starts a Maven layout such as `src/main/java`.
   */
  private fun List<String>.mavenRootIndex(src: Int): Int? {
    val sourceSet = getOrNull(src + 1)
    val sourceRoot = getOrNull(src + 2)
    return if (sourceSet in MAVEN_SOURCE_SETS && sourceRoot in MAVEN_SOURCE_ROOTS) src + 2 else null
  }

  private val JAVA_ROOTS = setOf("java", "javatests", "src", "testsrc")
  private val NESTED_JAVA_ROOTS = setOf("java", "javatests")
  private val TOP_LEVEL_PACKAGES = setOf("com", "org", "net")
  private val MAVEN_SOURCE_SETS = setOf("main", "test")
  private val MAVEN_SOURCE_ROOTS = setOf("java", "resources")
}
