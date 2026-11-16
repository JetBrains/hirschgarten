package org.jetbrains.bazel.server.tasks

import com.intellij.openapi.util.io.FileUtilRt
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.BazelHost
import org.jetbrains.bazel.commons.BazelStatus
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.progress.TaskConsole
import org.jetbrains.bazel.server.BazelServerFacade
import org.jetbrains.bsp.protocol.RunParams
import org.jetbrains.bsp.protocol.TaskId
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

@ApiStatus.Internal
class ScriptPathBuildTargetTask(
  private val scriptPath: Path,
  private val programArguments: List<String>,
  private val additionalBazelParams: List<String>,
) : BuildTargetTask {
  override suspend fun build(
    server: BazelServerFacade,
    targetIds: List<Label>,
    buildConsole: TaskConsole,
    taskId: TaskId,
    debugFlags: List<String>,
  ): BazelStatus {
    val params = RunParams(
      taskId = taskId,
      target = targetIds.single(),
      arguments = programArguments,
      additionalBazelParams = scriptPathParams(scriptPath) + debugFlags + additionalBazelParams,
      checkVisibility = true,
    )
    return server.buildTargetRun(params).statusCode
  }

  companion object {
    /** Returns the Bazel flags that make `bazel run` write a script for the target to [scriptPath] instead of running it. */
    fun scriptPathParams(scriptPath: Path): List<String> =
      // Bazel writes the script, so Bazel gets the path in the form of its own environment.
      listOf("--script_path=${BazelHost.of(scriptPath).toHostPath(scriptPath)}", "--test_sharding_strategy=disabled")

    /**
     * Creates the file for the script of `bazel run --script_path`.
     * Bazel writes the script, and then the IDE starts it in the environment of Bazel.
     * So the file is in a directory of [host], and the kind of the script follows the system of [host].
     */
    fun createTempScriptFile(host: BazelHost = BazelHost.LOCAL): Path {
      // on Windows, the only way to have an executable script is to have a
      // .bat file, but on unix the extension doesn't matter
      val suffix = if (host.isWindows) ".bat" else ""
      if (!host.isLocal) return host.createTempFile("bazel-script-", suffix)
      return Files.createTempFile(Paths.get(FileUtilRt.getTempDirectory()), "bazel-script-", suffix)
        .also { it.toFile().deleteOnExit() }
    }
  }
}
