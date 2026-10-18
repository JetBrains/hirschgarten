package org.jetbrains.bazel.test.framework.testproject

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.runWriteActionAndWait
import com.intellij.openapi.components.service
import com.intellij.openapi.components.serviceAsync
import com.intellij.openapi.components.serviceIfCreated
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.io.NioFiles
import com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess
import com.intellij.openapi.vfs.refreshAndFindVirtualDirectory
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.workspace.storage.entities
import com.intellij.testFramework.ExtensionTestUtil
import com.intellij.testFramework.common.timeoutRunBlocking
import com.intellij.testFramework.replaceService
import org.jetbrains.bazel.bazelrunner.BazelProcessLauncher
import org.jetbrains.bazel.bazelrunner.BazelProcessLauncherProvider
import org.jetbrains.bazel.commons.BazelInfo
import org.jetbrains.bazel.commons.constants.Constants.DEFAULT_PROJECT_VIEW_FILE_NAME
import org.jetbrains.bazel.commons.constants.Constants.LEGACY_DEFAULT_PROJECT_VIEW_FILE_NAME
import org.jetbrains.bazel.commons.constants.Constants.MODULE_BAZEL_FILE_NAME
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.flow.open.ProjectViewFileUtils.projectViewTemplate
import org.jetbrains.bazel.ignore.BazelIgnoreService
import org.jetbrains.bazel.ignore.DefaultBazelIgnoreService
import org.jetbrains.bazel.languages.projectview.ProjectViewService
import org.jetbrains.bazel.project.BazelProjectFixtures.initializeBazelProject
import org.jetbrains.bazel.project.BazelProjectFixtures.initializeBazelProjectViaProjectView
import org.jetbrains.bazel.project.BazelProjectFixtures.restoreProjectStoreOnDispose
import org.jetbrains.bazel.project.DefaultProjectViewService
import org.jetbrains.bazel.sync.SyncCache
import org.jetbrains.bazel.sync.workspace.persistence.WorkspaceSnapshotService
import org.jetbrains.bazel.sync.workspace.persistence.WorkspaceSnapshotServiceImpl
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSnapshot
import org.jetbrains.bazel.target.TargetStorage
import org.jetbrains.bazel.test.framework.installHostRcIsolation
import org.jetbrains.bazel.test.framework.installStableJdkPaths
import org.jetbrains.bazel.test.framework.installTestConsoleService
import org.jetbrains.bazel.workspacemodel.entities.BazelProjectDirectoriesEntity
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.io.path.createTempDirectory
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile
import kotlin.io.path.writeText

class TestBazelProject internal constructor(
  val project: Project,
  internal val defaultBazelInfo: BazelInfo,
  internal val lifetime: Disposable,
  /**
   * The disposable of the test that installed this project.
   * [uninstall] does not dispose it. A service or an extension that you register with it stays until the end of the test.
   */
  val disposable: Disposable,
)

internal fun Project.installTestBazelProject(root: Path, parentDisposable: Disposable): TestBazelProject {
  val outputBase = createTempDirectory("bazel-output-base").toRealPath()
  Disposer.register(parentDisposable) { NioFiles.deleteRecursively(outputBase) }
  return installTestBazelProject(root, outputBase, parentDisposable)
}

internal fun Project.installTestBazelProject(
  root: Path,
  outputBase: Path,
  parentDisposable: Disposable,
  realBazel: Boolean = false,
  persistentSnapshots: Boolean = false,
): TestBazelProject {
  require(root.isAbsolute) { "bazel root $root is not absolute" }
  val lifetime = Disposer.newDisposable(parentDisposable, "TestBazelProject")
  var installed = false
  try {
    return installInternal(root, outputBase, parentDisposable, lifetime, realBazel, persistentSnapshots).also { installed = true }
  }
  finally {
    if (!installed) Disposer.dispose(lifetime)
  }
}

