@file:Suppress("UnstableApiUsage", "SameParameterValue")

package org.jetbrains.bazel.workspace

import com.intellij.openapi.Disposable
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.events.ChildInfo
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent
import com.intellij.openapi.vfs.newvfs.events.VFileCopyEvent
import com.intellij.openapi.vfs.newvfs.events.VFileCreateEvent
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.openapi.vfs.newvfs.events.VFileMoveEvent
import com.intellij.openapi.vfs.newvfs.events.VFilePropertyChangeEvent
import com.intellij.platform.backend.workspace.toVirtualFileUrl
import com.intellij.platform.project.projectId
import com.intellij.platform.workspace.jps.entities.ContentRootEntity
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.jps.entities.ModuleId
import com.intellij.platform.workspace.jps.entities.SourceRootEntity
import com.intellij.platform.workspace.jps.entities.SourceRootTypeId
import com.intellij.platform.workspace.jps.entities.modifyModuleEntity
import com.intellij.platform.workspace.storage.ImmutableEntityStorage
import com.intellij.platform.workspace.storage.url.VirtualFileUrl
import com.intellij.testFramework.junit5.TestDisposable
import com.intellij.testFramework.replaceService
import com.intellij.testFramework.workspaceModel.updateProjectModel
import com.intellij.workspaceModel.ide.toPath
import io.kotest.common.runBlocking
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.magicmetamodel.formatAsModuleName
import org.jetbrains.bazel.sdkcompat.workspacemodel.entities.BazelModuleEntitySource
import org.jetbrains.bazel.server.connection.BazelServerConnection
import org.jetbrains.bazel.server.connection.BazelServerService
import org.jetbrains.bazel.target.targetUtils
import org.jetbrains.bazel.workspace.model.test.framework.BuildServerMock
import org.jetbrains.bazel.workspace.model.test.framework.WorkspaceModelBaseTest
import org.jetbrains.bsp.protocol.InverseSourcesParams
import org.jetbrains.bsp.protocol.InverseSourcesResult
import org.jetbrains.bsp.protocol.JoinedBuildServer
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Path
import kotlin.io.path.relativeTo

class AssignFileToModuleListenerTest : WorkspaceModelBaseTest() {
  @TestDisposable
  lateinit var disposable: Disposable

  private val target1 = Label.parse("//src:target1")
  private val target2 = Label.parse("//src:target2")
  private val target3 = Label.parse("//src/package:target3")
  private val target4 = Label.parse("//src:target4")
  private val requestor = this

  @BeforeEach
  override fun beforeEach() {
    super.beforeEach()
    project.isBazelProject = true
    project.replaceService(BazelServerService::class.java, InverseSourcesServer(projectBasePath).serverService, disposable)

    target1.createModule()
    target2.createModule()
    target3.createModule()
    target4.createModule()
  }

  @Test
  fun `source file creation`() {
    val file = project.rootDir.createDirectory("src").createFile("aaa", "java")

    file.assertFileBelongsToTargets(
      target1 to false,
      target2 to false,
    )

    createEvent(file).process().assertNotNullAndAwait()

    file.assertFileBelongsToTargets(
      target1 to true,
      target2 to true,
    )
  }

  @Test
  fun `source file rename`() {
    val file = project.rootDir.createDirectory("src").createFile("aaa", "java")
    createEvent(file).process().assertNotNullAndAwait()

    file.assertFileBelongsToTargets(
      target1 to true,
      target2 to true,
      target4 to false,
    )

    runTestWriteAction { file.rename(requestor, "bbb.java") }
    renameEvent(file, "aaa.java", "bbb.java").process().assertNotNullAndAwait()

    file.assertFileBelongsToTargets(
      target1 to true,
      target2 to false,
      target4 to true,
    )
  }

  @Test
  fun `source file move`() {
    val src = project.rootDir.createDirectory("src")
    val file = src.createFile("aaa", "java")
    val pack = src.createDirectory("package")
    createEvent(file).process().assertNotNullAndAwait()

    file.assertFileBelongsToTargets(
      target1 to true,
      target2 to true,
      target3 to false,
    )

    val moveEvent = moveEvent(file, pack)
    runTestWriteAction { file.move(requestor, pack) }
    moveEvent.process().assertNotNullAndAwait()

    file.assertFileBelongsToTargets(
      target1 to false,
      target2 to true,
      target3 to true,
    )
  }

