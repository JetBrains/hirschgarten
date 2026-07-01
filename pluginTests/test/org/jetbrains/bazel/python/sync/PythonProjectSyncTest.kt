package org.jetbrains.bazel.python.sync

import com.intellij.bazel.python.backend.chooseSdkName
import com.intellij.ide.util.ModuleRendererFactory
import com.intellij.openapi.application.edtWriteAction
import com.intellij.openapi.application.runReadActionBlocking
import com.intellij.openapi.diagnostic.fileLogger
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.util.progress.reportSequentialProgress
import com.intellij.platform.workspace.jps.entities.ContentRootEntity
import com.intellij.platform.workspace.jps.entities.DependencyScope
import com.intellij.platform.workspace.jps.entities.ModuleDependency
import com.intellij.platform.workspace.jps.entities.ModuleDependencyItem
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.jps.entities.ModuleId
import com.intellij.platform.workspace.jps.entities.ModuleSourceDependency
import com.intellij.platform.workspace.jps.entities.ModuleTypeId
import com.intellij.platform.workspace.jps.entities.SdkDependency
import com.intellij.platform.workspace.jps.entities.SdkId
import com.intellij.platform.workspace.jps.entities.SourceRootEntity
import com.intellij.platform.workspace.jps.entities.SourceRootTypeId
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.platform.workspace.storage.impl.url.toVirtualFileUrl
import com.intellij.platform.workspace.storage.url.VirtualFileUrlManager
import com.intellij.psi.PsiManager
import com.intellij.python.community.services.systemPython.SystemPythonService
import com.intellij.testFramework.common.timeoutRunBlocking
import com.intellij.testFramework.registerOrReplaceServiceInstance
import com.jetbrains.python.PythonBinary
import com.jetbrains.python.psi.PyFile
import com.jetbrains.python.sdk.PythonSdkUtil
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import org.jetbrains.bazel.commons.RepoMappingDisabled
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.label.DependencyLabel
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.magicmetamodel.formatAsModuleName
import org.jetbrains.bazel.progress.syncConsole
import org.jetbrains.bazel.project.BazelProjectFixtures.initializeBazelProject
import org.jetbrains.bazel.python.lang.PythonBuildTarget
import org.jetbrains.bazel.python.lang.PythonLanguageClass
import org.jetbrains.bazel.server.BazelServerService
import org.jetbrains.bazel.sync.environment.projectCtx
import org.jetbrains.bazel.sync.workspace.BazelResolvedWorkspace
import org.jetbrains.bazel.sync.workspace.importer.WorkspaceImporterHelper
import org.jetbrains.bazel.sync.workspace.snapshot.SourceFileCollectionBuilder
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSnapshot
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSnapshotBuilder
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey
import org.jetbrains.bazel.workspace.model.matchers.entries.ExpectedModuleEntity
import org.jetbrains.bazel.workspace.model.matchers.entries.ExpectedSourceRootEntity
import org.jetbrains.bazel.workspace.model.matchers.entries.shouldContainExactlyInAnyOrder
import org.jetbrains.bazel.workspace.model.test.framework.BuildServerMock
import org.jetbrains.bazel.workspace.model.test.framework.MockBuildServerService
import org.jetbrains.bazel.workspace.model.test.framework.MockProjectBaseTest
import org.jetbrains.bazel.workspace.model.test.framework.mockWorkspaceContext
import org.jetbrains.bazel.workspacemodel.entities.BazelProjectEntitySource
import org.jetbrains.bsp.protocol.RawBuildTarget
import org.jetbrains.bsp.protocol.SourceFileCollection
import org.jetbrains.bsp.protocol.TaskGroupId
import org.jetbrains.bsp.protocol.allSources
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText

private data class PythonTestSet(
  val workspaceSnapshot: WorkspaceSnapshot,
  val expectedModuleEntities: List<ExpectedModuleEntity>,
  val expectedSourceRootEntities: List<ExpectedSourceRootEntity>,
)

private data class GeneratedTargetInfo(
  val targetId: Label,
  val type: String,
  val dependencies: List<Label> = listOf(),
  val imports: List<String> = listOf(),
  val resourcesItems: List<String> = listOf(),
)

// TODO: convert it to real `BazelWorkspaceImporter` test fixture
class PythonProjectSyncTest : MockProjectBaseTest() {
  lateinit var virtualFileUrlManager: VirtualFileUrlManager
  lateinit var pythonBinary: PythonBinary

