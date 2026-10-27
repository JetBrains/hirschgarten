package org.jetbrains.bazel.sync.task

import com.intellij.internal.statistic.FUCollectorTestCase
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.commons.constants.Constants
import org.jetbrains.bazel.config.BazelExtBundle
import org.jetbrains.bazel.impl.flow.sync.DisabledTestProjectPostSyncHook
import org.jetbrains.bazel.impl.flow.sync.DisabledTestProjectPreSyncHook
import org.jetbrains.bazel.impl.flow.sync.DisabledTestProjectSyncHook
import org.jetbrains.bazel.impl.flow.sync.TestProjectPostSyncHook
import org.jetbrains.bazel.impl.flow.sync.TestProjectPreSyncHook
import org.jetbrains.bazel.impl.flow.sync.TestProjectSyncHook
import org.jetbrains.bazel.progress.ConsoleService
import org.jetbrains.bazel.progress.syncConsole
import org.jetbrains.bazel.progress.withSubtask
import org.jetbrains.bazel.sync.ProjectPostSyncHook
import org.jetbrains.bazel.sync.ProjectPreSyncHook
import org.jetbrains.bazel.sync.ProjectSyncAbortException
import org.jetbrains.bazel.sync.ProjectSyncHook
import org.jetbrains.bazel.sync.ProjectSyncScope
import org.jetbrains.bazel.sync.withSubtask
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.TestConsoleService
import org.jetbrains.bazel.test.framework.assertLastSyncSucceeded
import org.jetbrains.bazel.test.framework.installTestConsoleService
import org.jetbrains.bazel.test.framework.testproject.aspectSyncServer
import org.jetbrains.bazel.test.framework.testproject.countLoggedErrors
import org.jetbrains.bazel.test.framework.testproject.expectingSyncErrorLogged
import org.jetbrains.bazel.test.framework.testproject.failingBuildServer
import org.jetbrains.bazel.test.framework.testproject.runProjectSync
import org.jetbrains.bazel.test.framework.testproject.targetIdeInfo
import org.jetbrains.bazel.test.framework.testproject.testBazelProjectFixture
import org.jetbrains.bazel.test.framework.testproject.useProjectView
import org.jetbrains.bazel.test.framework.testproject.writeFile
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.util.concurrent.CancellationException

@BazelTestApplication
@DisplayName("ProjectSyncTask tests")
class ProjectSyncTaskTest {
  private val testBazel by testBazelProjectFixture()

  @Test
  fun `should call all enabled pre-sync, sync and post-sync hooks`() {
    // given
    testBazel.aspectSyncServer()

    // pre-sync hooks
    val preSyncHook = TestProjectPreSyncHook()
    ProjectPreSyncHook.ep.point.registerExtension(preSyncHook, testBazel.disposable)
    val disabledPreSyncHook = DisabledTestProjectPreSyncHook()
    ProjectPreSyncHook.ep.point.registerExtension(disabledPreSyncHook, testBazel.disposable)

    // sync hooks
    val syncHook = TestProjectSyncHook()
    ProjectSyncHook.ep.point.registerExtension(syncHook, testBazel.disposable)
    val disabledSyncHook = DisabledTestProjectSyncHook()
    ProjectSyncHook.ep.point.registerExtension(disabledSyncHook, testBazel.disposable)

    // post-sync hooks
    val postSyncHook = TestProjectPostSyncHook()
    ProjectPostSyncHook.ep.point.registerExtension(postSyncHook, testBazel.disposable)
    val disabledPostSyncHook = DisabledTestProjectPostSyncHook()
    ProjectPostSyncHook.ep.point.registerExtension(disabledPostSyncHook, testBazel.disposable)

    // when
    testBazel.runProjectSync()

    // then
    preSyncHook.wasCalled shouldBe true
    disabledPreSyncHook.wasCalled shouldBe false

    syncHook.wasCalled shouldBe true
    disabledSyncHook.wasCalled shouldBe false

    postSyncHook.wasCalled shouldBe true
    disabledPostSyncHook.wasCalled shouldBe false
  }

  @Test
  fun `should call post-sync hooks after failed sync without project model update`() {
    // given
    testBazel.aspectSyncServer(hasError = true)

    val postSyncHook = TestProjectPostSyncHook()
    ProjectPostSyncHook.ep.point.registerExtension(postSyncHook, testBazel.disposable)

    // when
    testBazel.runProjectSync()

    // then
    postSyncHook.wasCalled shouldBe true
    postSyncHook.projectModelUpdated shouldBe false
  }

