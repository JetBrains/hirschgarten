package org.jetbrains.bazel.redcodes

import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.readAction
import com.intellij.testFramework.common.timeoutRunBlocking
import com.intellij.workspaceModel.core.fileIndex.WorkspaceFileIndex
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.bazelrunner.BazelProcessLauncherProvider
import org.jetbrains.bazel.bazelrunner.BazelRunner
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.languages.projectview.ProjectViewService
import org.jetbrains.bazel.server.bsp.managers.BazelWorkspaceExternalRulesetsQueryImpl
import org.jetbrains.bazel.sync.BazelEnvironmentService
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelSyncCodeInsightFixture
import org.jetbrains.bazel.test.framework.checkHighlighting
import org.jetbrains.bazel.test.framework.enableGoHighlighting
import org.jetbrains.bsp.protocol.BazelTaskEventsHandler
import org.jetbrains.bsp.protocol.CachedTestLog
import org.jetbrains.bsp.protocol.CoverageReport
import org.jetbrains.bsp.protocol.LogMessageParams
import org.jetbrains.bsp.protocol.PublishDiagnosticsParams
import org.jetbrains.bsp.protocol.TaskFinishParams
import org.jetbrains.bsp.protocol.TaskGroupId
import org.jetbrains.bsp.protocol.TaskStartParams
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.DisabledOnOs
import org.junit.jupiter.api.condition.OS
import kotlin.time.Duration.Companion.minutes

@BazelTestApplication
internal class GoWorkspaceTest {

  private val fixture by bazelSyncCodeInsightFixture(
    "redcodes/go_workspace_git_repository",
    bazelVersion = "7.7.1",
    configure = { it.enableGoHighlighting() },
  )

  @Test
  @DisabledOnOs(OS.WINDOWS) // bazel failed to get go rules
  fun testCustomAspectGeneratedClassResolves(): Unit = timeoutRunBlocking(timeout = 5.minutes) {
    val project = fixture.project
    val workspaceIndex = WorkspaceFileIndex.getInstance(project)
    readAction { workspaceIndex.isIndexable(project.rootDir.findFileByRelativePath("main.go")!!) shouldBe true }

    withContext(Dispatchers.EDT) {
      fixture.checkHighlighting("main.go")
    }

    val projectRoot = project.rootDir.toNioPath()
    val projectView = ProjectViewService.getInstance(project).projectView
    val bazelProcessLauncher =
      BazelProcessLauncherProvider.getInstance()
        .createBazelProcessLauncher(projectRoot, BazelEnvironmentService.getInstance(project).getEnvironment())
    val bazelRunner = BazelRunner.create(project, null, projectRoot, bazelProcessLauncher, projectView)
    val taskEventsHandler = RecordingTaskEventsHandler()

    val rulesetNames =
      BazelWorkspaceExternalRulesetsQueryImpl(
        TaskGroupId.EMPTY.task(""),
        bazelRunner,
        isWorkspaceEnabled = true,
        taskEventsHandler,
        projectView,
      )
        .fetchExternalRulesetNames()

    // The macros `go_rules_dependencies()` and `maybe()` create the other http_archive rules, so they are not rulesets.
    rulesetNames shouldContainExactly listOf("io_bazel_rules_go")
    taskEventsHandler.logMessages.shouldBeEmpty()

  }

  /** Records the log messages that the query reports to the sync console. */
  private class RecordingTaskEventsHandler : BazelTaskEventsHandler {
    val logMessages = mutableListOf<LogMessageParams>()

    override fun onBuildLogMessage(params: LogMessageParams) {
      logMessages.add(params)
    }

    override fun onBuildPublishDiagnostics(params: PublishDiagnosticsParams) {}
    override fun onBuildTaskStart(params: TaskStartParams) {}
    override fun onBuildTaskFinish(params: TaskFinishParams) {}
    override fun onPublishCoverageReport(report: CoverageReport) {}
    override fun onCachedTestLog(testLog: CachedTestLog) {}
  }
}
