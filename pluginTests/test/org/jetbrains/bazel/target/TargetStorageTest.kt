package org.jetbrains.bazel.target

import com.intellij.configurationStore.SettingsSavingComponent
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.testFramework.junit5.TestApplication
import com.intellij.testFramework.junit5.fixture.projectFixture
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.kotest.matchers.types.shouldNotBeSameInstanceAs
import kotlinx.coroutines.runBlocking
import org.jetbrains.bazel.commons.RepoMappingDisabled
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.label.DependencyLabel
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.python.lang.PythonBuildTarget
import org.jetbrains.bazel.sync.JavaLanguageClass
import org.jetbrains.bazel.sync.workspace.languages.jvm.JavaToolchainData
import org.jetbrains.bazel.sync.workspace.languages.jvm.JvmBuildTarget
import org.jetbrains.bazel.sync.workspace.persistence.InMemoryWorkspaceTargetMap
import org.jetbrains.bazel.sync.workspace.persistence.WorkspaceSnapshotService
import org.jetbrains.bazel.sync.workspace.snapshot.CommonWorkspaceSyncConfig
import org.jetbrains.bazel.sync.workspace.snapshot.ExecutableTargetsIndexBuilder
import org.jetbrains.bazel.sync.workspace.snapshot.File2TargetMapBuilder
import org.jetbrains.bazel.sync.workspace.snapshot.SourceFileCollectionBuilder
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceConfigurationId
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSnapshot
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSnapshotMetadata
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTarget
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetGraphBuilder
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey
import org.jetbrains.bsp.protocol.BuildTargetData
import org.jetbrains.bsp.protocol.RawBuildTarget
import org.jetbrains.bsp.protocol.SourceFileCollection
import org.junit.jupiter.api.Test
import java.nio.file.Path

@TestApplication
class TargetStorageTest {

  private val projectFixture = projectFixture()
  private val project by projectFixture

  private suspend fun publish(project: Project, snapshot: WorkspaceSnapshot) {
    project.service<WorkspaceSnapshotService>().update { snapshot }
  }

  private suspend fun WorkspaceSnapshotService.save() = (this as SettingsSavingComponent).save()

  private fun target(
    label: String,
    deps: List<String> = emptyList(),
    executable: Boolean = false,
    data: List<BuildTargetData> = emptyList(),
    isManual: Boolean = false,
    isWorkspace: Boolean = true,
    baseDirectory: Path = Path.of("/workspace"),
    sources: SourceFileCollection = SourceFileCollection.EMPTY,
    key: WorkspaceTargetKey = WorkspaceTargetKey(label = Label.parse(label)),
  ): WorkspaceTarget =
    WorkspaceTarget(
      targetKey = key,
      rawBuildTarget = RawBuildTarget(
        key = key,
        dependencies = deps.map { DependencyLabel(targetKey = WorkspaceTargetKey(label = Label.parse(it))) },
        kind = TargetKind(
          kind = if (executable) "java_binary" else "java_library",
          ruleType = if (executable) RuleType.BINARY else RuleType.LIBRARY,
          languageClasses = setOf(JavaLanguageClass.JAVA),
        ),
        sources = sources,
        generatedSources = SourceFileCollection.EMPTY,
        resources = SourceFileCollection.EMPTY,
        baseDirectory = baseDirectory,
        data = data,
        isManual = isManual,
        isWorkspace = isWorkspace,
      ),
    )

  private fun snapshot(
    project: Project,
    targets: List<WorkspaceTarget>,
    roots: List<WorkspaceTarget> = targets,
    importDepth: Int = -1,
  ): WorkspaceSnapshot {
    val graph = WorkspaceTargetGraphBuilder.build(rootTargets = roots.map { it.targetKey }.toSet(), targets = targets)
    return WorkspaceSnapshot(
      targets = InMemoryWorkspaceTargetMap(targets.associateBy { it.targetKey }),
      configurations = mapOf(),
      targetGraph = graph,
      fileToTarget = File2TargetMapBuilder.build(targets = targets),
      executableTargets = ExecutableTargetsIndexBuilder.build(targetGraph = graph, importDepth = importDepth, targets = targets),
      syncConfigs = listOf(CommonWorkspaceSyncConfig(Path.of(project.basePath!!), "test", importDepth)),
      repoMapping = RepoMappingDisabled,
      metadata = WorkspaceSnapshotMetadata(version = 1),
    )
  }

