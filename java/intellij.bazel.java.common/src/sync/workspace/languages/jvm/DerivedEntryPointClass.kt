package org.jetbrains.bazel.sync.workspace.languages.jvm

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.id
import org.jetbrains.bsp.protocol.toExecrootPath

/**
 * Returns the class that `rules_java` derives for [target] when `test_class` or `main_class` is not set.
 * Returns `null` when Bazel derives no class.
 *
 * This is a copy of [_primary_class](https://github.com/bazelbuild/rules_java/blob/9.9.0/java/common/rules/impl/java_helper.bzl#L91-L97)
 * in `rules_java`, and it must give the same result.
 */
@ApiStatus.Internal
fun rulesJavaPrimaryClass(target: BuildTarget): String? {
  val label = target.id
  val primaryFileName = "${label.targetName}.java"
  val primarySrc = target.sourceExecPaths().firstOrNull { it.substringAfterLast('/') == primaryFileName }
  if (primarySrc != null) {
    return javaClassName(primarySrc.removeSuffix(".java").split('/'))
  }
  return javaClassName(label.packagePath.pathSegments + label.targetName.split('/'))
}

/**
 * Returns the test class that `rules_kotlin` derives for the `kt_jvm_test` [target] when `test_class` is not set.
 * Returns `null` when Bazel derives no class.
 *
 * This is a copy of [kt_jvm_junit_test_impl](https://github.com/bazelbuild/rules_kotlin/blob/v2.4.21/kotlin/internal/jvm/impl.bzl#L422-L459)
 * in `rules_kotlin`, and it must give the same result.
 */
@ApiStatus.Internal
fun rulesKotlinTestClass(target: BuildTarget): String? {
  val label = target.id
  val packagePrefix = label.packagePath.pathSegments.joinToString("/") + "/"
  var testClass: String? = null
  for (src in target.sourceExecPaths()) {
    if (src.replace(packagePrefix, "").substringBefore('.') != label.targetName) continue
    val splitter = KOTLIN_SPLIT_STRINGS.firstOrNull { it in src } ?: continue
    testClass = src
      .substringAfter(splitter)
      .substringBefore('.')
      .replace('/', '.')
  }
  return testClass?.ifEmpty { null }
}

private fun BuildTarget.sourceExecPaths(): Sequence<String> = sources.getOutputLocations().map { it.toExecrootPath() }

private fun javaClassName(path: List<String>): String? {
  val segments = path.filter { it.isNotEmpty() }
  val rootIndex = BazelConventionalRoots.javaRootIndex(segments) ?: return null
  return segments.drop(rootIndex + 1).joinToString(".").ifEmpty { null }
}

private val KOTLIN_SPLIT_STRINGS = listOf("src/test/java/", "src/test/kotlin/", "javatests/", "kotlin/", "java/", "test/")