  @Test
  fun `should call post-sync hooks after sync fails with an exception`() {
    // given a server that throws while resolving the workspace, i.e. sync fails mid-flight rather than resolving no targets
    val failure = IllegalStateException("boom while resolving workspace")
    testBazel.failingBuildServer(failure)

    val postSyncHook = TestProjectPostSyncHook()
    ProjectPostSyncHook.ep.point.registerExtension(postSyncHook, testBazel.disposable)

    // when (the sync failure is logged via log.error on purpose; don't let it fail the test)
    val events = expectingSyncErrorLogged {
      FUCollectorTestCase.collectLogEvents(testBazel.disposable) {
        testBazel.runProjectSync()
      }.filter { it.group.id == "bazel.sync" }.map { it.event }
    }

    // then post-sync hooks still run for cleanup, and the sync is reported as a failure (not "no targets")
    postSyncHook.wasCalled shouldBe true
    postSyncHook.projectModelUpdated shouldBe false

    val finished = events.singleOrNull { it.id == "sync.finished" }.shouldNotBeNull()
    finished.data["completion_result"] shouldBe "FAILURE"
  }

  @Test
  fun `should log FUS events for sync lifecycle timings and language counts`() {
    testBazel.aspectSyncServer(
      targetIdeInfo("//app:java", "java_library"),
      targetIdeInfo("//app:kotlin", "kt_jvm_library"),
    )

    val events = FUCollectorTestCase.collectLogEvents(testBazel.disposable) {
      testBazel.runProjectSync(ProjectSyncScope.Full(build = true, phased = false))
    }.filter { it.group.id == "bazel.sync" }.map { it.event }

    val started = events.singleOrNull { it.id == "sync.started" }.shouldNotBeNull()
    val finished = events.singleOrNull { it.id == "sync.finished" }.shouldNotBeNull()
    val phases = events.filter { it.id == "phase.finished" }
    val languages = events.filter { it.id == "language.discovered" }
    val activityId = started.data["ide_activity_id"]

    started.data["sync_phase"] shouldBe "SECOND_PHASE"
    started.data["build_project"] shouldBe true
    finished.data["ide_activity_id"] shouldBe activityId
    finished.data["completion_result"] shouldBe "SUCCESS"
    finished.data["resolved_target_count"] shouldBe 2
    ("duration_ms" in finished.data) shouldBe true

    phases.mapTo(mutableSetOf()) { it.data["phase"] } shouldBe setOf("COLLECT_PROJECT_DETAILS", "APPLY_PROJECT_MODEL")
    phases.forEach {
      it.data["ide_activity_id"] shouldBe activityId
      ("duration_ms" in it.data) shouldBe true
    }

    languages.associate { it.data["language"] to it.data["target_count"] } shouldBe mapOf(
      "java" to 1,
      "kotlin" to 1,
    )
    languages.forEach { it.data["ide_activity_id"] shouldBe activityId }
  }

  @Test
  fun `should stop the sync without an error log when a pre-sync hook throws ProjectSyncAbortException`() {
    // given
    testBazel.aspectSyncServer()
    ProjectPreSyncHook.ep.point.registerExtension(FailingProjectPreSyncHook(), testBazel.disposable)
    val nextPreSyncHook = TestProjectPreSyncHook()
    ProjectPreSyncHook.ep.point.registerExtension(nextPreSyncHook, testBazel.disposable)
    val syncHook = TestProjectSyncHook()
    ProjectSyncHook.ep.point.registerExtension(syncHook, testBazel.disposable)
    val postSyncHook = TestProjectPostSyncHook()
    ProjectPostSyncHook.ep.point.registerExtension(postSyncHook, testBazel.disposable)

    // when (the test logger fails the test if the sync logs an error)
    val events = FUCollectorTestCase.collectLogEvents(testBazel.disposable) {
      testBazel.runProjectSync()
    }.filter { it.group.id == "bazel.sync" }.map { it.event }

    // then
    nextPreSyncHook.wasCalled shouldBe false
    syncHook.wasCalled shouldBe false
    postSyncHook.wasCalled shouldBe true
    postSyncHook.projectModelUpdated shouldBe false

    val finished = events.singleOrNull { it.id == "sync.finished" }.shouldNotBeNull()
    finished.data["completion_result"] shouldBe "FAILURE"
  }

  @Test
  fun `should stop the sync without an error log when a required project view import has no file`() {
    val (syncHookCalled, completionResult) = syncWithProjectView("import missing.bazelproject")

    syncHookCalled shouldBe false
    completionResult shouldBe "FAILURE"
  }

  @Test
  fun `should stop the sync without an error log when a project view value is invalid`() {
    val (syncHookCalled, completionResult) = syncWithProjectView("shard_sync: not_a_boolean")

    syncHookCalled shouldBe false
    completionResult shouldBe "FAILURE"
  }

  @Test
  fun `should not stop the sync when the project view has only warnings`() {
    val (syncHookCalled, _) = syncWithProjectView("not_supported_section: someValue")

    syncHookCalled shouldBe true
  }