  @Test
  fun `empty snapshot answers empty everywhere`(): Unit = runBlocking {
    publish(project, snapshot(project, targets = emptyList()))

    val targetUtils = project.targetStorage
    targetUtils.getTotalTargetCount() shouldBe 0
    targetUtils.allTargetSummaries() shouldBe emptyList()
    targetUtils.getTargetSummary(Label.parse("//nothing:nothing")) shouldBe null
    targetUtils.getTargetsForPath(Path.of("/workspace/Nothing.java")) shouldBe emptyList()
  }

  @Test
  fun `summaries expose the depth-filtered import set only`(): Unit = runBlocking {
    val bin = target("//app:bin", deps = listOf("//lib:lib"), executable = true, isManual = true)
    val lib = target("//lib:lib", deps = listOf("//deep:deep"))
    val deep = target("//deep:deep")
    publish(project, snapshot(project, targets = listOf(bin, lib, deep), roots = listOf(bin), importDepth = 1))

    val summaries = project.targetStorage.allTargetSummaries().associateBy { it.label }
    summaries.keys shouldBe setOf(bin.targetKey.label, lib.targetKey.label)

    val binSummary = summaries.getValue(bin.targetKey.label)
    binSummary.kind shouldBe bin.rawBuildTarget.kind
    binSummary.baseDirectory shouldBe bin.rawBuildTarget.baseDirectory
    binSummary.isManual shouldBe bin.rawBuildTarget.isManual
    binSummary.isWorkspace shouldBe bin.rawBuildTarget.isWorkspace
  }

  @Test
  fun `getTargetDataForLabel loads language data before and after persist`(): Unit = runBlocking {
    val jvmData = JvmBuildTarget(javacOpts = listOf("-parameters"), mainClass = "com.example.Main")
    val lib = target("//lib:lib", deps = listOf("//other:other"), data = listOf(jvmData))
    val other = target("//other:other")
    publish(project, snapshot(project, targets = listOf(lib, other), roots = listOf(lib, other)))

    val targetUtils = project.targetStorage

    // in-memory (clean resync) snapshot holds full targets, language data is present
    targetUtils.getTargetDataForLabel(lib.targetKey.label, JvmBuildTarget::class) shouldBe jvmData

    project.service<WorkspaceSnapshotService>().save()

    // store-backed snapshot, the requested data frame is still loaded exactly, with no sentinel leaking
    targetUtils.getTargetDataForLabel(lib.targetKey.label, JvmBuildTarget::class) shouldBe jvmData
  }

  @Test
  fun `getTargetDataForLabel loads a single data type`(): Unit = runBlocking {
    val jvmData = JvmBuildTarget(javacOpts = listOf("-parameters"), mainClass = "com.example.Main")
    val toolchainData = JavaToolchainData(sourceVersion = "17", targetVersion = "17")
    val lib = target("//lib:lib", data = listOf(jvmData, toolchainData))
    publish(project, snapshot(project, targets = listOf(lib)))

    val targetUtils = project.targetStorage
    targetUtils.getTargetDataForLabel(lib.targetKey.label, JvmBuildTarget::class) shouldBe jvmData
    targetUtils.getTargetDataForLabel(lib.targetKey.label, PythonBuildTarget::class) shouldBe null
  }

