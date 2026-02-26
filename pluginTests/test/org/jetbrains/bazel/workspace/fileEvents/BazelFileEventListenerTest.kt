@file:Suppress("UnstableApiUsage", "SameParameterValue")

package org.jetbrains.bazel.workspace.fileEvents

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.events.ChildInfo
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent
import com.intellij.openapi.vfs.newvfs.events.VFileCopyEvent
import com.intellij.openapi.vfs.newvfs.events.VFileCreateEvent
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.openapi.vfs.newvfs.events.VFileMoveEvent
import com.intellij.openapi.vfs.newvfs.events.VFilePropertyChangeEvent
import com.intellij.platform.backend.workspace.toVirtualFileUrl
import com.intellij.platform.workspace.jps.entities.ContentRootEntity
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.jps.entities.ModuleId
import com.intellij.platform.workspace.jps.entities.SourceRootEntity
import com.intellij.platform.workspace.jps.entities.SourceRootTypeId
import com.intellij.platform.workspace.jps.entities.modifyModuleEntity
import com.intellij.platform.workspace.storage.ImmutableEntityStorage
import com.intellij.platform.workspace.storage.url.VirtualFileUrl
import com.intellij.testFramework.common.timeoutRunBlocking
import com.intellij.testFramework.replaceService
import com.intellij.testFramework.workspaceModel.updateProjectModel
import com.intellij.workspaceModel.ide.toPath
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.magicmetamodel.formatAsModuleName
import org.jetbrains.bazel.server.connection.BazelServerConnection
import org.jetbrains.bazel.server.connection.BazelServerService
import org.jetbrains.bazel.target.targetUtils
import org.jetbrains.bazel.test.framework.target.TestBuildTargetFactory
import org.jetbrains.bazel.workspace.model.test.framework.BuildServerMock
import org.jetbrains.bazel.workspace.model.test.framework.WorkspaceModelBaseTest
import org.jetbrains.bazel.workspacemodel.entities.BazelModuleEntitySource
import org.jetbrains.bsp.protocol.InverseSourcesParams
import org.jetbrains.bsp.protocol.InverseSourcesResult
import org.jetbrains.bsp.protocol.BazelServerFacade
import org.jetbrains.bsp.protocol.PartialBuildTarget
import org.jetbrains.bsp.protocol.RawBuildTarget
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.relativeTo
import kotlin.time.Duration.Companion.seconds

class BazelFileEventListenerTest : WorkspaceModelBaseTest() {
  private val target1 = Label.parse(TARGET1)
  private val target2 = Label.parse(TARGET2)
  private val target3 = Label.parse(TARGET3)
  private val target4 = Label.parse(TARGET4)
  private val requestor = this
  private lateinit var inverseSourcesServer: InverseSourcesServer

  @BeforeEach
  override fun beforeEach() {
    super.beforeEach()
    project.isBazelProject = true
    project.rootDir = VirtualFileManager.getInstance().findFileByNioPath(Path(project.basePath!!))!!
    inverseSourcesServer = InverseSourcesServer(projectBasePath)
    project.replaceService(BazelServerService::class.java, inverseSourcesServer.serverService, disposable)
    addMockTargetToProject(project)

    target1.createModule()
    target2.createModule()
    target3.createModule()
    target4.createModule()

    project.targetUtils.setTargets(
      listOf(
        TestBuildTargetFactory.createSimpleJavaLibraryTarget(id = target1),
        TestBuildTargetFactory.createSimpleJavaLibraryTarget(id = target2),
        TestBuildTargetFactory.createSimpleJavaLibraryTarget(id = target3),
        TestBuildTargetFactory.createSimpleJavaLibraryTarget(id = target4),
      ),
    )
  }

  @Test
  fun `source file creation`() {
    val file = project.rootDir.createDirectory("src").createFile("aaa", "java")

    file.assertFileBelongsToTargets(
      target1 to false,
      target2 to false,
    )

    createEvent(file).process().assertProcessingAndAwait()

    file.assertFileBelongsToTargets(
      target1 to true,
      target2 to true,
    )
  }

  @Test
  fun `source file rename with extension change`() {
    val file = project.rootDir.createDirectory("src").createFile("aaa", "java")
    createEvent(file).process().assertProcessingAndAwait()

    file.assertFileBelongsToTargets(
      target1 to true,
      target2 to true,
      target4 to false,
    )

    runTestWriteAction { file.rename(requestor, "aaa.kt") }
    renameEvent(file, "aaa.java", "aaa.kt").process().assertProcessingAndAwait()

    file.assertFileBelongsToTargets(
      target1 to false,
      target2 to false,
      target4 to true,
    )
  }