  @Test
  fun `source file copy`() {
    val src = project.rootDir.createDirectory("src")
    val file = src.createFile("aaa", "java")
    createEvent(file).process().assertNotNullAndAwait()

    file.assertFileBelongsToTargets(
      target1 to true,
      target2 to true,
      target4 to false,
    )

    val newFile = runTestWriteAction { file.copy(requestor, src, "bbb.java") }
    copyEvent(newFile, src, "bbb.java").process().assertNotNullAndAwait()

    file.assertFileBelongsToTargets(
      target1 to true,
      target2 to true,
      target4 to false,
    )
    newFile.assertFileBelongsToTargets(
      target1 to true,
      target2 to false,
      target4 to true,
    )
  }

  @Test
  fun `source file delete`() {
    val src = project.rootDir.createDirectory("src")
    val file = src.createFile("aaa", "java")
    createEvent(file).process().assertNotNullAndAwait()

    file.assertFileBelongsToTargets(
      target1 to true,
      target2 to true,
    )

    runTestWriteAction { file.delete(requestor) }
    deleteEvent(file).process().assertNotNullAndAwait()

    file.assertFileBelongsToTargets(
      target1 to false,
      target2 to false,
    )
  }

  @Test
  fun `should ignore non-source file`() {
    val file = project.rootDir.createDirectory("src").createFile("aaa", "txt")
    createEvent(file).process().shouldBeNull()
  }

  @Test
  fun `should ignore directory creation`() {
    val directory = project.rootDir.createDirectory("src")
    createEvent(directory).process().shouldBeNull()
  }

  @Test
  fun `should ignore non-applicable events`() {
    val file = project.rootDir.createDirectory("src").createFile("aaa", "txt")

    encodingChangeEvent(file).process().shouldBeNull()
    contentChangeEvent(file).process().shouldBeNull()
  }

  @Test
  fun `should not add file to model if its parent is already there`() {
    val src = project.rootDir.createDirectory("src")
    val srcUrl = src.toVirtualFileUrl(virtualFileUrlManager)
    val module = workspaceModel.currentSnapshot.resolveModule(target1)

    val sourceRoot =
      SourceRootEntity(
        url = srcUrl,
        entitySource = module.entitySource,
        rootTypeId = SourceRootTypeId("java-source"),
      )

    val contentRootEntity =
      ContentRootEntity(
        url = srcUrl,
        excludedPatterns = emptyList(),
        entitySource = module.entitySource,
      ) {
        sourceRoots += listOf(sourceRoot)
      }

    runTestWriteAction {
      workspaceModel.updateProjectModel { it.modifyModuleEntity(module) { this.contentRoots = listOf(contentRootEntity) } }
    }

    val file = src.createFile("aaa", "java")
    val fileUrl = file.toVirtualFileUrl(virtualFileUrlManager)
    createEvent(file).process().assertNotNullAndAwait()

    // should not be added to target1's model
    doesModuleContainFile(target1, fileUrl).shouldBeFalse()
    // but the rest of the actions should happen normally
    file.assertFileBelongsToTargets(target2 to true)
    fileUrl.belongsToTarget(target1).shouldBeTrue()
  }

  @Test
  fun `should not start a new processing if another is ongoing`() {
    val channel = Channel<Unit>()
    project.replaceService(BazelServerService::class.java, BlockedServerSimulator(channel).serverService, disposable)

    val src = project.rootDir.createDirectory("src")

    val fileA = src.createFile("aaa", "java")
    val eventA = createEvent(fileA).process()
    Thread.sleep(1_000)

    val fileB = src.createFile("bbb", "java")
    val eventB = createEvent(fileB).process()

    channel.trySend(Unit) // unblock the server

    eventA.shouldNotBeNull()
    eventB.shouldBeNull()
  }

  private fun VirtualFile.createFile(name: String, extension: String): VirtualFile {
    if (!this.isDirectory) error("Can't create a file in a non-directory file")
    return runTestWriteAction {
      this.createChildData(requestor, "$name.$extension")
    }
  }

  private fun VirtualFile.createDirectory(name: String): VirtualFile {
    if (!this.isDirectory) error("Can't create a directory in a non-directory file")
    return runTestWriteAction {
      this.createChildDirectory(requestor, name)
    }
  }

  private fun createEvent(file: VirtualFile) =
    VFileCreateEvent(
      requestor,
      file.parent,
      file.name,
      false,
      null,
      null,
      emptyArray<ChildInfo>(),
    )

  private fun deleteEvent(file: VirtualFile) = VFileDeleteEvent(this, file)

  private fun moveEvent(file: VirtualFile, newParent: VirtualFile) = VFileMoveEvent(this, file, newParent)

  private fun copyEvent(
    file: VirtualFile,
    newParent: VirtualFile,
    newChildName: String,
  ) = VFileCopyEvent(this, file, newParent, newChildName)

