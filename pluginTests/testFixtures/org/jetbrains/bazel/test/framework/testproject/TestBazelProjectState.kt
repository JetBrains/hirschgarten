package org.jetbrains.bazel.test.framework.testproject

import com.intellij.openapi.application.runWriteActionAndWait
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.refreshAndFindVirtualDirectory
import com.intellij.platform.backend.workspace.storeAndGet
import com.intellij.platform.backend.workspace.virtualFile
import com.intellij.platform.workspace.jps.entities.ContentRootEntity
import com.intellij.platform.workspace.storage.entities
import com.intellij.testFramework.IndexingTestUtil
import com.intellij.testFramework.common.timeoutRunBlocking
import com.intellij.testFramework.refreshVfs
import com.intellij.testFramework.registerOrReplaceServiceInstance
import org.jetbrains.bazel.commons.BazelInfo
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.languages.projectview.projectView
import org.jetbrains.bazel.server.BazelServerConnection
import org.jetbrains.bazel.server.BazelServerFacade
import org.jetbrains.bazel.server.BazelServerService
import org.jetbrains.bazel.symlinks.createBazelConvenienceSymlink
import org.jetbrains.bazel.sync.SyncCache
import org.jetbrains.bazel.sync.workspace.languages.LanguagePlugin
import org.jetbrains.bazel.sync.workspace.snapshot.CommonWorkspaceSyncConfig
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSnapshot
import org.jetbrains.bazel.sync.workspace.snapshot.workspaceSnapshotOrNull
import org.jetbrains.bazel.target.CachedSnapshotView
import org.jetbrains.bazel.workspace.indexing.IndexableContentCollector
import org.jetbrains.bazel.workspace.model.test.framework.BuildServerMock
import org.jetbrains.bazel.workspacemodel.entities.BazelProjectDirectoriesEntity
import org.jetbrains.bazel.workspacemodel.entities.BazelProjectEntitySource
import org.jetbrains.bsp.protocol.TaskId
import java.nio.file.Path
import kotlin.io.path.invariantSeparatorsPathString

val TestBazelProject.workspaceRoot: Path
  get() = defaultBazelInfo.workspaceRoot

val TestBazelProject.outputBase: Path
  get() = defaultBazelInfo.outputBase

val TestBazelProject.bazelInfo: BazelInfo
  get() = project.workspaceSnapshotOrNull
            ?.takeIf { snapshot -> snapshot.syncConfigs.any { it is CommonWorkspaceSyncConfig } }
            ?.bazelInfo
          ?: defaultBazelInfo

/**
 * Writes [text] to the file at [path] through the VFS and returns the file.
 * [path] must be under the workspace root or under the output base.
 * Call it from any thread, but not from a read action.
 */
fun TestBazelProject.writeFile(path: Path, text: String = ""): VirtualFile {
  val file = path.normalize()
  return runWriteActionAndWait {
    val parent = VfsUtil.createDirectoryIfMissing(file.parent.invariantSeparatorsPathString)
                 ?: error("Cannot make the directory ${file.parent}")
    val virtualFile = parent.findChild(file.fileName.toString()) ?: parent.createChildData(this, file.fileName.toString())
    VfsUtil.saveText(virtualFile, text)
    virtualFile
  }
}

fun TestBazelProject.writeFile(relativePath: String, text: String = ""): VirtualFile {
  val path = workspaceRoot.resolve(relativePath).normalize()
  require(path.startsWith(workspaceRoot) && path != workspaceRoot) { "File $relativePath is not under the bazel root $workspaceRoot" }
  return writeFile(path, text)
}

fun TestBazelProject.createDirectory(relativePath: String): VirtualFile {
  val path = workspaceRoot.resolve(relativePath).normalize()
  require(path.startsWith(workspaceRoot) && path != workspaceRoot) { "Directory $relativePath is not under the bazel root $workspaceRoot" }
  return runWriteActionAndWait {
    VfsUtil.createDirectoryIfMissing(path.invariantSeparatorsPathString) ?: error("Cannot make the directory $path")
  }
}

/**
 * Creates the Bazel convenience symlink [name] in the workspace root, points it to [target], and refreshes the VFS.
 * Do not use it in a test that sends its own VFS events, because the refresh starts the real listeners.
 */
