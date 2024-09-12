package org.jetbrains.plugins.bsp.impl.flow.sync

import ch.epfl.scala.bsp4j.ResourcesResult
import ch.epfl.scala.bsp4j.SourcesResult
import ch.epfl.scala.bsp4j.WorkspaceBuildTargetsResult
import io.kotest.common.runBlocking
import io.kotest.matchers.shouldBe
import org.jetbrains.bsp.protocol.BazelBuildServerCapabilities
import org.jetbrains.bsp.protocol.JoinedBuildServer
import org.jetbrains.plugins.bsp.config.BuildToolId
import org.jetbrains.plugins.bsp.config.bspBuildToolId
import org.jetbrains.plugins.bsp.config.buildToolId
import org.jetbrains.plugins.bsp.impl.server.connection.BspConnection
import org.jetbrains.plugins.bsp.impl.server.connection.setMockTestConnection
import org.jetbrains.workspace.model.test.framework.BuildServerMock
import org.jetbrains.workspace.model.test.framework.MockProjectBaseTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

private val mockBuildServer =
  BuildServerMock(
    workspaceBuildTargetsResult = WorkspaceBuildTargetsResult(emptyList()),
    sourcesResult = SourcesResult(emptyList()),
    resourcesResult = ResourcesResult(emptyList()),
  )

private class BspConnectionMock : BspConnection {
  override suspend fun connect() {
    // it's a mock, nothing to do
  }

  override fun disconnect() {
    // it's a mock, nothing to do
  }

  override suspend fun <T> runWithServer(task: suspend (server: JoinedBuildServer, capabilities: BazelBuildServerCapabilities) -> T): T =
    task(mockBuildServer, BazelBuildServerCapabilities())

  override fun isConnected(): Boolean = true
}

private val testBuildToolId = BuildToolId("test-build-tool")
private val anotherTestBuildToolId = BuildToolId("another-test-build-tool")

@DisplayName("ProjectSyncTask tests")
class ProjectSyncTaskTest : MockProjectBaseTest() {
  @Test
  fun `should call all enabled pre-sync, sync and post-sync hooks for bsp project`() {
    // given
    project.buildToolId = bspBuildToolId
    project.setMockTestConnection(BspConnectionMock())

    // pre-sync hooks
    val preSyncHook = TestProjectPreSyncHook(bspBuildToolId)
    ProjectPreSyncHook.ep.registerExtension(preSyncHook)
    val disabledPreSyncHook = DisabledTestProjectPreSyncHook(bspBuildToolId)
    ProjectPreSyncHook.ep.registerExtension(disabledPreSyncHook)

    // sync hooks
    val syncHook = TestProjectSyncHook(bspBuildToolId)
    ProjectSyncHook.ep.registerExtension(syncHook)
    val disabledSyncHook = DisabledTestProjectSyncHook(bspBuildToolId)
    ProjectSyncHook.ep.registerExtension(disabledSyncHook)

    // post-sync hooks
    val postSyncHook = TestProjectPostSyncHook(bspBuildToolId)
    ProjectPostSyncHook.ep.registerExtension(postSyncHook)
    val disabledPostSyncHook = DisabledTestProjectPostSyncHook(bspBuildToolId)
    ProjectPostSyncHook.ep.registerExtension(disabledPostSyncHook)

    // when
    runBlocking {
      ProjectSyncTask(project).sync(false)
    }

    // then
    preSyncHook.wasCalled shouldBe true
    disabledPreSyncHook.wasCalled shouldBe false

    syncHook.wasCalled shouldBe true
    disabledSyncHook.wasCalled shouldBe false

    postSyncHook.wasCalled shouldBe true
    disabledPostSyncHook.wasCalled shouldBe false
  }

