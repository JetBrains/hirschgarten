package org.jetbrains.bazel.golang.sync

import com.goide.vgo.project.workspaceModel.entities.VgoDependencyEntity
import com.goide.vgo.project.workspaceModel.entities.VgoStandaloneModuleEntity
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.util.progress.reportSequentialProgress
import com.intellij.platform.workspace.jps.entities.ModuleId
import com.intellij.platform.workspace.storage.EntitySource
import com.intellij.platform.workspace.storage.impl.url.toVirtualFileUrl
import com.intellij.platform.workspace.storage.url.VirtualFileUrl
import io.kotest.matchers.shouldBe
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
import org.jetbrains.bazel.workspace.model.test.framework.BuildServerMock
import org.jetbrains.bazel.workspace.model.test.framework.MockProjectBaseTest
import org.jetbrains.bazel.workspacemodel.entities.BspProjectEntitySource
import org.jetbrains.bazel.workspacemodel.entities.BuildTargetInfo
import org.jetbrains.bsp.protocol.BuildServerCapabilities
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.BuildTargetCapabilities
import org.jetbrains.bsp.protocol.BuildTargetIdentifier
import org.jetbrains.bsp.protocol.GoBuildTarget
import org.jetbrains.bsp.protocol.ResourcesItem
import org.jetbrains.bsp.protocol.SourceItem
import org.jetbrains.bsp.protocol.SourceItemKind
import org.jetbrains.bsp.protocol.SourcesItem
import org.jetbrains.workspace.model.test.framework.BuildServerMock
import org.jetbrains.workspace.model.test.framework.MockProjectBaseTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.net.URI
import kotlin.io.path.toPath

private data class GoTestSet(
  val baseTargetInfos: BaseTargetInfos,
  val expectedVgoStandaloneEntities: List<ExpectedVgoStandaloneModuleEntity>,
  val expectedVgoDependencyEntities: List<ExpectedVgoDependencyEntity>,
)

private data class ExpectedVgoStandaloneModuleEntity(
  val moduleId: ModuleId,
  val entitySource: EntitySource,
  val importPath: String,
  val root: VirtualFileUrl,
)

private data class ExpectedVgoDependencyEntity(
  val importPath: String,
  val entitySource: EntitySource,
  val isMainModule: Boolean,
  val internal: Boolean,
  val module: ExpectedVgoStandaloneModuleEntity,
  val root: VirtualFileUrl,
)

private data class GeneratedTargetInfo(
  val targetId: BuildTargetIdentifier,
  val type: String,
  val dependencies: List<BuildTargetIdentifier> = listOf(),
  val resourcesItems: List<String> = listOf(),
  val importPath: String,
)

class GoProjectSyncTest : MockProjectBaseTest() {
  lateinit var hook: ProjectSyncHook

  @BeforeEach
  override fun beforeEach() {
    // given
    hook = GoProjectSync()
  }

