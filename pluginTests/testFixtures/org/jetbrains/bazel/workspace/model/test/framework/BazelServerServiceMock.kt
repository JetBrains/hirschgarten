package org.jetbrains.bazel.workspace.model.test.framework

import org.jetbrains.bazel.commons.ExcludableValue
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.server.connection.BazelServerConnection
import org.jetbrains.bazel.server.connection.BazelServerService
import org.jetbrains.bazel.workspacecontext.WorkspaceContext
import org.jetbrains.bsp.protocol.JoinedBuildServer
import kotlin.io.path.Path

val mockWorkspaceContext =
  WorkspaceContext(
    targets = listOf(ExcludableValue.included(Label.parse("//..."))),
    directories = listOf(ExcludableValue.included(Path("."))),
    buildFlags = emptyList(),
    syncFlags = emptyList(),
    debugFlags = emptyList(),
    bazelBinary = Path("bazel"),
    allowManualTargetsSync = true,
    importDepth = -1,
    enabledRules = emptyList(),
    ideJavaHomeOverride = Path("java_home"),
    shardSync = false,
    targetShardSize = 1000,
    shardingApproach = null,
    importRunConfigurations = emptyList(),
    gazelleTarget = null,
    indexAllFilesInDirectories = false,
    pythonCodeGeneratorRuleNames = emptyList(),
    importIjars = false,
    deriveInstrumentationFilterFromTargets = true,
    indexAdditionalFilesInDirectories = emptyList(),
    preferClassJarsOverSourcelessJars = true,
  )

class MockBuildServerService(server: JoinedBuildServer) : BazelServerService {
  override val connection: BazelServerConnection =
    object : BazelServerConnection {
      override suspend fun <T> runWithServer(task: suspend (server: JoinedBuildServer) -> T): T = task(server)
    }
}
