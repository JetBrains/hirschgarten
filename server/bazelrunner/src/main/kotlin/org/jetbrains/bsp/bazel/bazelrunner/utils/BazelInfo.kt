package org.jetbrains.bsp.bazel.bazelrunner.utils

import org.jetbrains.bazel.commons.constants.Constants.DOT_BAZELBSP_DIR_NAME
import java.nio.file.Path
import kotlin.io.path.isReadable
import kotlin.io.path.readText

data class BazelInfo(
  val execRoot: String,
  val outputBase: Path,
  val workspaceRoot: Path,
  val release: BazelRelease,
  val isBzlModEnabled: Boolean,
  val isWorkspaceEnabled: Boolean,
) {
  fun shouldUseInjectRepository(): Boolean = release.major >= 8

  fun dotBazelBsp(): Path = workspaceRoot.resolve(DOT_BAZELBSP_DIR_NAME)
}

data class BazelRelease(val major: Int) {
  companion object {
    fun fromReleaseString(versionString: String): BazelRelease? = VERSION_REGEX.find(versionString)?.toBazelRelease()

    fun fromBazelVersionFile(workspacePath: Path): BazelRelease? {
      val versionString =
        workspacePath
          .resolve(".bazelversion")
          .takeIf { it.isReadable() }
          ?.readText()
          .orEmpty()
      return BAZEL_VERSION_MAJOR_REGEX.find(versionString)?.toBazelRelease()
    }

    private fun MatchResult.toBazelRelease() = BazelRelease(value.toInt())

    internal val LATEST_SUPPORTED_MAJOR = BazelRelease(6)

    private val BAZEL_VERSION_MAJOR_REGEX = """^\d+""".toRegex()

    private val VERSION_REGEX = """(?<=release )\d+(?=[0-9.]*)""".toRegex()
  }
}

fun BazelRelease?.orLatestSupported() = this ?: BazelRelease.LATEST_SUPPORTED_MAJOR
