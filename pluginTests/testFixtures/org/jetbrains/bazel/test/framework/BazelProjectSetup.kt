package org.jetbrains.bazel.test.framework

import com.intellij.configurationStore.ProjectStoreImpl
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.fileLogger
import com.intellij.openapi.extensions.LoadingOrder
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.impl.jrt.JrtFileSystemImpl
import com.intellij.openapi.vfs.jrt.JrtFileSystem
import com.intellij.project.stateStore
import com.intellij.testFramework.replaceService
import org.jetbrains.annotations.TestOnly
import org.jetbrains.bazel.bazelrunner.BazelCommandExecutionDescriptor
import org.jetbrains.bazel.bazelrunner.BazelProcessLauncher
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

private val LOG = fileLogger()

/** Swaps in a [TestConsoleService] so a sync writes to the test log. Restored when [disposable] disposes. */
internal fun installTestConsoleService(project: Project, disposable: Disposable) {
  LOG.info("Installing the test console service for ${project.name}")
  project.replaceService(
    ConsoleService::class.java,
    TestConsoleService(project).also { Disposer.register(disposable, it) },
    disposable,
  )
}

/**
 * Makes each Bazel command ignore the rc files of the host until [disposable] is disposed.
 *
 * Bazel reads `~/.bazelrc` after the workspace `.bazelrc`, so a host setting can replace a test setting. Bazel accepts
 * `--nohome_rc` and `--nosystem_rc` only on the command line, not in an rc file.
 */
internal fun installHostRcIsolation(disposable: Disposable) {
  val launcherProvider = BazelProcessLauncherProvider.getInstance()
  val isolatingProvider = object : BazelProcessLauncherProvider {
    override fun createBazelProcessLauncher(workspaceRoot: Path, parentEnvironment: Map<String, String>): BazelProcessLauncher {
      val launcher = launcherProvider.createBazelProcessLauncher(workspaceRoot, parentEnvironment)
      return object : BazelProcessLauncher {
        override fun launchProcess(executionDescriptor: BazelCommandExecutionDescriptor): Process =
          launcher.launchProcess(executionDescriptor.copy(command = executionDescriptor.command.withoutHostRcFiles()))
      }
    }
  }
  try {
    BazelProcessLauncherProvider.ep.point.registerExtension(isolatingProvider, LoadingOrder.FIRST, disposable)
  }
  catch (e: IllegalStateException) {
    // A test that masks the extension point launches its own processes, so it does not run the host Bazel.
    LOG.info("Skipping the host rc isolation: ${e.message}")
  }
}

private val HOST_RC_STARTUP_OPTIONS = listOf("--nohome_rc", "--nosystem_rc")

// The startup options follow the Bazel binary. A nested install must not add them twice.
private fun List<String>.withoutHostRcFiles(): List<String> =
  take(1) + HOST_RC_STARTUP_OPTIONS.filter { it !in this } + drop(1)

/** Writes the `.bazelversion` file in [projectRoot]. */
internal fun writeBazelVersion(projectRoot: Path, version: String) {
  LOG.info("Writing .bazelversion $version")
  projectRoot.resolve(".bazelversion").writeText(version)
}

/** Copies the project view file [projectView] from [projectRoot] into the store descriptor location. */
internal fun applyProjectView(project: Project, projectRoot: Path, projectView: String) {
  val source = projectRoot.resolve(projectView).takeIf { it.exists() }
  if (source == null) {
    LOG.info("Skipping the project view $projectView, because $projectRoot does not hold it")
    return
  }
  LOG.info("Applying the project view $projectView")
  source.copyTo(projectViewFile(project), overwrite = true)
}

fun writeProjectView(project: Project, content: String) {
  LOG.info("Writing the project view of ${project.name}")
  projectViewFile(project).writeText(content)
}

/** Returns the project view path of [project], with the parent directories in place. */
private fun projectViewFile(project: Project): Path {
  val descriptor = (project.stateStore as ProjectStoreImpl).storeDescriptor as BazelProjectStoreDescriptor
  return descriptor.projectViewFile.createParentDirectories()
}

/** Runs a full Bazel sync of [project]. */
internal suspend fun runBazelSync(project: Project, scope: ProjectSyncScope) {
  LOG.info("Syncing ${project.name} (scope=$scope)")
  project.service<ProjectSyncService>().sync(scope)
  LOG.info("Finished the sync of ${project.name}")
}

/** Removes every JDK the sync added, so it does not leak into the next test. */
internal fun purgeProjectJdkTable() {
  LOG.info("Purging the project JDK table")
  WriteAction.runAndWait<Throwable> {
    ProjectJdkTable.getInstance().apply {
      val jdks = allJdks
      releaseJrtFileSystems(jdks)
      jdks.forEach(this::removeJdk)
    }
  }
}

/**
 * Closes the JRT file systems of [jdks]. An open JRT file system holds `lib/modules`, and Windows then
 * cannot delete a JDK in the output base.
 */
private fun releaseJrtFileSystems(jdks: Array<Sdk>) {
  val jrtFileSystem = JrtFileSystem.getInstance() as JrtFileSystemImpl
  for (homePath in jdks.mapNotNull { it.homePath }) {
    try {
      jrtFileSystem.release(FileUtil.toSystemIndependentName(homePath))
      LOG.info("Released the JRT file system of $homePath")
    }
    catch (ignored: IllegalArgumentException) {
      // the test did not read this JDK through JRT
    }
  }
}

/** Stops the Bazel server, so it releases the file locks in [projectRoot] before the temp dir is removed. */
internal suspend fun stopBazelServer(project: Project, projectRoot: Path): BazelProcessResult {
  LOG.info("Stopping the Bazel server in $projectRoot")
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