  @Test
  fun `source file rename without extension change`() {
    val file = project.rootDir.createDirectory("src").createFile("aaa", "java")
    createEvent(file).process().assertProcessingAndAwait()
    inverseSourcesServer.called = false // file creation processing is just for technical purposes

    file.assertFileBelongsToTargets(
      target1 to true,
      target2 to true,
    )

    runTestWriteAction { file.rename(requestor, "bbb.java") }

    file.toVirtualFileUrl(virtualFileUrlManager).belongsToTarget(target1).shouldBeFalse()
    file.toVirtualFileUrl(virtualFileUrlManager).belongsToTarget(target2).shouldBeFalse()

    renameEvent(file, "aaa.java", "bbb.java").process().assertProcessingAndAwait()

    file.assertFileBelongsToTargets(
      target1 to true,
      target2 to true,
    )
    inverseSourcesServer.called.shouldBeFalse()
  }

  @Test
  fun `source file move`() {
    val src = project.rootDir.createDirectory("src")
    val file = src.createFile("aaa", "java")
    val pack = src.createDirectory("package")
    createEvent(file).process().assertProcessingAndAwait()

    file.assertFileBelongsToTargets(
      target1 to true,
      target2 to true,
    )

    val moveEvent = moveEvent(file, pack)
    runTestWriteAction { file.move(requestor, pack) }
    moveEvent.process().assertProcessingAndAwait()
    file.assertFileBelongsToTargets(
      target1 to false,
      target2 to true,
    )
  }

  @Test
  fun `source file copy`() {
    val src = project.rootDir.createDirectory("src")
    val file = src.createFile("aaa", "java")
    createEvent(file).process().assertProcessingAndAwait()

    file.assertFileBelongsToTargets(
      target1 to true,
      target2 to true,
      target4 to false,
    )

    val newFile = runTestWriteAction { file.copy(requestor, src, "bbb.java") }
    copyEvent(newFile, src, "bbb.java").process().assertProcessingAndAwait()

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
    createEvent(file).process().assertProcessingAndAwait()

    file.assertFileBelongsToTargets(
      target1 to true,
      target2 to true,
    )

    runTestWriteAction { file.delete(requestor) }
    deleteEvent(file).process().assertProcessingAndAwait()

    file.assertFileBelongsToTargets(
      target1 to false,
      target2 to false,
    )
  }

  @Test
  fun `multiple simultaneous file events should be processed`() {
    val src = project.rootDir.createDirectory("src")
    val pack = src.createDirectory("package")

    val fileCreated = src.createFile("bbb", "java")
    val fileMoved = src.createFile("aaa", "java")
    createEvent(fileMoved).process().assertProcessingAndAwait()

    fileMoved.assertFileBelongsToTargets(
      target1 to true,
      target2 to true,
      target3 to false,
    )
    fileCreated.assertFileBelongsToTargets(
      target1 to false,
      target4 to false,
    )

    val moveEvent = moveEvent(fileMoved, pack)
    runTestWriteAction { fileMoved.move(requestor, pack) }

    // the first and the last events should be ignored, but should not cause ignoring the two middle events
    processEvents(
      contentChangeEvent(fileMoved),
      createEvent(fileCreated),
      moveEvent,
      encodingChangeEvent(fileCreated),
    ).assertProcessingAndAwait()

    fileMoved.assertFileBelongsToTargets(
      target1 to false,
      target2 to true,
      target3 to true,
    )
    fileCreated.assertFileBelongsToTargets(
      target1 to true,
      target4 to true,
    )
  }

  @Test
  fun `should ignore non-source file`() {
    val file = project.rootDir.createDirectory("src").createFile("aaa", "txt")
    createEvent(file).process().assertNoProcessingHappened()
    deleteEvent(file).process().assertNoProcessingHappened()
  }

  @Test
  fun `should ignore directory events`() {
    val directory = project.rootDir.createDirectory("src")
    createEvent(directory).process().assertNoProcessingHappened()
    deleteEvent(directory).process().assertNoProcessingHappened()
  }

  @Test
  fun `should ignore non-applicable events`() {
    val file = project.rootDir.createDirectory("src").createFile("aaa", "txt")

    encodingChangeEvent(file).process().assertNoProcessingHappened()
    contentChangeEvent(file).process().assertNoProcessingHappened()
  }

  @Test
  fun `should ignore file creation inside excluded folders`() {
    val src = project.rootDir.createDirectory("src")
    val excluded = src.createExcludedDirectory("excluded")
    val file = excluded.createFile("aaa", "java")

    createEvent(file).process().assertNoProcessingHappened()
  }

