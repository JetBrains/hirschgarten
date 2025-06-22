package org.jetbrains.bazel.languages.bazelversion.psi

import com.intellij.util.text.SemVer

/**
  Represents bazel version placed inside .bazelversion file:
 ```
  <fork>/<version>
 ```
 in case ``<fork>`` is not present version is pulled from official google bazel repository
 */
data class BazelVersionLiteral(val fork: String?, val version: SemVer) {
  override fun toString(): String {
    if (fork.isNullOrBlank()) {
      return "$version"
    }
    return "$fork/$version"
  }
}

val BAZEL_VERSION_LITERAL_REGEX = """(?:([^/]+)/)?(.+)""".toRegex()

fun String.toBazelVersionLiteral(): BazelVersionLiteral? {
  val match = BAZEL_VERSION_LITERAL_REGEX.matchEntire(trim()) ?: return null
  val (fork, version) = match.destructured
  val semver = SemVer.parseFromText(version) ?: return null
  return BazelVersionLiteral(fork.ifBlank { null }, semver)
}
