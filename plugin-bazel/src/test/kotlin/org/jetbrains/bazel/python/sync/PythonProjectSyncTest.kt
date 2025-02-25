package org.jetbrains.bazel.python.sync

import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.util.progress.reportSequentialProgress
import com.intellij.platform.workspace.jps.entities.ContentRootEntity
import com.intellij.platform.workspace.jps.entities.DependencyScope
import com.intellij.platform.workspace.jps.entities.ModuleDependency
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.jps.entities.ModuleId
import com.intellij.platform.workspace.jps.entities.ModuleTypeId
import com.intellij.platform.workspace.jps.entities.SourceRootEntity
import com.intellij.platform.workspace.jps.entities.SourceRootTypeId
import com.intellij.platform.workspace.storage.url.VirtualFileUrlManager
import kotlinx.coroutines.runBlocking
import org.jetbrains.bazel.magicmetamodel.TargetNameReformatProvider
import org.jetbrains.bazel.magicmetamodel.findNameProvider
import org.jetbrains.bazel.magicmetamodel.orDefault
import org.jetbrains.bazel.sync.BaseTargetInfo
import org.jetbrains.bazel.sync.BaseTargetInfos
import org.jetbrains.bazel.sync.ProjectSyncHook
import org.jetbrains.bazel.sync.projectStructure.AllProjectStructuresProvider
import org.jetbrains.bazel.sync.projectStructure.workspaceModel.workspaceModelDiff
import org.jetbrains.bazel.sync.scope.SecondPhaseSync
import org.jetbrains.bazel.workspace.model.matchers.entries.ExpectedModuleEntity
import org.jetbrains.bazel.workspace.model.matchers.entries.ExpectedSourceRootEntity
import org.jetbrains.bazel.workspace.model.matchers.entries.shouldContainExactlyInAnyOrder
import org.jetbrains.bazel.workspace.model.test.framework.BuildServerMock
import org.jetbrains.bazel.workspace.model.test.framework.MockProjectBaseTest
import org.jetbrains.bazel.workspacemodel.entities.BspProjectEntitySource
import org.jetbrains.bazel.workspacemodel.entities.BuildTargetInfo
import org.jetbrains.bsp.protocol.BuildServerCapabilities
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.BuildTargetCapabilities
import org.jetbrains.bsp.protocol.BuildTargetIdentifier
import org.jetbrains.bsp.protocol.PythonBuildTarget
import org.jetbrains.bsp.protocol.ResourcesItem
import org.jetbrains.bsp.protocol.SourceItem
import org.jetbrains.bsp.protocol.SourceItemKind
import org.jetbrains.bsp.protocol.SourcesItem
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

private data class PythonTestSet(
  val baseTargetInfos: BaseTargetInfos,
  val expectedModuleEntities: List<ExpectedModuleEntity>,
  val expectedSourceRootEntities: List<ExpectedSourceRootEntity>,
)

private data class GeneratedTargetInfo(
  val targetId: BuildTargetIdentifier,
  val type: String,
  val dependencies: List<BuildTargetIdentifier> = listOf(),
  val resourcesItems: List<String> = listOf(),
)

class PythonProjectSyncTest : MockProjectBaseTest() {
  lateinit var hook: ProjectSyncHook
  lateinit var virtualFileUrlManager: VirtualFileUrlManager

  @BeforeEach
  override fun beforeEach() {
    super.beforeEach()
    // given
    hook = PythonProjectSync()
    virtualFileUrlManager = WorkspaceModel.getInstance(project).getVirtualFileUrlManager()
  }

  @Test
  fun `should add module with dependencies to workspace model diff`() {
    // given
    val server = BuildServerMock()
    val capabilities = BuildServerCapabilities()
    val diff = AllProjectStructuresProvider(project).newDiff()
    val pythonTestTargets = generateTestSet()

    // when
    runBlocking {
      reportSequentialProgress { reporter ->
        val environment =
          ProjectSyncHook.ProjectSyncHookEnvironment(
            project = project,
            syncScope = SecondPhaseSync,
            server = server,
            diff = diff,
            taskId = "test",
            progressReporter = reporter,
            baseTargetInfos = pythonTestTargets.baseTargetInfos,
          )
        hook.onSync(environment)
      }
    }

    // then
    val actualModuleEntities =
      diff.workspaceModelDiff.mutableEntityStorage
        .entities(ModuleEntity::class.java)
        .toList()
        .filter { it.type == ModuleTypeId("PYTHON_MODULE") }

    actualModuleEntities shouldContainExactlyInAnyOrder pythonTestTargets.expectedModuleEntities
  }

  @Test
  fun `should add module with sources to workspace model diff`() {
    // given
    val server = BuildServerMock()
    val capabilities = BuildServerCapabilities()
    val diff = AllProjectStructuresProvider(project).newDiff()
    val pythonTestTargets = generateTestSetWithSources()

    // when
    runBlocking {
      reportSequentialProgress { reporter ->
        val environment =
          ProjectSyncHook.ProjectSyncHookEnvironment(
            project = project,
            syncScope = SecondPhaseSync,
            server = server,
            diff = diff,
            taskId = "test",
            progressReporter = reporter,
            baseTargetInfos = pythonTestTargets.baseTargetInfos,
          )
        hook.onSync(environment)
      }
    }

    // then
    val actualModuleEntities =
      diff.workspaceModelDiff.mutableEntityStorage
        .entities(SourceRootEntity::class.java)
        .toList()

    actualModuleEntities shouldContainExactlyInAnyOrder pythonTestTargets.expectedSourceRootEntities
  }