  @Test
  fun `should ignore deletion from excluded folder`() {
    val src = project.rootDir.createDirectory("src")
    val excluded = src.createExcludedDirectory("excluded")
    val file = excluded.createFile("aaa", "java")

    runTestWriteAction { file.delete(requestor) }
    deleteEvent(file).process().assertNoProcessingHappened()
  }

  @Test
  fun `should ignore file move from excluded folder to non-excluded folder`() {
    val src = project.rootDir.createDirectory("src")
    val excluded = src.createExcludedDirectory("excluded")
    val pack = src.createDirectory("package")
    val file = excluded.createFile("aaa", "java")

    val moveEvent = moveEvent(file, pack)
    runTestWriteAction { file.move(requestor, pack) }

    moveEvent.process().assertNoProcessingHappened()
  }

  @Test
  fun `should ignore file move from non-excluded folder to excluded folder`() {
    val src = project.rootDir.createDirectory("src")
    val excluded = src.createExcludedDirectory("excluded")
    val pack = src.createDirectory("package")
    val file = pack.createFile("aaa", "java")

    val moveEvent = moveEvent(file, excluded)
    runTestWriteAction { file.move(requestor, excluded) }

    moveEvent.process().assertNoProcessingHappened()
  }

  @Test
  fun `should ignore file creation deep inside excluded folders`() {
    val src = project.rootDir.createDirectory("src")
    val excluded = src.createExcludedDirectory("excluded")
    val level2 = excluded.createDirectory("level2")
    val level3 = level2.createDirectory("level3")
    val file = level3.createFile("aaa", "java")

    createEvent(file).process().assertNoProcessingHappened()
  }

  @Test
  fun `should ignore deletion from non-existing folder deep inside an excluded folder`() {
    val src = project.rootDir.createDirectory("src")
    val excluded = src.createExcludedDirectory("excluded")
    val level2 = excluded.createDirectory("level2")
    val level3 = level2.createDirectory("level3")
    val file = level3.createFile("aaa", "java")

    runTestWriteAction { level2.delete(requestor) }
    deleteEvent(file).process().assertNoProcessingHappened()
  }

  @Test
  fun `should ignore projects without any targets`() {
    project.targetUtils.setTargets(emptyList())
    val file = project.rootDir.createDirectory("src").createFile("aaa", "java")
    createEvent(file).process().assertNoProcessingHappened()
    deleteEvent(file).process().assertNoProcessingHappened()
  }

  @Test
  fun `should ignore non-Bazel projects`() {
    project.isBazelProject = false
    val file = project.rootDir.createDirectory("src").createFile("aaa", "java")
    createEvent(file).process().assertNoProcessingHappened()
    deleteEvent(file).process().assertNoProcessingHappened()
  }

  @Test
  fun `should not add file to model if its parent is already there`() {
    val src = project.rootDir.createDirectory("src")
    val srcUrl = src.toVirtualFileUrl(virtualFileUrlManager)
    val module = workspaceModel.currentSnapshot.resolveModule(target1)

    project.targetUtils.saveTargets(
      targets =
        listOf(
          RawBuildTarget(
            id = target1,
            tags = emptyList(),
            dependencies = emptyList(),
            kind =
              TargetKind(
                kindString = "java_library",
                ruleType = RuleType.LIBRARY,
                languageClasses = setOf(LanguageClass.JAVA),
              ),
            sources = emptyList(),
            resources = emptyList(),
            baseDirectory = Path("/"),
          ),
        ),
      fileToTarget = emptyMap(),
      libraryItems = emptyList(),
    )

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
    createEvent(file).process().assertProcessingAndAwait()

    // Calling Bazel is too slow, we can skip it in this case
    inverseSourcesServer.called.shouldBeFalse()

    // should not be added to target1's model
    doesModuleContainFile(target1, fileUrl).shouldBeFalse()
    // but the rest of the actions should happen normally
    fileUrl.belongsToTarget(target1).shouldBeTrue()
  }

  @Test
  fun `should not start a new processing if another is ongoing`() {
    val blockedServer = BlockedServerSimulator()
    project.replaceService(BazelServerService::class.java, blockedServer.serverService, disposable)

    val src = project.rootDir.createDirectory("src")

    val fileA = src.createFile("aaa", "java")
    val eventA = createEvent(fileA).process()
    Thread.sleep(1_000)

    val fileB = src.createFile("bbb", "java")
    val eventB = createEvent(fileB).process()

    blockedServer.unblock()

    eventA.assertProcessingAndAwait()
    eventB.assertNoProcessingHappened()
  }

