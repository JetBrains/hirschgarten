package org.jetbrains.bazel.python.sync

import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.util.progress.reportSequentialProgress
import com.intellij.platform.workspace.jps.entities.ContentRootEntity
import com.intellij.platform.workspace.jps.entities.DependencyScope
import com.intellij.platform.workspace.jps.entities.ModuleDependency
import com.intellij.platform.workspace.jps.entities.ModuleDependencyItem
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.jps.entities.ModuleId
import com.intellij.platform.workspace.jps.entities.ModuleTypeId
import com.intellij.platform.workspace.jps.entities.SdkDependency
import com.intellij.platform.workspace.jps.entities.SdkId
import com.intellij.platform.workspace.jps.entities.SourceRootEntity
import com.intellij.platform.workspace.jps.entities.SourceRootTypeId
import com.intellij.platform.workspace.storage.impl.url.toVirtualFileUrl
import com.intellij.platform.workspace.storage.url.VirtualFileUrlManager
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.magicmetamodel.formatAsModuleName
import org.jetbrains.bazel.sync.ProjectSyncHook
import org.jetbrains.bazel.sync.projectStructure.AllProjectStructuresProvider
import org.jetbrains.bazel.sync.projectStructure.workspaceModel.workspaceModelDiff
import org.jetbrains.bazel.sync.scope.SecondPhaseSync
import org.jetbrains.bazel.workspace.model.matchers.entries.ExpectedModuleEntity
import org.jetbrains.bazel.workspace.model.matchers.entries.ExpectedSourceRootEntity
import org.jetbrains.bazel.workspace.model.matchers.entries.shouldContainExactlyInAnyOrder
import org.jetbrains.bazel.workspace.model.test.framework.BuildServerMock
import org.jetbrains.bazel.workspace.model.test.framework.MockProjectBaseTest
import org.jetbrains.bazel.workspacemodel.entities.BazelProjectEntitySource
import org.jetbrains.bsp.protocol.DependencySourcesResult
import org.jetbrains.bsp.protocol.PythonBuildTarget
import org.jetbrains.bsp.protocol.RawBuildTarget
import org.jetbrains.bsp.protocol.SourceItem
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetsResult
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Path
import kotlin.io.path.Path

private data class PythonTestSet(
  val buildTargets: WorkspaceBuildTargetsResult,
  val expectedModuleEntities: List<ExpectedModuleEntity>,
  val expectedSourceRootEntities: List<ExpectedSourceRootEntity>,
)