  private fun generateTestSet(): PythonTestSet {
    val pythonLibrary1 =
      GeneratedTargetInfo(
        targetId = BuildTargetIdentifier("@@server.lib:lib1"),
        type = "library",
      )
    val pythonLibrary2 =
      GeneratedTargetInfo(
        targetId = BuildTargetIdentifier("@@server/lib:lib2"),
        type = "library",
      )
    val pythonBinary =
      GeneratedTargetInfo(
        targetId = BuildTargetIdentifier("@@server:main_app"),
        type = "PYTHON_MODULE",
        dependencies = listOf(pythonLibrary1.targetId, pythonLibrary2.targetId),
      )

    val targetInfos = listOf(pythonLibrary1, pythonLibrary2, pythonBinary)
    val targets = targetInfos.map { generateTarget(it) }
    val baseTargetInfos =
      BaseTargetInfos(
        allTargetIds = targets.map { it.target.id },
        infos =
          targets.map {
            BaseTargetInfo(it.target, it.sources, it.resources)
          },
      )
    val nameProvider = project.findNameProvider().orDefault()

    val expectedModuleEntity1 = generateExpectedModuleEntity(pythonBinary, listOf(pythonLibrary1, pythonLibrary2), nameProvider)
    val expectedModuleEntity2 = generateExpectedModuleEntity(pythonLibrary1, emptyList(), nameProvider)
    val expectedModuleEntity3 = generateExpectedModuleEntity(pythonLibrary2, emptyList(), nameProvider)
    return PythonTestSet(baseTargetInfos, listOf(expectedModuleEntity1, expectedModuleEntity2, expectedModuleEntity3), emptyList())
  }

  private fun generateTestSetWithSources(): PythonTestSet {
    val pythonBinary =
      GeneratedTargetInfo(
        targetId = BuildTargetIdentifier("@@server:main_app"),
        type = "PYTHON_MODULE",
        dependencies = listOf(),
      )

    val sources =
      listOf(
        SourcesItem(
          pythonBinary.targetId,
          listOf(
            SourceItem("file:///SomeSourceItemFile", SourceItemKind.FILE, true),
            SourceItem("file:///SomeSourceItemDirectory", SourceItemKind.DIRECTORY, true),
          ),
        ),
      )

    val resources = listOf(ResourcesItem(pythonBinary.targetId, listOf("file:///Resource1", "file:///Resource2", "file:///Resource3")))
    val target = generateTarget(pythonBinary)
    val baseTargetInfos =
      BaseTargetInfos(
        allTargetIds = listOf(target.target.id),
        infos = listOf(BaseTargetInfo(target.target, sources, resources)),
      )

    val expectedModuleEntity = generateExpectedModuleEntity(pythonBinary, emptyList(), project.findNameProvider().orDefault())

    val expectedContentRootEntities =
      generateExpectedSourceRootEntities(sources, resources, expectedModuleEntity.moduleEntity)
    return PythonTestSet(baseTargetInfos, listOf(expectedModuleEntity), expectedContentRootEntities)
  }

  private fun generateTarget(info: GeneratedTargetInfo): BaseTargetInfo {
    val target =
      BuildTarget(
        info.targetId,
        listOf(info.type),
        listOf("python"),
        info.dependencies,
        BuildTargetCapabilities(),
        displayName = info.targetId.toString(),
        baseDirectory = "file:///targets_base_dir",
        data =
          PythonBuildTarget(
            version = "3",
            interpreter = "/path/to/interpreter",
          ),
      )

    return BaseTargetInfo(target, emptyList(), emptyList())
  }

  private fun generateExpectedModuleEntity(
    targetInfo: GeneratedTargetInfo,
    dependenciesTargetInfo: List<GeneratedTargetInfo>,
    nameProvider: TargetNameReformatProvider,
  ): ExpectedModuleEntity =
    ExpectedModuleEntity(
      moduleEntity =
        ModuleEntity(
          name = nameProvider(BuildTargetInfo(id = targetInfo.targetId)),
          entitySource = BspProjectEntitySource,
          dependencies =
            dependenciesTargetInfo.map {
              ModuleDependency(
                module = ModuleId(nameProvider(BuildTargetInfo(id = it.targetId))),
                exported = true,
                scope = DependencyScope.COMPILE,
                productionOnTest = true,
              )
            },
        ) {
          type = ModuleTypeId("PYTHON_MODULE")
        },
    )

  private fun generateExpectedSourceRootEntities(
    sources: List<SourcesItem>,
    resources: List<ResourcesItem>,
    parentModuleEntity: ModuleEntity,
  ): List<ExpectedSourceRootEntity> =
    sources.flatMap {
      it.sources.map {
        val url = virtualFileUrlManager.getOrCreateFromUrl(it.uri)
        val sourceRootEntity = SourceRootEntity(url, SourceRootTypeId("python-source"), parentModuleEntity.entitySource)
        val contentRootEntity =
          ContentRootEntity(url, emptyList(), parentModuleEntity.entitySource) {
            excludedUrls = emptyList()
            sourceRoots = listOf(sourceRootEntity)
          }
        ExpectedSourceRootEntity(sourceRootEntity, contentRootEntity, parentModuleEntity)
      }
    } +
      resources.flatMap {
        it.resources.map {
          val url = virtualFileUrlManager.getOrCreateFromUrl(it)
          val sourceRootEntity = SourceRootEntity(url, SourceRootTypeId("python-resource"), parentModuleEntity.entitySource)
          val contentRootEntity =
            ContentRootEntity(url, emptyList(), parentModuleEntity.entitySource) {
              excludedUrls = emptyList()
              sourceRoots = listOf(sourceRootEntity)
            }
          ExpectedSourceRootEntity(sourceRootEntity, contentRootEntity, parentModuleEntity)
        }
      }
}
