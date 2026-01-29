package org.jetbrains.bazel.commons

import org.jetbrains.bazel.commons.constants.Constants.DOT_BAZELBSP_DIR_NAME
import java.nio.file.Path
import kotlin.io.path.isReadable
import kotlin.io.path.readText

data class BazelInfo(
  val execRoot: Path,
  val outputBase: Path,
  val workspaceRoot: Path,
  val bazelBin: Path,
  val release: BazelRelease,
  val isBzlModEnabled: Boolean,
  val isWorkspaceEnabled: Boolean,
  /**
   * https://bazel.build/reference/command-line-reference#flag--incompatible_autoload_externally
   * May include rule names and/or provider names.
   */
  val externalAutoloads: List<String>,
) {
  fun dotBazelBsp(): Path = workspaceRoot.resolve(DOT_BAZELBSP_DIR_NAME)
}

data class BazelRelease(val major: Int, val minor: Int = 0) {
  companion object {
    fun fromReleaseString(versionString: String): BazelRelease? = VERSION_REGEX.find(versionString)?.toBazelRelease()

    fun fromBazelVersionFile(workspacePath: Path): BazelRelease? {
      val versionString =
        workspacePath
          .resolve(".bazelversion")
          .takeIf { it.isReadable() }
          ?.readText()
          .orEmpty()
      return BAZEL_VERSION_REGEX.find(versionString)?.toBazelRelease()
    }

    private fun MatchResult.toBazelRelease() = BazelRelease(groupValues[1].toInt(), groupValues[2].toInt())

    private val BAZEL_VERSION_REGEX = """^(\d+)\.(\d+)""".toRegex()

    private val VERSION_REGEX = """(?<=release )(\d+)\.(\d+)(?=[0-9.]*)""".toRegex()

    internal val FALLBACK_VERSION = BazelRelease(7, 5)

    internal val OLDEST_SUPPORTED_MAJOR = 7

    internal val MINIMAL_MINOR_VERSION = mapOf(7 to 5)
  }

  /**
   * Return a string hinting at the appropriate minimal version to update to, if a deprecated bazel version is used. Returns null if the version is not deprecated.
   */
  fun deprecated(): String? {
    if (major < OLDEST_SUPPORTED_MAJOR) return "Bazel major version $major is deprecated; the oldest supported version is $OLDEST_SUPPORTED_MAJOR.${MINIMAL_MINOR_VERSION[OLDEST_SUPPORTED_MAJOR] ?: 0}."

    if (minor < (MINIMAL_MINOR_VERSION[major] ?: 0)) return "Bazel-$major versions older than $major.${MINIMAL_MINOR_VERSION[major] ?: 0} are unsupported."

    return null
  }
}

fun BazelRelease?.orFallbackVersion() = this ?: BazelRelease.FALLBACK_VERSION