  @Test
  fun `should call all enabled pre-sync, sync and post-sync hooks for non-bsp project`() {
    // given
    project.buildToolId = testBuildToolId
    project.setMockTestConnection(BspConnectionMock())

    // pre-sync hooks
    val defaultPreSyncHook = TestProjectPreSyncHook(bspBuildToolId)
    ProjectPreSyncHook.ep.registerExtension(defaultPreSyncHook)
    val additionalPreSyncHook = TestProjectPreSyncHook(testBuildToolId)
    ProjectPreSyncHook.ep.registerExtension(additionalPreSyncHook)
    val thisPreSyncHookShouldNotBeCalled = TestProjectPreSyncHook(anotherTestBuildToolId)
    ProjectPreSyncHook.ep.registerExtension(thisPreSyncHookShouldNotBeCalled)

    val defaultDisabledPreSyncHook = DisabledTestProjectPreSyncHook(bspBuildToolId)
    ProjectPreSyncHook.ep.registerExtension(defaultDisabledPreSyncHook)
    val additionalDisabledPreSyncHook = DisabledTestProjectPreSyncHook(testBuildToolId)
    ProjectPreSyncHook.ep.registerExtension(additionalDisabledPreSyncHook)

    // sync hooks
    val defaultSyncHook = TestProjectSyncHook(bspBuildToolId)
    ProjectSyncHook.ep.registerExtension(defaultSyncHook)
    val additionalDefaultSyncHook = TestProjectSyncHook(testBuildToolId)
    ProjectSyncHook.ep.registerExtension(additionalDefaultSyncHook)
    val thisSyncHookShouldNotBeCalled = TestProjectSyncHook(anotherTestBuildToolId)
    ProjectSyncHook.ep.registerExtension(thisSyncHookShouldNotBeCalled)

    val defaultDisabledSyncHook = DisabledTestProjectSyncHook(bspBuildToolId)
    ProjectSyncHook.ep.registerExtension(defaultDisabledSyncHook)
    val additionalDisabledSyncHook = DisabledTestProjectSyncHook(testBuildToolId)
    ProjectSyncHook.ep.registerExtension(additionalDisabledSyncHook)

    // post-sync hooks
    val defaultPostSyncHook = TestProjectPostSyncHook(bspBuildToolId)
    ProjectPostSyncHook.ep.registerExtension(defaultPostSyncHook)
    val additionalPostSyncHook = TestProjectPostSyncHook(testBuildToolId)
    ProjectPostSyncHook.ep.registerExtension(additionalPostSyncHook)
    val thisPostSyncHookShouldNotBeCalled = TestProjectPostSyncHook(anotherTestBuildToolId)
    ProjectPostSyncHook.ep.registerExtension(thisPostSyncHookShouldNotBeCalled)

    val defaultDisabledPostSyncHook = DisabledTestProjectPostSyncHook(bspBuildToolId)
    ProjectPostSyncHook.ep.registerExtension(defaultDisabledPostSyncHook)
    val additionalDisabledPostSyncHook = DisabledTestProjectPostSyncHook(bspBuildToolId)
    ProjectPostSyncHook.ep.registerExtension(additionalDisabledPostSyncHook)

    // when
    runBlocking {
      ProjectSyncTask(project).sync(false)
    }

    // then
    // pre-sync hooks
    defaultPreSyncHook.wasCalled shouldBe true
    additionalPreSyncHook.wasCalled shouldBe true

    thisPreSyncHookShouldNotBeCalled.wasCalled shouldBe false
    defaultDisabledPreSyncHook.wasCalled shouldBe false
    additionalDisabledPreSyncHook.wasCalled shouldBe false

    // sync hooks
    defaultSyncHook.wasCalled shouldBe true
    additionalDefaultSyncHook.wasCalled shouldBe true

    thisSyncHookShouldNotBeCalled.wasCalled shouldBe false
    defaultDisabledSyncHook.wasCalled shouldBe false
    additionalDisabledSyncHook.wasCalled shouldBe false

    // post-sync hooks
    defaultPostSyncHook.wasCalled shouldBe true
    additionalPostSyncHook.wasCalled shouldBe true

    thisPostSyncHookShouldNotBeCalled.wasCalled shouldBe false
    defaultDisabledPostSyncHook.wasCalled shouldBe false
    additionalDisabledPostSyncHook.wasCalled shouldBe false
  }
}
