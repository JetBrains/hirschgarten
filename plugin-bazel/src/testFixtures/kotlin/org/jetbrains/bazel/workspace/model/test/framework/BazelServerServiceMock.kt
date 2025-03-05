package org.jetbrains.bazel.workspace.model.test.framework

import org.jetbrains.bazel.server.connection.BazelServerConnection
import org.jetbrains.bazel.server.connection.BazelServerService
import org.jetbrains.bsp.protocol.DependencySourcesResult
import org.jetbrains.bsp.protocol.JoinedBuildServer
import org.jetbrains.bsp.protocol.JvmBinaryJarsResult
import org.jetbrains.bsp.protocol.NonModuleTargetsResult
import org.jetbrains.bsp.protocol.ResourcesResult
import org.jetbrains.bsp.protocol.SourcesResult
import org.jetbrains.bsp.protocol.WorkspaceBazelRepoMappingResult
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetsResult
import org.jetbrains.bsp.protocol.WorkspaceDirectoriesResult
import org.jetbrains.bsp.protocol.WorkspaceInvalidTargetsResult
import org.jetbrains.bsp.protocol.WorkspaceLibrariesResult

private val mockBuildServer =
  BuildServerMock(
    workspaceBuildTargetsResult = WorkspaceBuildTargetsResult(emptyList()),
    sourcesResult = SourcesResult(emptyList()),
    resourcesResult = ResourcesResult(emptyList()),
    workspaceDirectoriesResult = WorkspaceDirectoriesResult(emptyList(), emptyList()),
    workspaceLibrariesResult = WorkspaceLibrariesResult(emptyList()),
    workspaceNonModuleTargetsResult = NonModuleTargetsResult(emptyList()),
    jvmBinaryJarsResult = JvmBinaryJarsResult(emptyList()),
    workspaceInvalidTargetsResult = WorkspaceInvalidTargetsResult(emptyList()),
    workspaceBazelRepoMappingResult = WorkspaceBazelRepoMappingResult(emptyMap(), emptyMap()),
    dependencySourcesResult = DependencySourcesResult(emptyList()),
  )

private class BazelServerConnectionMock : BazelServerConnection {
  override suspend fun <T> runWithServer(task: suspend (server: JoinedBuildServer) -> T): T = task(mockBuildServer)
}

class BazelServerServiceMock : BazelServerService {
  override val connection: BazelServerConnection = BazelServerConnectionMock()
}