  private companion object {
    val logger = fileLogger()
  }

  @BeforeEach
  fun beforeEach() {
    // given
    initializeBazelProject(project, projectDir.get())
    virtualFileUrlManager = WorkspaceModel.getInstance(project).getVirtualFileUrlManager()

    // Python plugin validates file.
    // The real python should have been used, but in absence of it this path is good enough as most agents on TC are Ubuntu
    pythonBinary =
      timeoutRunBlocking { SystemPythonService().findSystemPythons().firstOrNull()?.pythonBinary ?: Path.of("/usr/bin/python3") }
    logger.info("Found python $pythonBinary for project ${project.name}")
  }

  // Drop SDKs created by this test
  @AfterEach
  fun cleanupSdk(): Unit = timeoutRunBlocking {
    edtWriteAction {
      val table = ProjectJdkTable.getInstance()
      for (sdk in PythonSdkUtil.getAllSdks()) {
        table.removeJdk(sdk)
      }
    }
  }

  @Test
  fun `should add module with dependencies to workspace model diff`() {
    // given
    val pythonTestTargets = generateTestSet()

    // when
    val diff = MutableEntityStorage.create()
    runPythonImporter(pythonTestTargets.workspaceSnapshot, diff)

    // then
    val actualModuleEntities =
      diff.entities(ModuleEntity::class.java)
        .toList()
        .filter { it.type == ModuleTypeId("PYTHON_MODULE") }
    logger.info("Checking for project ${project.name}")
    actualModuleEntities shouldContainExactlyInAnyOrder pythonTestTargets.expectedModuleEntities
    actualModuleEntities.shouldAllHaveTheSameSDK()
  }

  @Test
  fun `should add module with sources to workspace model diff`() {
    // given
    val pythonTestTargets = generateTestSetWithSources()

    // when
    val diff = MutableEntityStorage.create()
    runPythonImporter(pythonTestTargets.workspaceSnapshot, diff)

    // then
    val actualModuleEntities =
      diff.entities(SourceRootEntity::class.java)
        .toList()

    actualModuleEntities shouldContainExactlyInAnyOrder pythonTestTargets.expectedSourceRootEntities
  }

  @Test
  fun `should render Python symbols with package location in Bazel project`() {
    val pyFile = createPythonFile(
      relativePath = "aaa/bbb.py",
      text = """
        class Foo:
            def bar(self):
                pass
      """.trimIndent(),
    )
    runReadActionBlocking {
      val pyClass = pyFile.findTopLevelClass("Foo")!!
      val method = pyClass.findMethodByName("bar", false, null)!!

      renderModuleLocationText(pyClass) shouldBe "(aaa.bbb)"
      renderModuleLocationText(method) shouldBe "(Foo in aaa.bbb)"
    }
  }

  @Test
  fun `should render Python symbols with package location from Bazel imports`() {
    val relativePath = "src/aaa/bbb.py"
    val pyFile = createPythonFile(
      relativePath = relativePath,
      text = """
        class Foo:
            def bar(self):
                pass
      """.trimIndent(),
    )
    val pythonBinary =
      GeneratedTargetInfo(
        targetId = Label.parse("@@server//:main_app"),
        type = "PYTHON_MODULE",
        imports = listOf("src"),
      )
    val target =
      generateTarget(
        pythonBinary,
        sources = listOf(projectDir.get().resolve(relativePath)),
        generatedSources = emptyList(),
        resources = emptyList(),
      )

    project.projectCtx.bazelExecPath = projectDir.get()
    project.projectCtx.bazelBinPath = projectDir.get().resolve("bazel-bin")
    project.registerOrReplaceServiceInstance(BazelServerService::class.java, MockBuildServerService(BuildServerMock()), disposable)
    runPythonImporter(generateWorkspaceSnapshot(listOf(target)), MutableEntityStorage.create(), runPostProcessing = true)

    runReadActionBlocking {
      val pyClass = pyFile.findTopLevelClass("Foo")!!
      val method = pyClass.findMethodByName("bar", false, null)!!

      renderModuleLocationText(pyClass) shouldBe "(aaa.bbb)"
      renderModuleLocationText(method) shouldBe "(Foo in aaa.bbb)"
    }
  }

