package org.jetbrains.bazel.sync.task

import io.kotest.common.runBlocking
import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.impl.flow.sync.DisabledTestProjectPostSyncHook
import org.jetbrains.bazel.impl.flow.sync.DisabledTestProjectPreSyncHook
import org.jetbrains.bazel.impl.flow.sync.DisabledTestProjectSyncHook
import org.jetbrains.bazel.impl.flow.sync.TestProjectPostSyncHook
import org.jetbrains.bazel.impl.flow.sync.TestProjectPreSyncHook
import org.jetbrains.bazel.impl.flow.sync.TestProjectSyncHook
import org.jetbrains.bazel.server.connection.BspConnection
import org.jetbrains.bazel.server.connection.setMockTestConnection
import org.jetbrains.bazel.sync.ProjectPostSyncHook
import org.jetbrains.bazel.sync.ProjectPreSyncHook
import org.jetbrains.bazel.sync.ProjectSyncHook
import org.jetbrains.bazel.sync.scope.SecondPhaseSync
import org.jetbrains.bazel.workspace.model.test.framework.BuildServerMock
import org.jetbrains.bazel.workspace.model.test.framework.MockProjectBaseTest
import org.jetbrains.bsp.protocol.JoinedBuildServer
import org.jetbrains.bsp.protocol.ResourcesResult
import org.jetbrains.bsp.protocol.SourcesResult
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetsResult
import org.jetbrains.bsp.protocol.WorkspaceDirectoriesResult
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

private val mockBuildServer =
  BuildServerMock(
    workspaceBuildTargetsResult = WorkspaceBuildTargetsResult(emptyList()),
    sourcesResult = SourcesResult(emptyList()),
    resourcesResult = ResourcesResult(emptyList()),
    workspaceDirectoriesResult = WorkspaceDirectoriesResult(emptyList(), emptyList()),
  )

private class BspConnectionMock : BspConnection {
  override suspend fun connect() {
    // it's a mock, nothing to do
  }

  override suspend fun disconnect() {
    // it's a mock, nothing to do
  }

  override suspend fun <T> runWithServer(task: suspend (server: JoinedBuildServer) -> T): T = task(mockBuildServer)

  override fun isConnected(): Boolean = true
}

@DisplayName("ProjectSyncTask tests")
class ProjectSyncTaskTest : MockProjectBaseTest() {
  @Test
  fun `should call all enabled pre-sync, sync and post-sync hooks`() {
    // given
    project.setMockTestConnection(BspConnectionMock())

    // pre-sync hooks
    val preSyncHook = TestProjectPreSyncHook()
    ProjectPreSyncHook.ep.registerExtension(preSyncHook)
    val disabledPreSyncHook = DisabledTestProjectPreSyncHook()
    ProjectPreSyncHook.ep.registerExtension(disabledPreSyncHook)

    // sync hooks
    val syncHook = TestProjectSyncHook()
    ProjectSyncHook.ep.registerExtension(syncHook)
    val disabledSyncHook = DisabledTestProjectSyncHook()
    ProjectSyncHook.ep.registerExtension(disabledSyncHook)

    // post-sync hooks
    val postSyncHook = TestProjectPostSyncHook()
    ProjectPostSyncHook.ep.registerExtension(postSyncHook)
    val disabledPostSyncHook = DisabledTestProjectPostSyncHook()
    ProjectPostSyncHook.ep.registerExtension(disabledPostSyncHook)

    // when
    runBlocking {
      ProjectSyncTask(project).sync(syncScope = SecondPhaseSync, false)
    }

    // then
    preSyncHook.wasCalled shouldBe true
    disabledPreSyncHook.wasCalled shouldBe false

    syncHook.wasCalled shouldBe true
    disabledSyncHook.wasCalled shouldBe false

    postSyncHook.wasCalled shouldBe true
    disabledPostSyncHook.wasCalled shouldBe false
  }
}
