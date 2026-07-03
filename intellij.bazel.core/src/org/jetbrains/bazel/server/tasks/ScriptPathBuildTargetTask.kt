package org.jetbrains.bazel.server.tasks

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.BazelStatus
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.progress.TaskConsole
import org.jetbrains.bazel.server.BazelServerFacade
import org.jetbrains.bsp.protocol.RunParams
import org.jetbrains.bsp.protocol.TaskId
import java.nio.file.Path

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
}
