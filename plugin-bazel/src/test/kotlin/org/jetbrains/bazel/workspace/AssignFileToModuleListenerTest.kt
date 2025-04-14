@file:Suppress("UnstableApiUsage")

package org.jetbrains.bazel.workspace

import com.intellij.ide.impl.setTrusted
import com.intellij.openapi.Disposable
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.events.ChildInfo
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent
import com.intellij.openapi.vfs.newvfs.events.VFileCreateEvent
import com.intellij.openapi.vfs.newvfs.events.VFilePropertyChangeEvent
import com.intellij.platform.backend.workspace.toVirtualFileUrl
import com.intellij.platform.workspace.jps.entities.ContentRootEntity
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.jps.entities.ModuleId
import com.intellij.platform.workspace.jps.entities.SourceRootEntity
import com.intellij.platform.workspace.jps.entities.modifyModuleEntity
import com.intellij.platform.workspace.storage.ImmutableEntityStorage
import com.intellij.platform.workspace.storage.url.VirtualFileUrl
import com.intellij.testFramework.junit5.TestDisposable
import com.intellij.testFramework.replaceService
import com.intellij.testFramework.workspaceModel.updateProjectModel
import com.intellij.workspaceModel.ide.legacyBridge.impl.java.JAVA_SOURCE_ROOT_ENTITY_TYPE_ID
import com.intellij.workspaceModel.ide.toPath
import io.kotest.common.runBlocking
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.channels.Channel
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.magicmetamodel.formatAsModuleName
import org.jetbrains.bazel.server.connection.BazelServerConnection
import org.jetbrains.bazel.server.connection.BazelServerService
import org.jetbrains.bazel.target.targetUtils
import org.jetbrains.bazel.workspace.model.test.framework.BuildServerMock
import org.jetbrains.bazel.workspace.model.test.framework.WorkspaceModelBaseTest
import org.jetbrains.bazel.workspacemodel.entities.BspModuleEntitySource
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

  private val waiter = Channel<Unit>()

  private val target1 = Label.parse("//src:target1")
  private val target2 = Label.parse("//src:target2")
  private val target3 = Label.parse("//src/package:target3")
  private val target4 = Label.parse("//src:target4")
  private val requestor = this

  @BeforeEach
  override fun beforeEach() {
    super.beforeEach()
    project.isBazelProject = true
    project.setTrusted(true)
    project.replaceService(BazelServerService::class.java, InverseSourcesServer(projectBasePath).serverService, disposable)

    target1.createModule()
    target2.createModule()
    target3.createModule()
    target4.createModule()

    registerSyncWaiter()
  }

  @Test
  fun `source file creation`() {
    val file = project.rootDir.createDirectory("src").createFile("aaa", "java")
    waiter.await()

    file.assertModelStatus(
      target1 to true,
      target2 to true,
    )
  }

  @Test
  fun `source file rename`() {
    val file = project.rootDir.createDirectory("src").createFile("aaa", "java")
    waiter.await()

    file.assertModelStatus(
      target1 to true,
      target2 to true,
      target4 to false,
    )

    runTestWriteAction { file.rename(requestor, "bbb.java") }
    waiter.await()

    file.assertModelStatus(
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
    waiter.await()

    file.assertModelStatus(
      target1 to true,
      target2 to true,
      target3 to false,
    )

    runTestWriteAction { file.move(requestor, pack) }
    waiter.await()

    file.assertModelStatus(
      target1 to false,
      target2 to true,
      target3 to true,
    )
  }

  @Test
  fun `source file copy`() {
    val src = project.rootDir.createDirectory("src")
    val file = src.createFile("aaa", "java")
    waiter.await()

    file.assertModelStatus(
      target1 to true,
      target2 to true,
      target4 to false,
    )

    val newFile = runTestWriteAction { file.copy(requestor, src, "bbb.java") }
    waiter.await()

    file.assertModelStatus(
      target1 to true,
      target2 to true,
      target4 to false,
    )
    newFile.assertModelStatus(
      target1 to true,
      target2 to false,
      target4 to true,
    )
  }

  @Test
  fun `source file delete`() {
    val src = project.rootDir.createDirectory("src")
    val file = src.createFile("aaa", "java")
    waiter.await()

    file.assertModelStatus(
      target1 to true,
      target2 to true,
    )

    runTestWriteAction { file.delete(requestor) }
    waiter.await()

    file.assertModelStatus(
      target1 to false,
      target2 to false,
    )
  }

  @Test
  fun `should ignore non-source file`() {
    val fileListener = AssignFileToModuleListener()
    val file = project.rootDir.createDirectory("src").createFile("aaa", "txt")
    val event = createEvent(file)

    val job = fileListener.testableAfter(listOf(event))[project]
    job.shouldBeNull()
  }

  @Test
  fun `should ignore directory creation`() {
    val fileListener = AssignFileToModuleListener()
    val directory = project.rootDir.createDirectory("src")
    val event = createEvent(directory)

    val job = fileListener.testableAfter(listOf(event))[project]
    job.shouldBeNull()
  }

  @Test
  fun `should ignore multiple simultaneous events`() {
    val fileListener = AssignFileToModuleListener()
    val src = project.rootDir.createDirectory("src")
    val fileA = src.createFile("aaa", "java")
    waiter.await() // waiting for the real file listener to finish to avoid disposable errors
    val fileB = src.createFile("bbb", "java")
    waiter.await() // waiting for the real file listener to finish to avoid disposable errors

    val events = listOf(createEvent(fileA), createEvent(fileB))
    val job = fileListener.testableAfter(events)[project]

    job.shouldBeNull()
  }

  @Test
  fun `should ignore non-applicable events`() {
    val fileListener = AssignFileToModuleListener()
    val file = project.rootDir.createDirectory("src").createFile("aaa", "txt")
    val event1 = encodingChangeEvent(file)
    val event2 = contentChangeEvent(file)

    val job1 = fileListener.testableAfter(listOf(event1))[project]
    job1.shouldBeNull()

    val job2 = fileListener.testableAfter(listOf(event2))[project]
    job2.shouldBeNull()
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
        rootTypeId = JAVA_SOURCE_ROOT_ENTITY_TYPE_ID,
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
    waiter.await()

    // should not be added to target1's model
    target1.itsModuleContainsFile(fileUrl).shouldBeFalse()
    // but the rest of the actions should happen normally
    file.assertModelStatus(target2 to true)
    fileUrl.belongsToTarget(target1).shouldBeTrue()
  }

  private fun createEvent(file: VirtualFile): VFileCreateEvent =
    VFileCreateEvent(
      requestor,
      file.parent,
      file.name,
      false,
      null,
      null,
      emptyArray<ChildInfo>(),
    )

  private fun encodingChangeEvent(file: VirtualFile) =
    VFilePropertyChangeEvent(requestor, file, VirtualFile.PROP_ENCODING, Charsets.US_ASCII, Charsets.UTF_8)

  private fun contentChangeEvent(file: VirtualFile) = VFileContentChangeEvent(requestor, file, 0, 0)

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

  private fun VirtualFile.assertModelStatus(vararg expectedStates: Pair<Label, Boolean>) {
    this.toVirtualFileUrl(virtualFileUrlManager).assertModelStatus(*expectedStates)
  }

  private fun VirtualFileUrl.assertModelStatus(vararg expectedStates: Pair<Label, Boolean>) {
    expectedStates.forEach { (target, shouldBeAdded) ->
      target.itsModuleContainsFile(this).shouldBe(shouldBeAdded)
      this.belongsToTarget(target).shouldBe(shouldBeAdded)
    }
  }

  private fun VirtualFileUrl.belongsToTarget(target: Label): Boolean =
    project.targetUtils.fileToTarget[this.toPath()]?.contains(target) ?: false

  private fun Label.createModule(contentRootFiles: List<VirtualFile> = emptyList()) {
    val moduleName = this.formatAsModuleName(project)
    val entitySource = BspModuleEntitySource(moduleName)
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

  private fun Label.itsModuleContainsFile(fileUrl: VirtualFileUrl): Boolean {
    val module = workspaceModel.currentSnapshot.resolveModule(this)
    return module.contentRoots.any { it.url == fileUrl }
  }

  private fun ImmutableEntityStorage.resolveModule(target: Label): ModuleEntity {
    val moduleId = ModuleId(target.formatAsModuleName(project))
    return resolve(moduleId) ?: error("Module for $target does not exist")
  }

  private fun registerSyncWaiter() {
    project.targetUtils.registerSyncListener { runBlocking { waiter.send(Unit) } }
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

private fun Channel<Unit>.await() {
  runBlocking { receive() }
}