  @Test
  fun `should render shortest Python package location from Bazel imports`() {
    val relativePath = "src/app/aaa/bbb.py"
    val pyFile = createPythonFile(
      relativePath = relativePath,
      text = """
        class Foo:
            def bar(self):
                pass
      """.trimIndent(),
    )
    val pythonBinary =
      GeneratedTargetInfo(
        targetId = Label.parse("@@server//:main_app"),
        type = "PYTHON_MODULE",
        imports = listOf("src", "src/app"),
      )
    val target =
      generateTarget(
        pythonBinary,
        sources = listOf(projectDir.get().resolve(relativePath)),
        generatedSources = emptyList(),
        resources = emptyList(),
      )

    project.projectCtx.bazelExecPath = projectDir.get()
    project.projectCtx.bazelBinPath = projectDir.get().resolve("bazel-bin")
    project.registerOrReplaceServiceInstance(BazelServerService::class.java, MockBuildServerService(BuildServerMock()), disposable)
    runPythonImporter(generateWorkspaceSnapshot(listOf(target)), MutableEntityStorage.create(), runPostProcessing = true)

    runReadActionBlocking {
      val pyClass = pyFile.findTopLevelClass("Foo")!!
      val method = pyClass.findMethodByName("bar", false, null)!!

      renderModuleLocationText(pyClass) shouldBe "(aaa.bbb)"
      renderModuleLocationText(method) shouldBe "(Foo in aaa.bbb)"
    }
  }

  private fun renderModuleLocationText(element: Any): String =
    ModuleRendererFactory.findInstance(element).getModuleTextWithIcon(element)!!.text

  private fun runPythonImporter(
    snapshot: WorkspaceSnapshot,
    builder: MutableEntityStorage,
    runPostProcessing: Boolean = false,
  ) = runBlocking {
    //ExtensionTestUtil.maskExtensions(BazelWorkspaceImporter.EP_NAME, listOf(...))
    reportSequentialProgress { reporter ->
      val helper = WorkspaceImporterHelper(
        project = project,
        taskConsole = project.syncConsole,
        progressReporter = reporter,
        taskId = TaskGroupId.EMPTY.task("test"),
        builder = builder
      )
      helper.invoke(reporter, snapshot)
      if (runPostProcessing) {
        helper.invokeLate(reporter, snapshot)
      }
    }
  }

  private fun generateTestSet(): PythonTestSet {
    val pythonLibrary1 =
      GeneratedTargetInfo(
        targetId = Label.parse("@@server.lib//:lib1"),
        type = "library",
      )
    val pythonLibrary2 =
      GeneratedTargetInfo(
        targetId = Label.parse("@@server//lib:lib2"),
        type = "library",
      )
    val pythonBinary =
      GeneratedTargetInfo(
        targetId = Label.parse("@@server//:main_app"),
        type = "PYTHON_MODULE",
        dependencies = listOf(pythonLibrary1.targetId, pythonLibrary2.targetId),
      )

    val targetInfos = listOf(pythonLibrary1, pythonLibrary2, pythonBinary)
    val targets = targetInfos.map { generateTarget(it, emptyList(), emptyList(), emptyList()) }

    val expectedModuleEntity1 = generateExpectedModuleEntity(pythonBinary, listOf(pythonLibrary1, pythonLibrary2))
    val expectedModuleEntity2 = generateExpectedModuleEntity(pythonLibrary1, emptyList())
    val expectedModuleEntity3 = generateExpectedModuleEntity(pythonLibrary2, emptyList())
    return PythonTestSet(
      generateWorkspaceSnapshot(targets),
      listOf(expectedModuleEntity1, expectedModuleEntity2, expectedModuleEntity3),
      emptyList(),
    )
  }

  private fun generateTestSetWithSources(): PythonTestSet {
    val pythonBinary =
      GeneratedTargetInfo(
        targetId = Label.parse("@@server//:main_app"),
        type = "PYTHON_MODULE",
        dependencies = listOf(),
      )

    val target =
      generateTarget(
        pythonBinary,
        emptyList(),
        listOf(Path("/SomeSourceItemFile")),
        listOf(Path("/Resource1"), Path("/Resource2"), Path("/Resource3")),
      )

    val expectedModuleEntity = generateExpectedModuleEntity(pythonBinary, emptyList())

    val expectedContentRootEntities = generateExpectedSourceRootEntities(target, expectedModuleEntity.moduleEntity)
    return PythonTestSet(
      generateWorkspaceSnapshot(listOf(target)),
      listOf(expectedModuleEntity),
      expectedContentRootEntities,
    )
  }

