package org.jetbrains.bazel.sync.task

import com.intellij.testFramework.registerOrReplaceServiceInstance
import io.kotest.common.runBlocking
import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.impl.flow.sync.DisabledTestProjectPostSyncHook
import org.jetbrains.bazel.impl.flow.sync.DisabledTestProjectPreSyncHook
import org.jetbrains.bazel.impl.flow.sync.DisabledTestProjectSyncHook
import org.jetbrains.bazel.impl.flow.sync.TestProjectPostSyncHook
import org.jetbrains.bazel.impl.flow.sync.TestProjectPreSyncHook
import org.jetbrains.bazel.impl.flow.sync.TestProjectSyncHook
import org.jetbrains.bazel.server.connection.BazelServerService
import org.jetbrains.bazel.sync.ProjectPostSyncHook
import org.jetbrains.bazel.sync.ProjectPreSyncHook
import org.jetbrains.bazel.sync.ProjectSyncHook
import org.jetbrains.bazel.sync.scope.SecondPhaseSync
import org.jetbrains.bazel.workspace.model.test.framework.BazelServerServiceMock
import org.jetbrains.bazel.workspace.model.test.framework.MockProjectBaseTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("ProjectSyncTask tests")
class ProjectSyncTaskTest : MockProjectBaseTest() {
  // Hooks for testing
  private lateinit var preSyncHook: TestProjectPreSyncHook
  private lateinit var disabledPreSyncHook: DisabledTestProjectPreSyncHook
  private lateinit var syncHook: TestProjectSyncHook
  private lateinit var disabledSyncHook: DisabledTestProjectSyncHook
  private lateinit var postSyncHook: TestProjectPostSyncHook
  private lateinit var disabledPostSyncHook: DisabledTestProjectPostSyncHook

  @BeforeEach
  fun setUp() {
    // Set up server mock
    project.registerOrReplaceServiceInstance(BazelServerService::class.java, BazelServerServiceMock(), disposable)

    // Set up pre-sync hooks
    preSyncHook = TestProjectPreSyncHook()
    ProjectPreSyncHook.ep.registerExtension(preSyncHook)
    disabledPreSyncHook = DisabledTestProjectPreSyncHook()
    ProjectPreSyncHook.ep.registerExtension(disabledPreSyncHook)

    // Set up sync hooks
    syncHook = TestProjectSyncHook()
    ProjectSyncHook.ep.registerExtension(syncHook)
    disabledSyncHook = DisabledTestProjectSyncHook()
    ProjectSyncHook.ep.registerExtension(disabledSyncHook)

    // Set up post-sync hooks
    postSyncHook = TestProjectPostSyncHook()
    ProjectPostSyncHook.ep.registerExtension(postSyncHook)
    disabledPostSyncHook = DisabledTestProjectPostSyncHook()
    ProjectPostSyncHook.ep.registerExtension(disabledPostSyncHook)
  }

  @Test
  fun `should call all enabled pre-sync, sync and post-sync hooks`() {
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

  @Test
  fun `should call only post-sync hooks when runPostSyncHooks is called`() {
    // when
    runBlocking {
      ProjectSyncTask(project).runPostSyncHooks()
    }

    // then
    // Pre-sync hooks should not be called
    preSyncHook.wasCalled shouldBe false
    disabledPreSyncHook.wasCalled shouldBe false

    // Sync hooks should not be called
    syncHook.wasCalled shouldBe false
    disabledSyncHook.wasCalled shouldBe false

    // Only enabled post-sync hooks should be called
    postSyncHook.wasCalled shouldBe true
    disabledPostSyncHook.wasCalled shouldBe false
  }
}
