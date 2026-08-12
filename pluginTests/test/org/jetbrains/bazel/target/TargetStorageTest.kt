package org.jetbrains.bazel.target

import com.intellij.configurationStore.SettingsSavingComponent
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.testFramework.junit5.TestApplication
import com.intellij.testFramework.junit5.fixture.projectFixture
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldNotBeNull
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
import org.jetbrains.bazel.sync.workspace.persistence.TargetLoadOptions
import org.jetbrains.bazel.sync.workspace.persistence.TargetSection
import org.jetbrains.bazel.sync.workspace.persistence.WorkspaceSnapshotService
import org.jetbrains.bazel.sync.workspace.snapshot.CommonWorkspaceSyncConfig
import org.jetbrains.bazel.sync.workspace.snapshot.ExecutableTargetsIndexBuilder
import org.jetbrains.bazel.sync.workspace.snapshot.File2TargetMapBuilder
import org.jetbrains.bazel.sync.workspace.snapshot.SourceFileCollectionBuilder
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceConfigurationId
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSnapshot
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSnapshotMetadata
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetGraphBuilder
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey
import org.jetbrains.bazel.test.framework.target.TestBuildTarget
import org.jetbrains.bazel.ui.gutters.NonImportedBuildTarget
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.BuildTargetData
import org.jetbrains.bsp.protocol.BuildTargetTag
import org.jetbrains.bsp.protocol.SourceFileCollection
import org.jetbrains.bsp.protocol.data
import org.jetbrains.bsp.protocol.id
import org.junit.jupiter.api.Test
import java.nio.file.Path
import kotlin.io.path.Path

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
    isWorkspace: Boolean = true,
    baseDirectory: Path = Path.of("/workspace"),
    sources: SourceFileCollection = SourceFileCollection.EMPTY,
    key: WorkspaceTargetKey = WorkspaceTargetKey(label = Label.parse(label)),
    tags: List<String> = emptyList(),
  ): TestBuildTarget =
    TestBuildTarget(
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
      isWorkspace = isWorkspace,
      tags = tags,
    )

  private fun BuildTarget.summaryView(): List<Any?> = listOf(key, kind, baseDirectory, tags, isWorkspace)

  private fun snapshot(
    project: Project,
    targets: List<BuildTarget>,
    roots: List<BuildTarget> = targets,
    importDepth: Int = -1,
  ): WorkspaceSnapshot {
    val graph = WorkspaceTargetGraphBuilder.build(rootTargets = roots.map { it.key }.toSet(), targets = targets)
    return WorkspaceSnapshot(
      targets = InMemoryWorkspaceTargetMap(targets.associateBy { it.key }),
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
    val bin = target("//app:bin", deps = listOf("//lib:lib"), executable = true, tags = listOf(BuildTargetTag.MANUAL))
    val lib = target("//lib:lib", deps = listOf("//deep:deep"))
    val deep = target("//deep:deep")
    publish(project, snapshot(project, targets = listOf(bin, lib, deep), roots = listOf(bin), importDepth = 1))

    val summaries = project.targetStorage.allTargetSummaries().associateBy { it.id }
    summaries.keys shouldBe setOf(bin.key.label, lib.key.label)

    // an in-memory snapshot hands back the very targets it was given, so compare them whole
    summaries.getValue(bin.key.label) shouldBe bin
  }

  @Test
  fun `getTargetDataForLabel loads language data before and after persist`(): Unit = runBlocking {
    val jvmData = JvmBuildTarget(javacOpts = listOf("-parameters"), mainClass = "com.example.Main")
    val lib = target("//lib:lib", deps = listOf("//other:other"), data = listOf(jvmData))
    val other = target("//other:other")
    publish(project, snapshot(project, targets = listOf(lib, other), roots = listOf(lib, other)))

    val targetUtils = project.targetStorage

    // in-memory (clean resync) snapshot holds full targets, language data is present
    targetUtils.getTargetDataForLabel(lib.key.label, JvmBuildTarget::class.java) shouldBe jvmData

    project.service<WorkspaceSnapshotService>().save()

    // store-backed snapshot, the requested data frame is still loaded exactly, with no sentinel leaking
    targetUtils.getTargetDataForLabel(lib.key.label, JvmBuildTarget::class.java) shouldBe jvmData
  }

  @Test
  fun `getTargetDataForLabel loads a single data type`(): Unit = runBlocking {
    val jvmData = JvmBuildTarget(javacOpts = listOf("-parameters"), mainClass = "com.example.Main")
    val toolchainData = JavaToolchainData(sourceVersion = "17", targetVersion = "17")
    val lib = target("//lib:lib", data = listOf(jvmData, toolchainData))
    publish(project, snapshot(project, targets = listOf(lib)))

    val targetUtils = project.targetStorage
    targetUtils.getTargetDataForLabel(lib.key.label, JvmBuildTarget::class.java) shouldBe jvmData
    targetUtils.getTargetDataForLabel(lib.key.label, PythonBuildTarget::class.java) shouldBe null
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
    targetUtils.addFileToTargetIdEntry(file, listOf(bin.key.label, deep.key.label))
    targetUtils.getTargetsForPath(file) shouldBe listOf(bin.key.label)

    targetUtils.removeFileToTargetIdEntry(file)
    targetUtils.getTargetsForPath(file) shouldBe emptyList()
  }

  @Test
  fun `getExecutableTargetsForTarget answers from the snapshot index`(): Unit = runBlocking {
    val bin = target("//app:bin", deps = listOf("//lib:lib"), executable = true)
    val lib = target("//lib:lib")
    publish(project, snapshot(project, targets = listOf(bin, lib), roots = listOf(bin)))

    project.targetStorage.getExecutableTargetsForTarget(lib.key.label).shouldContainExactly(bin.key.label)
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

    val summariesBefore = targetUtils.allTargetSummaries().map { it.summaryView() }
    val targetsForPathBefore = targetUtils.getTargetsForPath(file)
    val executableBefore = targetUtils.getExecutableTargetsForTarget(lib.key.label)
    val dataBefore = targetUtils.getTargetDataForLabel(lib.key.label, JvmBuildTarget::class.java)

    project.service<WorkspaceSnapshotService>().save()

    targetUtils.allTargetSummaries().map { it.summaryView() } shouldBe summariesBefore
    targetUtils.getTargetsForPath(file) shouldBe targetsForPathBefore
    targetUtils.getExecutableTargetsForTarget(lib.key.label) shouldBe executableBefore
    targetUtils.getTargetDataForLabel(lib.key.label, JvmBuildTarget::class.java) shouldBe dataBefore
  }

  @Test
  fun `a persisted summary carries INFO and materializes the sections it did not load`(): Unit = runBlocking {
    val jvmData = JvmBuildTarget(javacOpts = listOf("-parameters"), mainClass = "com.example.Main")
    val bin = target("//app:bin", deps = listOf("//lib:lib"), executable = true, data = listOf(jvmData))
    val lib = target("//lib:lib")
    publish(project, snapshot(project, targets = listOf(bin, lib), roots = listOf(bin, lib)))
    project.service<WorkspaceSnapshotService>().save()

    val summary = project.targetStorage.getTargetSummary(bin.key.label)
    summary.shouldNotBeNull()
    summary.loaded.sections shouldBe setOf(TargetSection.INFO)

    summary.kind shouldBe bin.kind
    summary.baseDirectory shouldBe bin.baseDirectory
    summary.tags shouldBe bin.tags

    summary.sources.getFiles().toList() shouldBe bin.sources.getFiles().toList()
    summary.dependencies shouldBe bin.dependencies
    summary.data(JvmBuildTarget::class.java) shouldBe jvmData

    project.targetStorage.getTargetDataForLabel(bin.key.label, JvmBuildTarget::class.java) shouldBe jvmData
  }

  @Test
  fun `data selects from what a target already carries`() {
    val jvmData = JvmBuildTarget(javacOpts = listOf("-parameters"), mainClass = "com.example.Main")
    val toolchain = JavaToolchainData(sourceVersion = "17", targetVersion = "17")
    val full = target("//lib:lib", data = listOf(jvmData, toolchain))

    full.data(JvmBuildTarget::class.java) shouldBe jvmData
    full.data(JavaToolchainData::class.java) shouldBe toolchain
    full.data(PythonBuildTarget::class.java) shouldBe null
  }

  @Test
  fun `a minimally loaded target names itself and its sections when a missing one is read`() {
    val label = Label.parse("//app:bin")
    val guessed = NonImportedBuildTarget(
      label = label,
      kind = TargetKind(kind = "java_binary", ruleType = RuleType.BINARY, languageClasses = setOf(JavaLanguageClass.JAVA)),
      baseDirectory = Path("/tmp/workspace")
    )

    guessed.id shouldBe label
    guessed.loaded shouldBe TargetLoadOptions.MINIMAL

    guessed.data shouldBe emptyList()
  }

  @Test
  fun `allTargetSummaries returns the same cached List instance for the same snapshot`(): Unit = runBlocking {
    val bin = target("//app:bin", executable = true)
    publish(project, snapshot(project, targets = listOf(bin)))

    val targetUtils = project.targetStorage
    val first = targetUtils.allTargetSummaries()
    val second = targetUtils.allTargetSummaries()
    second shouldBeSameInstanceAs first

    targetUtils.setTargets(listOf(bin))

    val afterNewSnapshot = targetUtils.allTargetSummaries()
    afterNewSnapshot shouldNotBeSameInstanceAs first
  }
}
