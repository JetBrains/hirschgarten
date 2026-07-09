package org.jetbrains.bazel.server.sync

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.bazel.bazelrunner.BazelRunner
import org.jetbrains.bazel.commons.BazelRelease
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceConfiguration
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceConfigurationId
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceConfigurationSummary
import org.jetbrains.bazel.workspacecontext.WorkspaceContext
import org.jetbrains.bsp.protocol.TaskId

// sentinel values from aspect
internal val FALLBACK_CONFIG = WorkspaceConfigurationId.of("00000f1")
internal val FALLBACK_EXEC_CONFIG = WorkspaceConfigurationId.of("00000f2")

internal val BazelRelease.isConfigurationSupportEnabled: Boolean
  get() {
    // this one is dictated by whenever bazel support `short_id` on `BuildConfigurationApi`
    val (major, minor) = this
    return when {
      major >= 9 -> true
      major == 8 && minor >= 5 -> true
      major == 7 && minor >= 7 -> true
      else -> false
    }
  }

// obtained through: `bazel config --dump_all --output=json`
@Serializable
internal data class BazelConfiguration(
  val skyKey: String,
  val configHash: String,
  val mnemonic: String,
  val isExec: Boolean,
  val fragments: List<BazelConfigurationFragment>,
  val fragmentOptions: List<BazelConfigurationFragmentOptions>,
)

@Serializable
internal data class BazelConfigurationFragment(
  val name: String,
  val fragmentOptions: List<String>,
)

@Serializable
internal data class BazelConfigurationFragmentOptions(
  val name: String,
  val options: Map<String, String>,
)

internal fun BazelConfiguration.toWorkspaceConfiguration(): WorkspaceConfiguration {
  val options = this.fragmentOptions.firstOrNull { it.name.contains("CoreOptions") }?.options
                ?: error("`CoreOptions` options not found")
  val cpu = options["cpu"] ?: error("missing `cpu` options")
  val mode = options["compilation_mode"] ?: error("missing `compilation_mode` option")
  val platformSuffix = options["platform_suffix"]

  val platformName = buildString {
    append(cpu)
    append("-")
    append(mode)

    if (!platformSuffix.isNullOrBlank() && platformSuffix != "null") {
      append("-").append(platformSuffix)
    }
  }

  return WorkspaceConfiguration(
    id = WorkspaceConfigurationId.of(configHash),
    summary = WorkspaceConfigurationSummary(
      hash = configHash,
      mnemonic = mnemonic,
      platformName = platformName,
      cpu = cpu,
      isTool = isExec,
    ),
    fragments = listOf(), // can be filled by parsing options
  )
}

internal suspend fun fetchConfigurationsFromAnalysisCache(
  workspaceContext: WorkspaceContext,
  runner: BazelRunner,
  taskId: TaskId?,
): Result<List<WorkspaceConfiguration>> {
  val command = runner.buildBazelCommand(workspaceContext) {
    config {
      options += listOf("--dump_all", "--output=json")
    }
  }
  val result = runner.runBazelCommand(command, taskId)
    .waitAndGetResult()

  return runCatching {
    val json = result.stdout.toString(charset = Charsets.UTF_8)
    Json.decodeFromString<List<BazelConfiguration>>(json)
      .map { it.toWorkspaceConfiguration() }
  }
}
