package org.jetbrains.bazel.languages.bazelversion.psi

import com.intellij.util.text.SemVer

sealed interface BazelVersionLiteral {
  data class Forked(val fork: String, val version: BazelVersionLiteral) : BazelVersionLiteral

  data class Specific(val version: SemVer) : BazelVersionLiteral

  data class Other(val version: String) : BazelVersionLiteral

  data class Latest(val offset: Int) : BazelVersionLiteral

  enum class Special : BazelVersionLiteral {
    LAST_GREEN,
    LAST_RC,
    ROLLING,
  }
}

fun BazelVersionLiteral.toBazelVersionStringLiteral(): String =
  when (this) {
    is BazelVersionLiteral.Forked -> "$fork/${version.toBazelVersionStringLiteral()}"
    is BazelVersionLiteral.Latest -> if (offset == 0) "latest" else "latest-$offset"
    is BazelVersionLiteral.Other -> version
    BazelVersionLiteral.Special.LAST_GREEN -> "last_green"
    BazelVersionLiteral.Special.LAST_RC -> "last_rc"
    BazelVersionLiteral.Special.ROLLING -> "rolling"
    is BazelVersionLiteral.Specific -> "$version"
  }

fun BazelVersionLiteral.toSemVer(): SemVer? =
  when (this) {
    is BazelVersionLiteral.Forked -> this.version.toSemVer()
    is BazelVersionLiteral.Other -> SemVer.parseFromText(version)
    is BazelVersionLiteral.Specific -> version
    else -> null
  }

fun BazelVersionLiteral.withNewVersionWhenPossible(newVersion: String): BazelVersionLiteral =
  when (this) {
    is BazelVersionLiteral.Forked -> BazelVersionLiteral.Forked(fork, version.withNewVersionWhenPossible(newVersion))
    is BazelVersionLiteral.Other -> newVersion.toBazelVersionLiteral() ?: this
    is BazelVersionLiteral.Specific -> newVersion.toBazelVersionLiteral() ?: this
    else -> this
  }

val BAZEL_FORK_VERSION_REGEX = """(?:([^/]+)/)?(.+)""".toRegex()
val BAZEL_VERSION_LATEST_REGEX = """latest(?:-(\d+))?""".toRegex()

fun String.toBazelVersionLiteral(): BazelVersionLiteral? {
  val forkMatch = BAZEL_FORK_VERSION_REGEX.matchEntire(trim()) ?: return null
  val (fork, version) = forkMatch.destructured
  if (fork.isNotBlank()) {
    return BazelVersionLiteral.Forked(fork, version.toBazelVersionLiteral() ?: return null)
  }

  val latestMatch = BAZEL_VERSION_LATEST_REGEX.matchEntire(version)
  if (latestMatch != null) {
    val (_, offset) = latestMatch.destructured
    return BazelVersionLiteral.Latest(offset.toIntOrNull() ?: return null)
  }

  when (version) {
    "last_green" -> return BazelVersionLiteral.Special.LAST_GREEN
    "last_rc" -> return BazelVersionLiteral.Special.LAST_RC
    "rolling" -> return BazelVersionLiteral.Special.ROLLING
    else -> {}
  }

  val semver =
    SemVer.parseFromText(version)
      ?: return BazelVersionLiteral.Other(version)
  return BazelVersionLiteral.Specific(semver)
}