private data class GeneratedTargetInfo(
  val targetId: Label,
  val type: String,
  val dependencies: List<Label> = listOf(),
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
    val pythonTestTargets = generateTestSet()
    val server =
      BuildServerMock(
        workspaceBuildTargetsResult = pythonTestTargets.buildTargets,
        dependencySourcesResult = DependencySourcesResult(emptyList()),
      )
    val diff = AllProjectStructuresProvider(project).newDiff()

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
            // TODO: not used yet, https://youtrack.jetbrains.com/issue/BAZEL-1960
            buildTargets = emptyMap(),
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
    actualModuleEntities.shouldAllHaveTheSameSDK()
  }

  @Test
  fun `should add module with sources to workspace model diff`() {
    // given
    val pythonTestTargets = generateTestSetWithSources()
    val server =
      BuildServerMock(
        workspaceBuildTargetsResult = pythonTestTargets.buildTargets,
        dependencySourcesResult = DependencySourcesResult(emptyList()),
      )
    val diff = AllProjectStructuresProvider(project).newDiff()

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
            // TODO: not used yet, https://youtrack.jetbrains.com/issue/BAZEL-1960
            buildTargets = emptyMap(),
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
        targetId = Label.parse("@@server.lib:lib1"),
        type = "library",
      )
    val pythonLibrary2 =
      GeneratedTargetInfo(
        targetId = Label.parse("@@server/lib:lib2"),
        type = "library",
      )
    val pythonBinary =
      GeneratedTargetInfo(
        targetId = Label.parse("@@server:main_app"),
        type = "PYTHON_MODULE",
        dependencies = listOf(pythonLibrary1.targetId, pythonLibrary2.targetId),
      )

    val targetInfos = listOf(pythonLibrary1, pythonLibrary2, pythonBinary)
    val targets = targetInfos.map { generateTarget(it, emptyList(), emptyList()) }

    val expectedModuleEntity1 = generateExpectedModuleEntity(pythonBinary, listOf(pythonLibrary1, pythonLibrary2))
    val expectedModuleEntity2 = generateExpectedModuleEntity(pythonLibrary1, emptyList())
    val expectedModuleEntity3 = generateExpectedModuleEntity(pythonLibrary2, emptyList())
    return PythonTestSet(
      WorkspaceBuildTargetsResult(
        targets,
        hasError = false,
      ),
      listOf(expectedModuleEntity1, expectedModuleEntity2, expectedModuleEntity3),
      emptyList(),
    )
  }

  private fun generateTestSetWithSources(): PythonTestSet {
    val pythonBinary =
      GeneratedTargetInfo(
        targetId = Label.parse("@@server:main_app"),
        type = "PYTHON_MODULE",
        dependencies = listOf(),
      )

    val target =
      generateTarget(
        pythonBinary,
        listOf(SourceItem(Path("/SomeSourceItemFile"), true)),
        listOf(Path("/Resource1"), Path("/Resource2"), Path("/Resource3")),
      )

    val expectedModuleEntity = generateExpectedModuleEntity(pythonBinary, emptyList())

    val expectedContentRootEntities =
      generateExpectedSourceRootEntities(target, expectedModuleEntity.moduleEntity)
    return PythonTestSet(
      WorkspaceBuildTargetsResult(
        listOf(target),
        hasError = false,
      ),
      listOf(expectedModuleEntity),
      expectedContentRootEntities,
    )
  }

  private fun generateTarget(
    info: GeneratedTargetInfo,
    sources: List<SourceItem>,
    resources: List<Path>,
  ): RawBuildTarget {
    val target =
      RawBuildTarget(
        info.targetId,
        listOf(info.type),
        info.dependencies,
        TargetKind(
          kindString = "python_binary",
          ruleType = RuleType.BINARY,
          languageClasses = setOf(LanguageClass.PYTHON),
        ),
        baseDirectory = Path("/targets_base_dir"),
        data =
          PythonBuildTarget(
            version = "3",
            interpreter = Path(PYTHON_INTERPRETER),
          ),
        sources = sources,
        resources = resources,
      )

    return target
  }

  private fun generateExpectedModuleEntity(
    targetInfo: GeneratedTargetInfo,
    dependenciesTargetInfo: List<GeneratedTargetInfo>,
  ): ExpectedModuleEntity {
    val sdkDependency: ModuleDependencyItem = SdkDependency(SdkId("${project.name}-python-$PYTHON_INTERPRETER_MD5", "PythonSDK"))
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
          dependencies = moduleDependencies + sdkDependency,
        ) {
          type = ModuleTypeId("PYTHON_MODULE")
        },
    )
  }

  private fun generateExpectedSourceRootEntities(target: RawBuildTarget, parentModuleEntity: ModuleEntity): List<ExpectedSourceRootEntity> =
    target.sources.map {
      val url = it.path.toVirtualFileUrl(virtualFileUrlManager)
      val sourceRootEntity = SourceRootEntity(url, SourceRootTypeId("python-source"), parentModuleEntity.entitySource)
      val contentRootEntity =
        ContentRootEntity(url, emptyList(), parentModuleEntity.entitySource) {
          excludedUrls = emptyList()
          sourceRoots = listOf(sourceRootEntity)
        }
      ExpectedSourceRootEntity(sourceRootEntity, contentRootEntity, parentModuleEntity)
    } +
      target.resources.map {
        val url = it.toVirtualFileUrl(virtualFileUrlManager)
        val sourceRootEntity = SourceRootEntity(url, SourceRootTypeId("python-resource"), parentModuleEntity.entitySource)
        val contentRootEntity =
          ContentRootEntity(url, emptyList(), parentModuleEntity.entitySource) {
            excludedUrls = emptyList()
            sourceRoots = listOf(sourceRootEntity)
          }
        ExpectedSourceRootEntity(sourceRootEntity, contentRootEntity, parentModuleEntity)
      }

  private fun List<ModuleEntity>.shouldAllHaveTheSameSDK() {
    val sdks = this.map { module -> module.dependencies.firstNotNullOfOrNull { it as? SdkDependency } }
    sdks.any { it == null }.shouldBeFalse()
    sdks.distinct().size shouldBe 1
  }
}

private const val PYTHON_INTERPRETER = "/path/to/interpreter"
private const val PYTHON_INTERPRETER_MD5 = "efdb3"