  @Test
  fun `should add VgoStandaloneModuleEntities to workspace model diff`() {
    // given
    val server = BuildServerMock()
    val capabilities = BuildServerCapabilities()
    val diff = AllProjectStructuresProvider(project).newDiff()
    val goTestTargets = generateTestSet(project.findNameProvider().orDefault())

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
            baseTargetInfos = goTestTargets.baseTargetInfos,
          )
        hook.onSync(environment)
      }
    }

    // then
    val actualVgoStandaloneEntitiesResult =
      diff.workspaceModelDiff.mutableEntityStorage
        .entities(
          VgoStandaloneModuleEntity::class.java,
        ).toList()
    actualVgoStandaloneEntitiesResult.shouldBeEqual(goTestTargets.expectedVgoStandaloneEntities) { actualEntity, expectedEntity ->
      actualEntity shouldBeEqual expectedEntity
    }
  }

  @Test
  fun `should add dependencies to workspace model diff`() {
    // given
    val server = BuildServerMock()
    val capabilities = BuildServerCapabilities()
    val diff = AllProjectStructuresProvider(project).newDiff()
    val goTestTargets = generateTestSet(project.findNameProvider().orDefault())

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
            baseTargetInfos = goTestTargets.baseTargetInfos,
          )
        hook.onSync(environment)
      }
    }

    // then
    val actualVgoDependencyEntity =
      diff.workspaceModelDiff.mutableEntityStorage
        .entities(VgoDependencyEntity::class.java)
        .toList()
    actualVgoDependencyEntity.shouldBeEqual(goTestTargets.expectedVgoDependencyEntities) { actualEntity, expectedEntity ->
      actualEntity shouldBeEqual expectedEntity
    }
  }

  private fun generateTestSet(nameProvider: TargetNameReformatProvider): GoTestSet {
    val goLibrary1 =
      GeneratedTargetInfo(
        targetId = BuildTargetIdentifier("@@server/lib:hello_lib"),
        type = "library",
        importPath = "server/lib/file1.go",
      )
    val goLibrary2 =
      GeneratedTargetInfo(
        targetId = BuildTargetIdentifier("@@server/parser:parser_lib"),
        dependencies = listOf(goLibrary1.targetId),
        type = "library",
        importPath = "server/lib/file1.go",
      )
    val goApplication =
      GeneratedTargetInfo(
        targetId = BuildTargetIdentifier("@@server:main_app"),
        type = "application",
        dependencies = listOf(goLibrary1.targetId, goLibrary2.targetId),
        importPath = "server/main_file.go",
      )

    val targetInfos = listOf(goLibrary1, goLibrary2, goApplication)
    val targets = targetInfos.map { generateTarget(it) }
    val baseTargetInfos =
      BaseTargetInfos(
        allTargetIds = targets.map { it.target.id },
        infos =
          targets.map {
            BaseTargetInfo(it.target, it.sources, it.resources)
          },
      )
    val virtualFileUrlManager = WorkspaceModel.getInstance(project).getVirtualFileUrlManager()

    val expectedRoot = URI.create("file:///targets_base_dir").toPath().toVirtualFileUrl(virtualFileUrlManager)
    val expectedVgoStandaloneEntities = targetInfos.map { generateVgoStandaloneResult(it, expectedRoot, nameProvider) }
    val expectedVgoDependencyEntities =
      listOf(
        generateVgoDependencyResult(goLibrary1, goLibrary2, expectedRoot, nameProvider),
        generateVgoDependencyResult(goLibrary1, goApplication, expectedRoot, nameProvider),
        generateVgoDependencyResult(goLibrary2, goApplication, expectedRoot, nameProvider),
      )
    return GoTestSet(baseTargetInfos, expectedVgoStandaloneEntities, expectedVgoDependencyEntities)
  }

  private fun generateTarget(info: GeneratedTargetInfo): BaseTargetInfo {
    val target =
      BuildTarget(
        info.targetId,
        listOf(info.type),
        listOf("go"),
        info.dependencies,
        BuildTargetCapabilities(),
        displayName = info.targetId.toString(),
        baseDirectory = "file:///targets_base_dir",
        data =
          GoBuildTarget(
            sdkHomePath = URI("file:///go_sdk/"),
            importPath = info.importPath,
            generatedLibraries = emptyList(),
          ),
      )

    val sources =
      listOf(SourcesItem(info.targetId, listOf(SourceItem("file:///root/${info.importPath}", SourceItemKind.FILE, false))))
    val resources = info.resourcesItems.map { ResourcesItem(info.targetId, listOf(it)) }
    return BaseTargetInfo(target, sources, resources)
  }

  private fun generateVgoStandaloneResult(
    info: GeneratedTargetInfo,
    expectedRoot: VirtualFileUrl,
    nameProvider: TargetNameReformatProvider,
  ): ExpectedVgoStandaloneModuleEntity =
    ExpectedVgoStandaloneModuleEntity(
      moduleId = ModuleId(nameProvider(BuildTargetInfo(id = info.targetId))),
      entitySource = BspProjectEntitySource,
      importPath = info.importPath,
      root = expectedRoot,
    )

  private fun generateVgoDependencyResult(
    dependencyInfo: GeneratedTargetInfo,
    parentInfo: GeneratedTargetInfo,
    expectedRoot: VirtualFileUrl,
    nameProvider: TargetNameReformatProvider,
  ): ExpectedVgoDependencyEntity =
    ExpectedVgoDependencyEntity(
      importPath = dependencyInfo.importPath,
      entitySource = BspProjectEntitySource,
      isMainModule = false,
      internal = true,
      module = generateVgoStandaloneResult(parentInfo, expectedRoot, nameProvider),
      root = expectedRoot,
    )

  private inline fun <reified T, reified E> List<T>.shouldBeEqual(expected: List<E>, crossinline compare: (T, E) -> Unit) {
    if (this.size != expected.size) {
      throw AssertionError("Expected size ${expected.size} but got ${this.size}")
    }
    this.zip(expected).forEach { (actualEntity, expectedEntity) -> compare(actualEntity, expectedEntity) }
  }

  private infix fun VgoStandaloneModuleEntity.shouldBeEqual(expected: ExpectedVgoStandaloneModuleEntity) {
    this.moduleId shouldBe expected.moduleId
    this.importPath shouldBe expected.importPath
    this.root shouldBe expected.root
  }

  private infix fun VgoDependencyEntity.shouldBeEqual(expected: ExpectedVgoDependencyEntity) {
    this.importPath shouldBe expected.importPath
    this.isMainModule shouldBe expected.isMainModule
    this.internal shouldBe expected.internal
    this.module?.shouldBeEqual(expected.module)
    this.root shouldBe expected.root
  }
}