  /**
   * Syncs with [projectView] as the project view file, and returns whether the sync hooks ran and the completion result.
   * The test logger fails the test if the sync logs an error.
   */
  private fun syncWithProjectView(projectView: String): Pair<Boolean, Any?> {
    testBazel.useProjectView(testBazel.writeFile(Constants.DEFAULT_PROJECT_VIEW_FILE_NAME, projectView).toNioPath())
    testBazel.aspectSyncServer()
    val syncHook = TestProjectSyncHook()
    ProjectSyncHook.ep.point.registerExtension(syncHook, testBazel.disposable)

    val events = FUCollectorTestCase.collectLogEvents(testBazel.disposable) {
      testBazel.runProjectSync()
    }.filter { it.group.id == "bazel.sync" }.map { it.event }

    val finished = events.singleOrNull { it.id == "sync.finished" }.shouldNotBeNull()
    return syncHook.wasCalled to finished.data["completion_result"]
  }

  @ParameterizedTest(name = "the error is in a subtask: {0}")
  @ValueSource(booleans = [true, false])
  fun `should report a sync hook error once and continue the sync`(inSubtask: Boolean) {
    // given
    testBazel.aspectSyncServer()
    installTestConsoleService(testBazel.project, testBazel.disposable)
    ProjectSyncHook.ep.point.registerExtension(FailingProjectSyncHook(inSubtask), testBazel.disposable)
    val nextSyncHook = TestProjectSyncHook()
    ProjectSyncHook.ep.point.registerExtension(nextSyncHook, testBazel.disposable)

    // when
    val loggedErrors = countLoggedErrors(SYNC_HOOK_FAILURE) { testBazel.runProjectSync() }

    // then
    loggedErrors shouldBe 1
    syncDiagnostics().count { SYNC_HOOK_FAILURE in it } shouldBe 1
    nextSyncHook.wasCalled shouldBe true
    assertLastSyncSucceeded(testBazel.project)
  }

  @Test
  fun `should show a cancellation in a nested subtask once`() {
    // given
    testBazel.aspectSyncServer()
    installTestConsoleService(testBazel.project, testBazel.disposable)
    ProjectSyncHook.ep.point.registerExtension(CancellingProjectSyncHook(), testBazel.disposable)

    // when
    val loggedErrors = countLoggedErrors(SYNC_HOOK_CANCELLATION) { testBazel.runProjectSync() }

    // then
    val cancelled = BazelExtBundle.message("console.task.exception.cancellation.message")
    syncDiagnostics().count { it == cancelled } shouldBe 1
    loggedErrors shouldBe 0
  }

  @Test
  fun `should report a sync pipeline error once`() {
    // given a server that throws while the workspace is resolved, so the sync fails
    testBazel.failingBuildServer(IllegalStateException(SYNC_PIPELINE_FAILURE))
    installTestConsoleService(testBazel.project, testBazel.disposable)

    // when
    val loggedErrors = countLoggedErrors(SYNC_PIPELINE_FAILURE) { testBazel.runProjectSync() }

    // then the failed subtask shows the error, and the root task does not show it again
    loggedErrors shouldBe 1
    syncDiagnostics().count { SYNC_PIPELINE_FAILURE in it } shouldBe 1
  }

  private fun syncDiagnostics(): List<String> = (ConsoleService.getInstance(testBazel.project) as TestConsoleService).syncDiagnostics

}

private class FailingProjectPreSyncHook : ProjectPreSyncHook {
  override suspend fun onPreSync(environment: ProjectPreSyncHook.ProjectPreSyncHookEnvironment) {
    throw ProjectSyncAbortException()
  }
}

private const val SYNC_HOOK_FAILURE = "the sync hook fails"

private const val SYNC_HOOK_CANCELLATION = "the sync hook is cancelled"

private const val SYNC_PIPELINE_FAILURE = "the sync pipeline fails"

private class FailingProjectSyncHook(private val inSubtask: Boolean) : ProjectSyncHook {
  override suspend fun onSync(environment: ProjectSyncHook.ProjectSyncHookEnvironment) {
    if (inSubtask) {
      // the outer subtask must not show the error again
      environment.withSubtask("Outer step") { outerTaskId ->
        environment.project.syncConsole.withSubtask(outerTaskId.subTask("failing"), "Failing step") {
          throw IllegalStateException(SYNC_HOOK_FAILURE)
        }
      }
    }
    else {
      throw IllegalStateException(SYNC_HOOK_FAILURE)
    }
  }
}

private class CancellingProjectSyncHook : ProjectSyncHook {
  override suspend fun onSync(environment: ProjectSyncHook.ProjectSyncHookEnvironment) {
    environment.withSubtask("Outer step") { outerTaskId ->
      environment.project.syncConsole.withSubtask(outerTaskId.subTask("cancelled"), "Cancelled step") {
        throw CancellationException(SYNC_HOOK_CANCELLATION)
      }
    }
  }
}
