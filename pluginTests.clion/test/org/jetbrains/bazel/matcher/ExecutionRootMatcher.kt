package org.jetbrains.bazel.matcher

import io.kotest.matchers.Matcher
import io.kotest.matchers.be
import io.kotest.matchers.collections.containAll
import io.kotest.matchers.should
import org.jetbrains.bazel.clion.sync.ExecutionRootPath
import java.nio.file.Path

private const val BAZEL_OUT = "bazel-out"
private const val BAZEL_BIN = "bazel-bin"
private const val BAZEL_PATH_SEPARATOR = "/"

/**
 * Matches a collection of execution root relative paths, where paths in
 * bazel-bin (e.g. bazel-out/k8-fastbuild/bin) can be matched against the
 * symlinked named.
 */
private fun containExecutionRootPaths(expected: Collection<String>): Matcher<Collection<ExecutionRootPath>> {
  return containAll(expected.map(Path::of)).contramap { paths -> paths.map { it.path.resolveBazelBin() } }
}

internal fun Collection<ExecutionRootPath>.shouldContainExecutionRootPaths(vararg expected: String) {
  should(containExecutionRootPaths(expected.toList()))
}

private fun beExecutionRootPath(expected: String): Matcher<ExecutionRootPath> {
  return be(Path.of(expected)).contramap { it.path.resolveBazelBin() }
}

internal fun ExecutionRootPath.shouldBeExecutionRootPath(expected: String) {
  should(beExecutionRootPath(expected))
}

internal fun Path.resolveBazelBin(): Path {
  return Path.of(joinToString(BAZEL_PATH_SEPARATOR).resolveBazelBin())
}

internal fun String.resolveBazelBin(): String {
  val segments = split(BAZEL_PATH_SEPARATOR)
  if (segments.size < 3 || segments[0] != BAZEL_OUT || segments[2] != "bin") return this

  return (listOf(BAZEL_BIN) + segments.drop(3)).joinToString(BAZEL_PATH_SEPARATOR)
}
