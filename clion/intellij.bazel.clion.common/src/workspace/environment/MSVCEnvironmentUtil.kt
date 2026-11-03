package org.jetbrains.bazel.clion.workspace.environment

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.Key
import com.intellij.testFramework.TestModeFlags
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.annotations.VisibleForTesting
import java.nio.file.InvalidPathException
import java.nio.file.Path
import java.util.regex.Pattern

@ApiStatus.Internal
object MSVCEnvironmentUtil {
  private val LOG = Logger.getInstance(MSVCEnvironmentUtil::class.java)

  /**
   * The environment variable used by bazel to find the Visual Studio directory.
   */
  const val BAZEL_VC_VAR: String = "BAZEL_VC"

  /**
   * Key for [TestModeFlags] to mock the value of BAZEL_VC.
   */
  @VisibleForTesting
  val BAZEL_VC_KEY: Key<String?> = Key("BAZEL_VC_ENV")

  /**
   * Pattern to find the Visual Studio directory from the absolute path of the compiler executable.
   */
  private val VC_DIRECTORY_PATTERN: Pattern = Pattern.compile(
    "(?<path>.*)/vc/tools/msvc/.*/bin/.*/cl\\.exe".replace("/", "\\\\"),
  )

  private fun getBazelVC(): String? = TestModeFlags.get(BAZEL_VC_KEY) ?: System.getenv(BAZEL_VC_VAR)

  /**
   * The MSVC toolset requires a home path. Which is the path to VisualStudio installation e.g.: `C:\Program
   * Files\Microsoft Visual Studio\2022\Community`
   *
   * This path can be derived from the BAZEL_VC environment variable which points to: `C:\Program Files\Microsoft Visual
   * Studio\2022\Community\VC`
   *
   * Or from the compiler path. Not sure what is more reliable, so let's do both.
   */
  fun getToolSetPath(compiler: Path?): String? {
    val bazelVC: String? = getBazelVC()

    // try to get the path from the BAZEL_VC environment variable
    if (bazelVC != null) {
      getToolSetPathFromBazelVC(bazelVC)?.let { return it }
    }

    // if BAZEL_VC is not set or invalid, try to derive the path from the compiler path
    val path = compiler?.toAbsolutePath()?.toString()?.lowercase() ?: return null
    val matcher = VC_DIRECTORY_PATTERN.matcher(path)
    if (!matcher.matches()) {
      return null
    }
    return matcher.group("path")
  }

  private fun getToolSetPathFromBazelVC(bazelVC: String): String? =
    try {
      val parent = Path.of(bazelVC).parent ?: return null
      parent.toString()
    }
    catch (e: InvalidPathException) {
      LOG.warn("Invalid path in BAZEL_VC environment variable.", e)
      null
    }
}