private fun Project.installInternal(
  root: Path,
  outputBase: Path,
  parentDisposable: Disposable,
  lifetime: Disposable,
  realBazel: Boolean,
  persistentSnapshots: Boolean,
): TestBazelProject {
  val project = this
  val defaultBazelInfo = testProjectBazelInfo(workspaceRoot = root, outputBase = outputBase)
  defaultBazelInfo.bazelBin.createDirectories()

  if (realBazel) {
    installTestConsoleService(project, lifetime)
    installHostRcIsolation(lifetime)
    installStableJdkPaths(lifetime)
  }
  else {
    val moduleFile = root.resolve(MODULE_BAZEL_FILE_NAME)
    if (!moduleFile.exists()) {
      moduleFile.createFile()
    }
    // store picks its delegate on the first access, so a real sync must not touch it before `initializeBazelProject`
    installSnapshotStore(lifetime, persistentSnapshots)
    ExtensionTestUtil.maskExtensions(BazelProcessLauncherProvider.ep, listOf(NoBazelProcessLauncherProvider), lifetime)
  }

  replaceService(ProjectViewService::class.java, DefaultProjectViewService(project), lifetime)
  replaceService(BazelIgnoreService::class.java, DefaultBazelIgnoreService(project), lifetime)

  restoreProjectStoreOnDispose(project, lifetime)
  initializeBazelProject(project, root, projectViewFile = if (realBazel) null else defaultProjectViewFile(root, lifetime))
  check(rootDir.toNioPath() == root) { "project root ${rootDir.path} is not bazel root $root" }

  Disposer.register(lifetime) { if (!project.isDisposed) removeProjectDirectoriesEntities() }
  val testBazel = TestBazelProject(project, defaultBazelInfo, lifetime, parentDisposable)
  if (!realBazel) {
    testBazel.sync()
  }
  return testBazel
}

private fun defaultProjectViewFile(root: Path, lifetime: Disposable): Path? {
  if (root.resolve(DEFAULT_PROJECT_VIEW_FILE_NAME).isRegularFile()
      || root.resolve(LEGACY_DEFAULT_PROJECT_VIEW_FILE_NAME).isRegularFile()) {
    return null
  }
  val directory = createTempDirectory("bazel-project-view").toRealPath()
  Disposer.register(lifetime) { NioFiles.deleteRecursively(directory) }
  // light test allows VFS access only to known roots
  VfsRootAccess.allowRootAccess(lifetime, directory.toString())
  val rootDirectory = root.refreshAndFindVirtualDirectory() ?: error("Directory `$root` does not exist")
  return directory.resolve(DEFAULT_PROJECT_VIEW_FILE_NAME).apply { writeText(projectViewTemplate(rootDirectory)) }
}

private fun Project.installSnapshotStore(lifetime: Disposable, persistentSnapshots: Boolean) {
  val project = this
  val store = service<WorkspaceSnapshotService>() as? WorkspaceSnapshotServiceImpl
              ?: error("Expected `${WorkspaceSnapshotServiceImpl::class}` as `${WorkspaceSnapshotService::class}` implementation")
  if (persistentSnapshots) {
    store.forcePersistent = true
  }
  else {
    store.isEphemeral = true
  }
  val savedSnapshot = store.snapshot.value
  publishSnapshotAndAwait(WorkspaceSnapshot.EMPTY)
  Disposer.register(lifetime) { if (!project.isDisposed) publishSnapshotAndAwait(savedSnapshot) }
}

fun TestBazelProject.uninstall() {
  Disposer.dispose(lifetime)
}

fun TestBazelProject.useProjectView(projectViewFile: Path) {
  require(projectViewFile.parent == workspaceRoot) { "project view file $projectViewFile is not in the bazel root $workspaceRoot" }
  initializeBazelProjectViaProjectView(project, projectViewFile)
}

internal fun Project.publishSnapshotAndAwait(snapshot: WorkspaceSnapshot) {
  val project = this
  timeoutRunBlocking {
    project.serviceAsync<WorkspaceSnapshotService>().update { snapshot to Unit }
    SyncCache.getInstance(project).clear()
    project.serviceIfCreated<TargetStorage>()?.awaitLoaded()
  }
}

private fun Project.removeProjectDirectoriesEntities() {
  val workspaceModel = WorkspaceModel.getInstance(this)
  if (workspaceModel.currentSnapshot.entities<BazelProjectDirectoriesEntity>().none()) {
    return
  }
  runWriteActionAndWait {
    workspaceModel.updateProjectModel("Remove test Bazel project directories") { storage ->
      storage.entities<BazelProjectDirectoriesEntity>().toList().forEach(storage::removeEntity)
    }
  }
}

private object NoBazelProcessLauncherProvider : BazelProcessLauncherProvider {
  override fun createBazelProcessLauncher(workspaceRoot: Path, parentEnvironment: Map<String, String>): BazelProcessLauncher =
    NoBazelProcessLauncher
}
