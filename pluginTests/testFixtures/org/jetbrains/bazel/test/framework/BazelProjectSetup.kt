package org.jetbrains.bazel.test.framework

import com.intellij.configurationStore.ProjectStoreImpl
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.util.Disposer
import com.intellij.project.stateStore
import com.intellij.testFramework.replaceService
import org.jetbrains.bazel.bazelrunner.BazelProcessLauncherProvider
import org.jetbrains.bazel.bazelrunner.BazelProcessResult
import org.jetbrains.bazel.bazelrunner.BazelRunner
import org.jetbrains.bazel.flow.open.BazelProjectStoreDescriptor
import org.jetbrains.bazel.languages.projectview.ProjectViewService
import org.jetbrains.bazel.progress.ConsoleService
import org.jetbrains.bazel.sync.BazelEnvironmentService
import org.jetbrains.bazel.sync.ProjectSyncScope
import org.jetbrains.bazel.sync.ProjectSyncService
import org.jetbrains.bsp.protocol.TaskGroupId
import java.nio.file.Path
import kotlin.io.path.copyTo
import kotlin.io.path.createParentDirectories
import kotlin.io.path.exists
import kotlin.io.path.writeText

/**
 * Setup and lifecycle steps for a Bazel test project.
 *
 * Each step works on a [Project] and a project root [Path], so it does not need a code-insight test
 * fixture. Both the fixture and the declarative [bazelProjectFixture] use these steps.
 */

/** Swaps in a [TestConsoleService] so a sync writes to the test log. Restored when [disposable] disposes. */
internal fun installTestConsoleService(project: Project, disposable: Disposable) {
  project.replaceService(
    ConsoleService::class.java,
    TestConsoleService(project).also { Disposer.register(disposable, it) },
    disposable,
  )
}

/** Writes the `.bazelversion` file in [projectRoot]. */
internal fun writeBazelVersion(projectRoot: Path, version: String) {
  projectRoot.resolve(".bazelversion").writeText(version)
}

/** Copies the project view file [projectView] from [projectRoot] into the store descriptor location. */
internal fun applyProjectView(project: Project, projectRoot: Path, projectView: String) {
  val source = projectRoot.resolve(projectView).takeIf { it.exists() } ?: return
  val descriptor = (project.stateStore as ProjectStoreImpl).storeDescriptor as BazelProjectStoreDescriptor
  source.copyTo(descriptor.projectViewFile.createParentDirectories(), overwrite = true)
}

/** Runs a full Bazel sync of [project]. */
internal suspend fun runBazelSync(project: Project, buildProject: Boolean) {
  project.service<ProjectSyncService>().sync(ProjectSyncScope.Full(build = buildProject, phased = false))
}

/** Removes every JDK the sync added, so it does not leak into the next test. */
internal fun purgeProjectJdkTable() {
  WriteAction.runAndWait<Throwable> {
    ProjectJdkTable.getInstance().apply {
      allJdks.forEach(this::removeJdk)
    }
  }
}

/** Stops the Bazel server, so it releases the file locks in [projectRoot] before the temp dir is removed. */
internal suspend fun stopBazelServer(project: Project, projectRoot: Path): BazelProcessResult {
  val bazelProcessLauncher =
    BazelProcessLauncherProvider.getInstance()
      .createBazelProcessLauncher(
        projectRoot,
        BazelEnvironmentService.getInstance(project).getEnvironment(),
      )
  val projectView = ProjectViewService.getInstance(project).projectView
  val bazelRunner = BazelRunner.create(
    project, null, projectRoot, bazelProcessLauncher,
    projectView,
  )
  return bazelRunner.run {
    val command =
      buildBazelCommand(projectView) {
        shutDown()
      }
    runBazelCommand(command, TaskGroupId.EMPTY.task(""))
      .waitAndGetResult()
  }
}
