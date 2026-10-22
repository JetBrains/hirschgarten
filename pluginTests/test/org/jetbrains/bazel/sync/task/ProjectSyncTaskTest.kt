package org.jetbrains.bazel.sync.task

import com.intellij.internal.statistic.FUCollectorTestCase
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.impl.flow.sync.DisabledTestProjectPostSyncHook
import org.jetbrains.bazel.impl.flow.sync.DisabledTestProjectPreSyncHook
import org.jetbrains.bazel.impl.flow.sync.DisabledTestProjectSyncHook
import org.jetbrains.bazel.impl.flow.sync.TestProjectPostSyncHook
import org.jetbrains.bazel.impl.flow.sync.TestProjectPreSyncHook
import org.jetbrains.bazel.impl.flow.sync.TestProjectSyncHook
import org.jetbrains.bazel.sync.ProjectPostSyncHook
import org.jetbrains.bazel.sync.ProjectPreSyncHook
import org.jetbrains.bazel.sync.ProjectSyncHook
import org.jetbrains.bazel.sync.ProjectSyncScope
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.testproject.aspectSyncServer
import org.jetbrains.bazel.test.framework.testproject.expectingSyncErrorLogged
import org.jetbrains.bazel.test.framework.testproject.failingBuildServer
import org.jetbrains.bazel.test.framework.testproject.runProjectSync
import org.jetbrains.bazel.test.framework.testproject.targetIdeInfo
import org.jetbrains.bazel.test.framework.testproject.testBazelProjectFixture
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

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
}
