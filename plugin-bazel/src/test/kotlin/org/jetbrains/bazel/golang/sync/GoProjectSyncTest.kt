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
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.label.CanonicalLabel
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.magicmetamodel.formatAsModuleName
import org.jetbrains.bazel.sdkcompat.workspacemodel.entities.BazelProjectEntitySource
import org.jetbrains.bazel.sync.ProjectSyncHook
import org.jetbrains.bazel.sync.projectStructure.AllProjectStructuresProvider
import org.jetbrains.bazel.sync.projectStructure.workspaceModel.workspaceModelDiff
import org.jetbrains.bazel.sync.scope.SecondPhaseSync
import org.jetbrains.bazel.workspace.model.test.framework.BuildServerMock
import org.jetbrains.bazel.workspace.model.test.framework.MockProjectBaseTest
import org.jetbrains.bsp.protocol.GoBuildTarget
import org.jetbrains.bsp.protocol.RawBuildTarget
import org.jetbrains.bsp.protocol.SourceItem
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetsResult
import org.jetbrains.bsp.protocol.WorkspaceGoLibrariesResult
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.nio.file.Path
import kotlin.io.path.Path

private data class GoTestSet(
  val buildTargets: WorkspaceBuildTargetsResult,
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
  val targetId: CanonicalLabel,
  val type: String,
  val dependencies: List<CanonicalLabel> = listOf(),
  val resourcesItems: List<Path> = listOf(),
  val importPath: String,
)

@Disabled("deprecating for new Go sync")
class GoProjectSyncTest : MockProjectBaseTest() {
  lateinit var hook: ProjectSyncHook

  @BeforeEach
  override fun beforeEach() {
    // given
    super.beforeEach()
    hook = GoProjectSync()
  }

  @Test
  fun `should add VgoStandaloneModuleEntities to workspace model diff`() {
    // given
    val goTestTargets = generateTestSet()
    val server =
      BuildServerMock(
        workspaceBuildTargetsResult = goTestTargets.buildTargets,
        workspaceGoLibrariesResult = WorkspaceGoLibrariesResult(emptyList()),
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
            // TODO: not used yet, https://youtrack.jetbrains.com/issue/BAZEL-1961
            buildTargets = emptyMap(),
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
    val goTestTargets = generateTestSet()
    val server =
      BuildServerMock(
        workspaceBuildTargetsResult = goTestTargets.buildTargets,
        workspaceGoLibrariesResult = WorkspaceGoLibrariesResult(emptyList()),
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
            // TODO: not used yet, https://youtrack.jetbrains.com/issue/BAZEL-1961
            buildTargets = emptyMap(),
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

  private fun generateTestSet(): GoTestSet {
    val goLibrary1 =
      GeneratedTargetInfo(
        targetId = Label.parseCanonical("@@server/lib:hello_lib"),
        type = "library",
        importPath = "server/lib/file1.go",
      )
    val goLibrary2 =
      GeneratedTargetInfo(
        targetId = Label.parseCanonical("@@server/parser:parser_lib"),
        dependencies = listOf(goLibrary1.targetId),
        type = "library",
        importPath = "server/lib/file1.go",
      )
    val goApplication =
      GeneratedTargetInfo(
        targetId = Label.parseCanonical("@@server:main_app"),
        type = "application",
        dependencies = listOf(goLibrary1.targetId, goLibrary2.targetId),
        importPath = "server/main_file.go",
      )

    val targetInfos = listOf(goLibrary1, goLibrary2, goApplication)
    val targets = targetInfos.map { generateTarget(it) }
    val buildTargets = WorkspaceBuildTargetsResult(targets, false)
    val virtualFileUrlManager = WorkspaceModel.getInstance(project).getVirtualFileUrlManager()

    val expectedRoot = Path("/targets_base_dir").toVirtualFileUrl(virtualFileUrlManager)
    val expectedVgoStandaloneEntities = targetInfos.map { generateVgoStandaloneResult(it, expectedRoot) }
    val expectedVgoDependencyEntities =
      listOf(
        generateVgoDependencyResult(goLibrary1, goLibrary2, expectedRoot),
        generateVgoDependencyResult(goLibrary1, goApplication, expectedRoot),
        generateVgoDependencyResult(goLibrary2, goApplication, expectedRoot),
      )
    return GoTestSet(buildTargets, expectedVgoStandaloneEntities, expectedVgoDependencyEntities)
  }

  private fun generateTarget(info: GeneratedTargetInfo): RawBuildTarget =
    RawBuildTarget(
      info.targetId,
      listOf(info.type),
      info.dependencies,
      TargetKind(
        kindString = "go_binary",
        ruleType = RuleType.BINARY,
        languageClasses = setOf(LanguageClass.GO),
      ),
      baseDirectory = Path("/targets_base_dir"),
      data =
        GoBuildTarget(
          sdkHomePath = Path("/go_sdk/"),
          importPath = info.importPath,
          generatedLibraries = emptyList(),
          generatedSources = emptyList(),
          libraryLabels = emptyList(),
        ),
      sources = listOf(SourceItem(Path("/root/${info.importPath}"), false)),
      resources = info.resourcesItems,
    )

  private fun generateVgoStandaloneResult(info: GeneratedTargetInfo, expectedRoot: VirtualFileUrl): ExpectedVgoStandaloneModuleEntity =
    ExpectedVgoStandaloneModuleEntity(
      moduleId = ModuleId(info.targetId.formatAsModuleName(project)),
      entitySource = BazelProjectEntitySource,
      importPath = info.importPath,
      root = expectedRoot,
    )

  private fun generateVgoDependencyResult(
    dependencyInfo: GeneratedTargetInfo,
    parentInfo: GeneratedTargetInfo,
    expectedRoot: VirtualFileUrl,
  ): ExpectedVgoDependencyEntity =
    ExpectedVgoDependencyEntity(
      importPath = dependencyInfo.importPath,
      entitySource = BazelProjectEntitySource,
      isMainModule = false,
      internal = true,
      module = generateVgoStandaloneResult(parentInfo, expectedRoot),
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
