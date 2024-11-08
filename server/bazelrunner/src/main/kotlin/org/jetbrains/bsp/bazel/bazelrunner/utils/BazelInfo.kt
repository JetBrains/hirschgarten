package org.jetbrains.bsp.bazel.bazelrunner.utils

import java.nio.file.Path
import kotlin.io.path.isReadable
import kotlin.io.path.readText

interface BazelInfo {
  val execRoot: String
  val outputBase: Path
  val workspaceRoot: Path
  val release: BazelRelease
  val isBzlModEnabled: Boolean
  val isWorkspaceEnabled: Boolean
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

data class BasicBazelInfo(
  override val execRoot: String,
  override val outputBase: Path,
  override val workspaceRoot: Path,
  override val release: BazelRelease,
  override val isBzlModEnabled: Boolean,
  override val isWorkspaceEnabled: Boolean,
) : BazelInfo
