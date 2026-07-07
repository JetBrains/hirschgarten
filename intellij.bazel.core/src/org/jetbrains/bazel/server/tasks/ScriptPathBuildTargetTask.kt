package org.jetbrains.bazel.server.tasks

import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.util.system.OS
import org.jetbrains.annotations.ApiStatus
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
    val scriptPathParams = listOf("--script_path=$scriptPath", "--test_sharding_strategy=disabled")
    val params = RunParams(
      taskId = taskId,
      target = targetIds.single(),
      arguments = programArguments,
      additionalBazelParams = (scriptPathParams + additionalBazelParams).joinToString(" "),
      environmentVariables = null,
      checkVisibility = true,
    )
    return server.buildTargetRun(params).statusCode
  }

  companion object {
    fun createTempScriptFile(): Path {
      // on Windows, the only way to have an executable script is to have a
      // .bat file, but on unix the extension doesn't matter
      val suffix = if (OS.CURRENT == OS.Windows) ".bat" else ""
      return Files.createTempFile(Paths.get(FileUtilRt.getTempDirectory()), "bazel-script-", suffix)
        .also { it.toFile().deleteOnExit() }
    }
  }
}