  private fun addMockTargetToProject(project: Project) {
    val mockLabel = Label.parse("//mock:target")
    val mockBuildTarget =
      PartialBuildTarget(
        id = mockLabel,
        tags = emptyList(),
        kind = TargetKind("mock", emptySet(), RuleType.LIBRARY),
        baseDirectory = projectBasePath,
      )
    project.targetUtils.setTargets(listOf(mockBuildTarget))
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

  private fun VirtualFile.createExcludedDirectory(name: String): VirtualFile {
    val directory = this.createDirectory(name)
    val module = workspaceModel.currentSnapshot.resolveModule(target1)
    val srcUrl = this.toVirtualFileUrl(virtualFileUrlManager)
    val contentRoot =
      ContentRootEntity(
        url = srcUrl,
        excludedPatterns = listOf("excluded"),
        entitySource = module.entitySource,
      )
    runTestWriteAction {
      workspaceModel.updateProjectModel { it.modifyModuleEntity(module) { contentRoots = listOf(contentRoot) } }
    }
    return directory
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

  private fun VFileEvent.process(): Deferred<Boolean>? = processEvents(this)

  private fun processEvents(vararg events: VFileEvent): Deferred<Boolean>? =
    BazelFileEventListener().process(events.toList())[project.locationHash]

  private fun VirtualFile.assertFileBelongsToTargets(vararg expectedBelongingStatus: Pair<Label, Boolean>) {
    this.toVirtualFileUrl(virtualFileUrlManager).assertFileBelongsToTargets(*expectedBelongingStatus)
  }

  private fun VirtualFileUrl.assertFileBelongsToTargets(vararg expectedBelongingStatus: Pair<Label, Boolean>) {
    expectedBelongingStatus.forEach { (target, shouldBeAdded) ->
      // separate variables help with debugging
      val projectModel = doesModuleContainFile(target, this)
      val ourModel = this.belongsToTarget(target)
      projectModel.shouldBe(shouldBeAdded)
      ourModel.shouldBe(shouldBeAdded)
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
  var called: Boolean = false

  private val inverseSourcesData =
    mapOf(
      "src/aaa.java" to listOf(TARGET1, TARGET2),
      "src/package/aaa.java" to listOf(TARGET3, TARGET2),
      "src/bbb.java" to listOf(TARGET1, TARGET4),
      "src/aaa.kt" to listOf(TARGET4),
    ).mapValues { it.value.map { target -> Label.parse(target) } }

  private val connection =
    object : BazelServerConnection {
      override suspend fun <T> runWithServer(task: suspend (BazelServerFacade) -> T): T = task(this@InverseSourcesServer)
    }

  val serverService =
    object : BazelServerService {
      override val connection: BazelServerConnection = this@InverseSourcesServer.connection
    }

  override suspend fun buildTargetInverseSources(inverseSourcesParams: InverseSourcesParams): InverseSourcesResult {
    called = true
    val results = inverseSourcesParams.files.associateWith {
      inverseSourcesData.getOrDefault(it.relativeTo(projectBasePath).toString(), emptyList())
    }
    return InverseSourcesResult(results)
  }
}

/** Not supposed to provide any valuable information - only simulates Bazel being blocked */
private class BlockedServerSimulator : BuildServerMock() {
  private val lock = Mutex(true)

  private val connection =
    object : BazelServerConnection {
      override suspend fun <T> runWithServer(task: suspend (BazelServerFacade) -> T): T = task(this@BlockedServerSimulator)
    }

  val serverService =
    object : BazelServerService {
      override val connection: BazelServerConnection = this@BlockedServerSimulator.connection
    }

  override suspend fun buildTargetInverseSources(inverseSourcesParams: InverseSourcesParams): InverseSourcesResult =
    lock.withLock {
      InverseSourcesResult(emptyMap())
    }

  fun unblock() {
    lock.unlock()
  }
}

private fun Deferred<Boolean>?.assertProcessingAndAwait() {
  val processingHappened = timeoutRunBlocking(timeout=15.seconds) { this@assertProcessingAndAwait.shouldNotBeNull().await() }
  processingHappened.shouldBeTrue()
}

private fun Deferred<Boolean>?.assertNoProcessingHappened() {
  if (this == null) return // no processing happened for this project
  val processingHappened = timeoutRunBlocking(timeout=5.seconds) { this@assertNoProcessingHappened.await() }
  processingHappened.shouldBeFalse()
}

private const val TARGET1 = "//src:target1"
private const val TARGET2 = "//src:target2"
private const val TARGET3 = "//src/package:target3"
private const val TARGET4 = "//src:target4"