  @Test
  fun `label collisions prefer the empty-configuration key`(): Unit = runBlocking {
    val label = Label.parse("//lib:lib")
    val configuredKey = WorkspaceTargetKey(label = label, configuration = WorkspaceConfigurationId.of("abcdef1"))
    val plainKey = WorkspaceTargetKey(label = label)
    val configured = target("//lib:lib", key = configuredKey, baseDirectory = Path.of("/workspace/configured"))
    val plain = target("//lib:lib", key = plainKey, baseDirectory = Path.of("/workspace/plain"))
    publish(project, snapshot(project, targets = listOf(configured, plain), roots = listOf(configured, plain)))

    val summary = project.targetStorage.getTargetSummary(label)
    summary?.baseDirectory shouldBe Path.of("/workspace/plain")
  }

  @Test
  fun `addFileToTargetIdEntry and removeFileToTargetIdEntry are visible through getTargetsForPath`(): Unit = runBlocking {
    val bin = target("//app:bin", deps = listOf("//lib:lib"), executable = true)
    val lib = target("//lib:lib", deps = listOf("//deep:deep"))
    val deep = target("//deep:deep")
    publish(project, snapshot(project, targets = listOf(bin, lib, deep), roots = listOf(bin), importDepth = 1))
    val file = Path.of("/workspace/src/Extra.java")

    val targetUtils = project.targetStorage
    targetUtils.addFileToTargetIdEntry(file, listOf(bin.targetKey.label, deep.targetKey.label))
    targetUtils.getTargetsForPath(file) shouldBe listOf(bin.targetKey.label)

    targetUtils.removeFileToTargetIdEntry(file)
    targetUtils.getTargetsForPath(file) shouldBe emptyList()
  }

  @Test
  fun `getExecutableTargetsForTarget answers from the snapshot index`(): Unit = runBlocking {
    val bin = target("//app:bin", deps = listOf("//lib:lib"), executable = true)
    val lib = target("//lib:lib")
    publish(project, snapshot(project, targets = listOf(bin, lib), roots = listOf(bin)))

    project.targetStorage.getExecutableTargetsForTarget(lib.targetKey.label).shouldContainExactly(bin.targetKey.label)
  }

  @Test
  fun `answers are identical after the snapshot is persisted`(): Unit = runBlocking {
    val jvmData = JvmBuildTarget(javacOpts = listOf("-parameters"), mainClass = "com.example.Main")
    val file = Path.of("/workspace/app/Bin.java")
    val bin = target(
      "//app:bin",
      deps = listOf("//lib:lib"),
      executable = true,
      sources = SourceFileCollectionBuilder.build(relativeRoot = Path.of("/workspace"), paths = listOf(file)),
    )
    val lib = target("//lib:lib", data = listOf(jvmData))
    publish(project, snapshot(project, targets = listOf(bin, lib), roots = listOf(bin)))

    val targetUtils = project.targetStorage

    val summariesBefore = targetUtils.allTargetSummaries()
    val targetsForPathBefore = targetUtils.getTargetsForPath(file)
    val executableBefore = targetUtils.getExecutableTargetsForTarget(lib.targetKey.label)
    val dataBefore = targetUtils.getTargetDataForLabel(lib.targetKey.label, JvmBuildTarget::class)

    project.service<WorkspaceSnapshotService>().save()

    targetUtils.allTargetSummaries() shouldBe summariesBefore
    targetUtils.getTargetsForPath(file) shouldBe targetsForPathBefore
    targetUtils.getExecutableTargetsForTarget(lib.targetKey.label) shouldBe executableBefore
    targetUtils.getTargetDataForLabel(lib.targetKey.label, JvmBuildTarget::class) shouldBe dataBefore
  }

  @Test
  fun `allTargetSummaries returns the same cached List instance for the same snapshot`(): Unit = runBlocking {
    val bin = target("//app:bin", executable = true)
    publish(project, snapshot(project, targets = listOf(bin)))

    val targetUtils = project.targetStorage
    val first = targetUtils.allTargetSummaries()
    val second = targetUtils.allTargetSummaries()
    second shouldBeSameInstanceAs first

    targetUtils.setTargets(listOf(bin.rawBuildTarget))

    val afterNewSnapshot = targetUtils.allTargetSummaries()
    afterNewSnapshot shouldNotBeSameInstanceAs first
  }
}
