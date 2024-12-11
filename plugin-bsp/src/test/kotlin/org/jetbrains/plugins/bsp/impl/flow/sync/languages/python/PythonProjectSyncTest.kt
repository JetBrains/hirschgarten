package org.jetbrains.plugins.bsp.impl.flow.sync.languages.python

import ch.epfl.scala.bsp4j.BuildTarget
import ch.epfl.scala.bsp4j.BuildTargetCapabilities
import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.PythonBuildTarget
import ch.epfl.scala.bsp4j.ResourcesItem
import ch.epfl.scala.bsp4j.SourceItem
import ch.epfl.scala.bsp4j.SourceItemKind
import ch.epfl.scala.bsp4j.SourcesItem
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
import org.jetbrains.bsp.protocol.BazelBuildServerCapabilities
import org.jetbrains.plugins.bsp.config.bspBuildToolId
import org.jetbrains.plugins.bsp.config.buildToolId
import org.jetbrains.plugins.bsp.impl.flow.sync.BaseTargetInfo
import org.jetbrains.plugins.bsp.impl.flow.sync.BaseTargetInfos
import org.jetbrains.plugins.bsp.impl.flow.sync.ProjectSyncHook
import org.jetbrains.plugins.bsp.impl.flow.sync.SecondPhaseSync
import org.jetbrains.plugins.bsp.projectStructure.AllProjectStructuresProvider
import org.jetbrains.plugins.bsp.projectStructure.workspaceModel.workspaceModelDiff
import org.jetbrains.plugins.bsp.workspacemodel.entities.BspProjectEntitySource
import org.jetbrains.workspace.model.matchers.entries.ExpectedModuleEntity
import org.jetbrains.workspace.model.matchers.entries.ExpectedSourceRootEntity
import org.jetbrains.workspace.model.matchers.entries.shouldContainExactlyInAnyOrder
import org.jetbrains.workspace.model.test.framework.BuildServerMock
import org.jetbrains.workspace.model.test.framework.MockProjectBaseTest
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
    project.buildToolId = bspBuildToolId
    hook = PythonProjectSync()
    virtualFileUrlManager = WorkspaceModel.getInstance(project).getVirtualFileUrlManager()
  }

  @Test
  fun `should add module with dependencies to workspace model diff`() {
    // given
    val server = BuildServerMock()
    val capabilities = BazelBuildServerCapabilities()
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
            capabilities = capabilities,
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
    val capabilities = BazelBuildServerCapabilities()
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
            capabilities = capabilities,
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
        targetId = BuildTargetIdentifier("@@server/lib:lib1"),
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

    val expectedModuleEntity1 = generateExpectedModuleEntity(pythonBinary, listOf(pythonLibrary1, pythonLibrary2))
    val expectedModuleEntity2 = generateExpectedModuleEntity(pythonLibrary1, emptyList())
    val expectedModuleEntity3 = generateExpectedModuleEntity(pythonLibrary2, emptyList())
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

    val expectedModuleEntity = generateExpectedModuleEntity(pythonBinary, emptyList())

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
      )
    target.displayName = target.id.toString()
    target.baseDirectory = "file:///targets_base_dir"
    target.dataKind = "python"
    target.data =
      PythonBuildTarget().also {
        it.version = "3"
        it.interpreter = "/path/to/interpreter"
      }

    return BaseTargetInfo(target, emptyList(), emptyList())
  }

  private fun generateExpectedModuleEntity(
    targetInfo: GeneratedTargetInfo,
    dependenciesTargetInfo: List<GeneratedTargetInfo>,
  ): ExpectedModuleEntity =
    ExpectedModuleEntity(
      moduleEntity =
        ModuleEntity(
          name = targetInfo.targetId.uri,
          entitySource = BspProjectEntitySource,
          dependencies =
            dependenciesTargetInfo.map {
              ModuleDependency(
                module = ModuleId(it.targetId.uri),
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