fun TestBazelProject.createConvenienceSymlink(name: String, target: Path = workspaceRoot.resolve("execroot/$name")): Path =
  workspaceRoot.createBazelConvenienceSymlink(name, target).also { workspaceRoot.refreshVfs() }

fun TestBazelProject.sync(declare: TestSnapshotBuilder.() -> Unit = {}) {
  val builder = TestSnapshotBuilder().apply(declare)
  val snapshotBazelInfo = builder.bazelInfoFrom(bazelInfo)
  val languageSyncConfigs = timeoutRunBlocking {
    val projectView = project.projectView()
    LanguagePlugin.EP_NAME.extensionList.flatMap { it.createSyncConfigs(project, projectView) }
  }
  val snapshot = builder.build(snapshotBazelInfo, builder.repoMappingFrom(snapshotBazelInfo), languageSyncConfigs)
  // catch potential exceptions caused by `CachedSnapshotView` construction here
  CachedSnapshotView(snapshot)
  project.publishSnapshotAndAwait(snapshot)
}

/** Publishes the empty snapshot, as before the first sync. The snapshot has no targets, and [bazelInfo] returns the fixture default. */
fun TestBazelProject.clearSnapshot() {
  project.publishSnapshotAndAwait(WorkspaceSnapshot.EMPTY)
}

fun TestBazelProject.buildServer(server: BazelServerFacade = layoutBuildServer(bazelInfo)) {
  project.registerOrReplaceServiceInstance(BazelServerService::class.java, MockBuildServerService(server), lifetime)
}

private fun layoutBuildServer(layout: BazelInfo): BazelServerFacade =
  object : BuildServerMock() {
    override val bazelInfo: BazelInfo = layout
  }

private class MockBuildServerService(server: BazelServerFacade) : BazelServerService {
  override val connection: BazelServerConnection =
    object : BazelServerConnection {
      override suspend fun <T> runWithServer(taskId: TaskId?, task: suspend (server: BazelServerFacade) -> T): T = task(server)
    }
}

/**
 * Manages `directories:` project view section
 */
class TestProjectDirectoriesBuilder internal constructor() {
  internal val included = mutableListOf<String>()
  internal val excluded = mutableListOf<String>()
  internal val indexPatterns = mutableListOf<String>()


  fun include(vararg relativePaths: String) {
    included += relativePaths
  }

  fun exclude(vararg relativePaths: String) {
    excluded += relativePaths
  }

  fun index(vararg patterns: String) {
    indexPatterns += patterns
  }
}

fun TestBazelProject.projectDirectories(declare: TestProjectDirectoriesBuilder.() -> Unit = {}) {
  val builder = TestProjectDirectoriesBuilder().apply(declare)
  val included = builder.included.map { directory(it) }
  val excluded = builder.excluded.map { directory(it) }
  VfsUtil.markDirtyAndRefresh(false, true, true, *included.toTypedArray())
  val urls = virtualFileUrlManager
  val content = IndexableContentCollector(
    project = project,
    indexPatterns = builder.indexPatterns,
    includedRoots = included.toSet(),
    excludedRoots = excluded.toSet(),
    contentRoots = workspaceModel.currentSnapshot.entities<ContentRootEntity>().mapNotNullTo(mutableSetOf()) { it.url.virtualFile },
  ).collect(urls)
  updateWorkspaceModel { storage ->
    storage.entities<BazelProjectDirectoriesEntity>().toList().forEach(storage::removeEntity)
    storage.addEntity(
      BazelProjectDirectoriesEntity(
        projectRoot = urls.storeAndGet(project.rootDir),
        includedRoots = included.map(urls::storeAndGet),
        excludedRoots = excluded.map(urls::storeAndGet),
        indexPatterns = builder.indexPatterns.toList(),
        indexableRecursiveRoots = content.recursiveRoots.toList(),
        indexableNonRecursiveRoots = content.nonRecursiveRoots.toList(),
        entitySource = BazelProjectEntitySource,
      ),
    )
  }
  SyncCache.getInstance(project).clear()
  IndexingTestUtil.waitUntilIndexesAreReady(project)
}

private fun TestBazelProject.directory(relativePath: String): VirtualFile {
  val path = workspaceRoot.resolve(relativePath).normalize()
  return path.refreshAndFindVirtualDirectory() ?: error("Directory `$path` does not exist")
}
