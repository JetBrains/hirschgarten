package org.jetbrains.bazel.sync.task

import com.intellij.testFramework.registerOrReplaceServiceInstance
import io.kotest.common.runBlocking
import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.bazelrunner.outputs.ProcessSpawner
import org.jetbrains.bazel.impl.flow.sync.DisabledTestProjectPostSyncHook
import org.jetbrains.bazel.impl.flow.sync.DisabledTestProjectPreSyncHook
import org.jetbrains.bazel.impl.flow.sync.DisabledTestProjectSyncHook
import org.jetbrains.bazel.impl.flow.sync.TestProjectPostSyncHook
import org.jetbrains.bazel.impl.flow.sync.TestProjectPreSyncHook
import org.jetbrains.bazel.impl.flow.sync.TestProjectSyncHook
import org.jetbrains.bazel.performance.telemetry.TelemetryManager
import org.jetbrains.bazel.server.connection.BazelServerService
import org.jetbrains.bazel.startup.GenericCommandLineProcessSpawner
import org.jetbrains.bazel.startup.IntellijTelemetryManager
import org.jetbrains.bazel.sync.ProjectPostSyncHook
import org.jetbrains.bazel.sync.ProjectPreSyncHook
import org.jetbrains.bazel.sync.ProjectSyncHook
import org.jetbrains.bazel.sync.scope.SecondPhaseSync
import org.jetbrains.bazel.workspace.model.test.framework.BazelServerServiceMock
import org.jetbrains.bazel.workspace.model.test.framework.MockProjectBaseTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("ProjectSyncTask tests")
class ProjectSyncTaskTest : MockProjectBaseTest() {
  @Test
  fun `should call all enabled pre-sync, sync and post-sync hooks`() {
    // given
    project.registerOrReplaceServiceInstance(BazelServerService::class.java, BazelServerServiceMock(), disposable)

    // Initialize ProcessSpawner and TelemetryManager
    ProcessSpawner.provideProcessSpawner(GenericCommandLineProcessSpawner)
    TelemetryManager.provideTelemetryManager(IntellijTelemetryManager)

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