  private fun generateTarget(
    info: GeneratedTargetInfo,
    sources: List<Path>,
    generatedSources: List<Path>,
    resources: List<Path>,
  ): RawBuildTarget {

    val target =
      RawBuildTarget(
        key = WorkspaceTargetKey(label = info.targetId),
        info.dependencies.map { DependencyLabel(WorkspaceTargetKey(label = it)) },
        TargetKind(
          kind = "python_binary",
          ruleType = RuleType.BINARY,
          languageClasses = setOf(PythonLanguageClass.PYTHON),
        ),
        baseDirectory = Path("/targets_base_dir"),
        data = listOf(
          PythonBuildTarget(
            version = "3",
            interpreter = pythonBinary,
            info.imports,
            SourceFileCollection.EMPTY,
            externalSources = SourceFileCollection.EMPTY,
          ),
        ),
        sources = SourceFileCollectionBuilder.build(sources),
        generatedSources = SourceFileCollectionBuilder.build(generatedSources),
        resources = SourceFileCollectionBuilder.build(resources),
      )

    return target
  }

  private fun generateWorkspaceSnapshot(targets: List<RawBuildTarget>): WorkspaceSnapshot = runBlocking {
    WorkspaceSnapshotBuilder.build(
      project = project,
      workspaceContext = mockWorkspaceContext,
      repoMapping = RepoMappingDisabled,
      resolved = BazelResolvedWorkspace(
        workspaceName = null,
        repoMapping = RepoMappingDisabled,
        rootTargets = targets.map { it.key }.toSet(),
        targets = targets,
        configurations = emptyMap()
      ),
    )
  }

  private fun generateExpectedModuleEntity(
    targetInfo: GeneratedTargetInfo,
    dependenciesTargetInfo: List<GeneratedTargetInfo>,
  ): ExpectedModuleEntity {
    val sdkName = chooseSdkName(pythonBinary, project.name)
    val sdkDependency: ModuleDependencyItem = SdkDependency(SdkId(sdkName, "PythonSDK"))
    val moduleDependencies: List<ModuleDependencyItem> =
      dependenciesTargetInfo.map {
        ModuleDependency(
          module = ModuleId(it.targetId.formatAsModuleName(project)),
          exported = true,
          scope = DependencyScope.COMPILE,
          productionOnTest = true,
        )
      }
    return ExpectedModuleEntity(
      moduleEntity =
        ModuleEntity(
          name = targetInfo.targetId.formatAsModuleName(project),
          entitySource = BazelProjectEntitySource,
          dependencies = moduleDependencies + ModuleSourceDependency + sdkDependency,
        ) {
          type = ModuleTypeId("PYTHON_MODULE")
        },
    )
  }

  private fun generateExpectedSourceRootEntities(target: RawBuildTarget, parentModuleEntity: ModuleEntity): List<ExpectedSourceRootEntity> =
    (target.allSources.map {
      val url = it.toVirtualFileUrl(virtualFileUrlManager)
      val sourceRootEntity = SourceRootEntity(url, SourceRootTypeId("python-source"), parentModuleEntity.entitySource)
      val contentRootEntity =
        ContentRootEntity(url, emptyList(), parentModuleEntity.entitySource) {
          excludedUrls = emptyList()
          sourceRoots = listOf(sourceRootEntity)
        }
      ExpectedSourceRootEntity(sourceRootEntity, contentRootEntity, parentModuleEntity)
    } +
     target.resources.getFiles().map {
       val url = it.toVirtualFileUrl(virtualFileUrlManager)
       val sourceRootEntity = SourceRootEntity(url, SourceRootTypeId("python-resource"), parentModuleEntity.entitySource)
       val contentRootEntity =
         ContentRootEntity(url, emptyList(), parentModuleEntity.entitySource) {
           excludedUrls = emptyList()
           sourceRoots = listOf(sourceRootEntity)
         }
       ExpectedSourceRootEntity(sourceRootEntity, contentRootEntity, parentModuleEntity)
     }).toList()

  private fun List<ModuleEntity>.shouldAllHaveTheSameSDK() {
    val sdks = this.map { module -> module.dependencies.firstNotNullOfOrNull { it as? SdkDependency } }
    sdks.any { it == null }.shouldBeFalse()
    sdks.distinct().size shouldBe 1
  }

  private fun createPythonFile(relativePath: String, text: String): PyFile {
    val path = projectDir.get().resolve(relativePath)
    path.parent.createDirectories()
    path.writeText(text)
    val virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(path)!!
    return runReadActionBlocking {
      PsiManager.getInstance(project).findFile(virtualFile) as PyFile
    }
  }
}
