package org.jetbrains.bazel.install

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.constants.Constants
import org.jetbrains.bazel.server.bsp.utils.FileUtils.writeIfDifferent
import java.nio.file.FileAlreadyExistsException
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.deleteIfExists

@ApiStatus.Internal
class EnvironmentCreator(private val projectRootDir: Path) {
  fun create() = createDotBazelBsp()

  private fun createDotBazelBsp(): Path {
    val bazelBspDir = createDir(projectRootDir, Constants.DOT_BAZELBSP_DIR_NAME)
    createDotBazelBspFiles(bazelBspDir)
    return bazelBspDir
  }

  private fun createDotBazelBspFiles(dotBazelBspDir: Path) {
    createGitIgnoreFile(dotBazelBspDir)
    createEmptyBuildFile(dotBazelBspDir)
  }

  private fun createEmptyBuildFile(dotBazelBspDir: Path) {
    dotBazelBspDir.resolve(Constants.defaultBuildFileName())
      .writeIfDifferent("")
    dotBazelBspDir.resolve(Constants.WORKSPACE_FILE_NAME).deleteIfExists()
  }

  fun createGitIgnoreFile(dotBazelBspDir: Path) {
    dotBazelBspDir.resolve(".gitignore")
      .writeIfDifferent(
        """
        aspects
        .bazelproject
        .gitignore
        BUILD.bazel
        bazel_invocation_cache/output_base
      """.trimIndent(),
      )
  }

  private fun createDir(rootDir: Path, name: String): Path {
    val dir = rootDir.resolve(name)
    try {
      Files.createDirectories(dir)
    } catch (_: FileAlreadyExistsException) {
    }
    return dir
  }
}
