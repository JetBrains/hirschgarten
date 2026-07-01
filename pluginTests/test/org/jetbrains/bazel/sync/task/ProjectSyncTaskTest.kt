package org.jetbrains.bazel.sync.task

import com.google.devtools.intellij.ideinfo.IntellijIdeInfo.TargetIdeInfo
import com.google.devtools.intellij.ideinfo.IntellijIdeInfo.TargetKey
import com.intellij.internal.statistic.FUCollectorTestCase
import com.intellij.testFramework.LoggedErrorProcessor
import com.intellij.testFramework.registerOrReplaceServiceInstance
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import org.jetbrains.bazel.commons.BazelRelease
import org.jetbrains.bazel.impl.flow.sync.DisabledTestProjectPostSyncHook
import org.jetbrains.bazel.impl.flow.sync.DisabledTestProjectPreSyncHook
import org.jetbrains.bazel.impl.flow.sync.DisabledTestProjectSyncHook
import org.jetbrains.bazel.impl.flow.sync.TestProjectPostSyncHook
import org.jetbrains.bazel.impl.flow.sync.TestProjectPreSyncHook
import org.jetbrains.bazel.impl.flow.sync.TestProjectSyncHook
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.project.BazelProjectFixtures.initializeBazelProject
import org.jetbrains.bazel.server.BazelServerService
import org.jetbrains.bazel.server.model.AspectSyncProject
import org.jetbrains.bazel.sync.ProjectPostSyncHook
import org.jetbrains.bazel.sync.ProjectPreSyncHook
import org.jetbrains.bazel.sync.ProjectSyncHook
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey
import org.jetbrains.bazel.workspace.model.test.framework.BuildServerMock
import org.jetbrains.bazel.workspace.model.test.framework.MockBuildServerService
import org.jetbrains.bazel.workspace.model.test.framework.MockProjectBaseTest
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetParams
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.io.path.Path
import kotlin.test.assertNotNull

@DisplayName("ProjectSyncTask tests")
class ProjectSyncTaskTest : MockProjectBaseTest() {

  @BeforeEach
  fun beforeAll() {
    initializeBazelProject(project, projectDir.get())
  }

  @Test
  fun `should call all enabled pre-sync, sync and post-sync hooks`() {
    // given
    project.registerOrReplaceServiceInstance(
      BazelServerService::class.java,
      MockBuildServerService(
        BuildServerMock(
          aspectSyncProject = AspectSyncProject(
            workspaceRoot = Path(""),
            bazelRelease = BazelRelease(9, 0),
            workspaceName = "",
            targets = emptyMap(),
            rootTargets = emptySet(),
            configurations = emptyMap(),
          ),
        ),
      ),
      disposable,
    )

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
      ProjectSyncTask(project).fullSync(false)
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
  fun `should call post-sync hooks after failed sync without project model update`() {
    // given
    project.registerOrReplaceServiceInstance(
      BazelServerService::class.java,
      MockBuildServerService(
        BuildServerMock(
          aspectSyncProject = AspectSyncProject(
            workspaceRoot = Path(""),
            bazelRelease = BazelRelease(9, 0),
            workspaceName = "",
            hasError = true,
            targets = emptyMap(),
            rootTargets = emptySet(),
            configurations = emptyMap(),
          ),
        ),
      ),
      disposable,
    )

    val postSyncHook = TestProjectPostSyncHook()
    ProjectPostSyncHook.ep.registerExtension(postSyncHook)

    // when
    runBlocking {
      ProjectSyncTask(project).fullSync(false)
    }

    // then
    postSyncHook.wasCalled shouldBe true
    postSyncHook.projectModelUpdated shouldBe false
  }

  @Test
  fun `should call post-sync hooks after sync fails with an exception`() {
    // given a server that throws while resolving the workspace, i.e. sync fails mid-flight rather than resolving no targets
    val failure = IllegalStateException("boom while resolving workspace")
    project.registerOrReplaceServiceInstance(
      BazelServerService::class.java,
      MockBuildServerService(FailingBuildServerMock(failure)),
      disposable,
    )

    val postSyncHook = TestProjectPostSyncHook()
    ProjectPostSyncHook.ep.registerExtension(postSyncHook)

    // when (the sync failure is logged via log.error on purpose; don't let it fail the test)
    val events = expectingSyncErrorLogged {
      FUCollectorTestCase.collectLogEvents(disposable) {
        runBlocking {
          ProjectSyncTask(project).fullSync(false)
        }
      }.filter { it.group.id == "bazel.sync" }.map { it.event }
    }

    // then post-sync hooks still run for cleanup, and the sync is reported as a failure (not "no targets")
    postSyncHook.wasCalled shouldBe true
    postSyncHook.projectModelUpdated shouldBe false

    val finished = assertNotNull(events.singleOrNull { it.id == "sync.finished" })
    finished.data["completion_result"] shouldBe "FAILURE"
  }

  @Test
  fun `should log FUS events for sync lifecycle timings and language counts`() {
    val javaTarget = targetInfo("//app:java", "java_library")
    val kotlinTarget = targetInfo("//app:kotlin", "kt_jvm_library")
    project.registerOrReplaceServiceInstance(
      BazelServerService::class.java,
      MockBuildServerService(
        BuildServerMock(
          aspectSyncProject = AspectSyncProject(
            workspaceRoot = Path(""),
            bazelRelease = BazelRelease(9, 0),
            workspaceName = "",
            targets = toIdToTargetInfoMap(javaTarget, kotlinTarget),
            rootTargets = setOf(javaTarget.workspaceTargetKey(), kotlinTarget.workspaceTargetKey()),
            configurations = emptyMap(),
          ),
        ),
      ),
      disposable,
    )

    val events = FUCollectorTestCase.collectLogEvents(disposable) {
      runBlocking {
        ProjectSyncTask(project).fullSync(buildProject = true)
      }
    }.filter { it.group.id == "bazel.sync" }.map { it.event }

    val started = assertNotNull(events.singleOrNull { it.id == "sync.started" })
    val finished = assertNotNull(events.singleOrNull { it.id == "sync.finished" })
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

private class FailingBuildServerMock(private val failure: Throwable) : BuildServerMock() {
  override suspend fun workspaceBuildTargets(params: WorkspaceBuildTargetParams): AspectSyncProject = throw failure
}

// the production code logs sync failures via log.error; intercept it so the test logger doesn't flag it as a failure
private fun <T> expectingSyncErrorLogged(block: () -> T): T {
  var result: T? = null
  LoggedErrorProcessor.executeWith<RuntimeException>(
    object : LoggedErrorProcessor() {
      override fun processError(category: String, message: String, details: Array<String>, t: Throwable?): Set<Action> =
        if (message.contains("Error syncing project")) Action.NONE else Action.ALL
    },
  ) {
    result = block()
  }
  @Suppress("UNCHECKED_CAST")
  return result as T
}

private fun targetInfo(id: String, kind: String): TargetIdeInfo =
  TargetIdeInfo
    .newBuilder()
    .setKey(TargetKey.newBuilder().setLabel(id).build())
    .setKind(kind)
    .build()

private fun toIdToTargetInfoMap(vararg targets: TargetIdeInfo): Map<WorkspaceTargetKey, TargetIdeInfo> =
  targets.associateBy { it.workspaceTargetKey() }

private fun TargetIdeInfo.workspaceTargetKey(): WorkspaceTargetKey = WorkspaceTargetKey(label = Label.parse(key.label))