  private fun renameEvent(
    file: VirtualFile,
    oldName: String,
    newName: String,
  ) = VFilePropertyChangeEvent(this, file, VirtualFile.PROP_NAME, oldName, newName)

  private fun encodingChangeEvent(file: VirtualFile) =
    VFilePropertyChangeEvent(requestor, file, VirtualFile.PROP_ENCODING, Charsets.US_ASCII, Charsets.UTF_8)

  private fun contentChangeEvent(file: VirtualFile) = VFileContentChangeEvent(requestor, file, 0, 0)

  private fun VFileEvent.process(): Job? = AssignFileToModuleListener().afterSingleFileEvent(this)[project.projectId()]

  private fun VirtualFile.assertFileBelongsToTargets(vararg expectedBelongingStatus: Pair<Label, Boolean>) {
    this.toVirtualFileUrl(virtualFileUrlManager).assertFileBelongsToTargets(*expectedBelongingStatus)
  }

  private fun VirtualFileUrl.assertFileBelongsToTargets(vararg expectedBelongingStatus: Pair<Label, Boolean>) {
    expectedBelongingStatus.forEach { (target, shouldBeAdded) ->
      doesModuleContainFile(target, this).shouldBe(shouldBeAdded)
      this.belongsToTarget(target).shouldBe(shouldBeAdded)
    }
  }

  private fun VirtualFileUrl.belongsToTarget(target: Label): Boolean = project.targetUtils.getTargetsForPath(this.toPath()).contains(target)

  private fun Label.createModule(contentRootFiles: List<VirtualFile> = emptyList()) {
    val moduleName = this.formatAsModuleName(project)
    val entitySource = BazelModuleEntitySource(moduleName)
    val contentRoots =
      contentRootFiles.map {
        ContentRootEntity(
          url = it.toVirtualFileUrl(virtualFileUrlManager),
          excludedPatterns = emptyList(),
          entitySource = entitySource,
        )
      }
    val module =
      ModuleEntity(
        name = moduleName,
        dependencies = emptyList(),
        entitySource = entitySource,
      ) {
        this.contentRoots = contentRoots
      }
    runTestWriteAction { workspaceModel.updateProjectModel { it.addEntity(module) } }
  }

  private fun doesModuleContainFile(moduleTarget: Label, fileUrl: VirtualFileUrl): Boolean =
    workspaceModel.currentSnapshot
      .resolveModule(moduleTarget)
      .contentRoots
      .any { it.url == fileUrl }

  private fun ImmutableEntityStorage.resolveModule(target: Label): ModuleEntity {
    val moduleId = ModuleId(target.formatAsModuleName(project))
    return resolve(moduleId) ?: error("Module for $target does not exist")
  }
}

private class InverseSourcesServer(private val projectBasePath: Path) : BuildServerMock() {
  private val inverseSourcesData =
    mapOf(
      "src/aaa.java" to listOf("//src:target1", "//src:target2"),
      "src/package/aaa.java" to listOf("//src/package:target3", "//src:target2"),
      "src/bbb.java" to listOf("//src:target1", "//src:target4"),
    ).mapValues { it.value.map { target -> Label.parse(target) } }

  private val connection =
    object : BazelServerConnection {
      override suspend fun <T> runWithServer(task: suspend (JoinedBuildServer) -> T): T = task(this@InverseSourcesServer)
    }

  val serverService =
    object : BazelServerService {
      override val connection: BazelServerConnection = this@InverseSourcesServer.connection
    }

  override suspend fun buildTargetInverseSources(inverseSourcesParams: InverseSourcesParams): InverseSourcesResult {
    val relativePath =
      inverseSourcesParams.textDocument.path
        .relativeTo(projectBasePath)
        .toString()
    return InverseSourcesResult(inverseSourcesData.getOrDefault(relativePath, emptyList()))
  }
}

/** Not supposed to provide any valuable information - only simulates Bazel being blocked */
private class BlockedServerSimulator(private val rendezvous: Channel<*>) : BuildServerMock() {
  private val connection =
    object : BazelServerConnection {
      override suspend fun <T> runWithServer(task: suspend (JoinedBuildServer) -> T): T = task(this@BlockedServerSimulator)
    }

  val serverService =
    object : BazelServerService {
      override val connection: BazelServerConnection = this@BlockedServerSimulator.connection
    }

  override suspend fun buildTargetInverseSources(inverseSourcesParams: InverseSourcesParams): InverseSourcesResult {
    rendezvous.receive()
    return InverseSourcesResult(emptyList())
  }
}

private fun Job?.assertNotNullAndAwait() {
  this.shouldNotBeNull()
  runBlocking { join() }
}
