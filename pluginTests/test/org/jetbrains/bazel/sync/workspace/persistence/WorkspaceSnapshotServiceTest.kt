package org.jetbrains.bazel.sync.workspace.persistence

import com.intellij.configurationStore.SettingsSavingComponent
import com.intellij.ide.impl.OpenProjectTask
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ex.ProjectManagerEx
import com.intellij.openapi.project.getProjectDataPath
import com.intellij.testFramework.junit5.TestApplication
import com.intellij.testFramework.junit5.fixture.tempPathFixture
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.jetbrains.bazel.commons.BzlmodRepoMapping
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.commons.RepoMappingDisabled
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.label.DependencyLabel
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync.JavaLanguageClass
import org.jetbrains.bazel.sync.workspace.languages.jvm.JavaToolchainData
import org.jetbrains.bazel.sync.workspace.languages.jvm.JvmBuildTarget
import org.jetbrains.bazel.sync.workspace.snapshot.CommonWorkspaceSyncConfig
import org.jetbrains.bazel.sync.workspace.snapshot.ExecutableTargetsIndex
import org.jetbrains.bazel.sync.workspace.snapshot.FileToTargetMap
import org.jetbrains.bazel.sync.workspace.snapshot.SourceFileCollectionBuilder
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceConfiguration
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceConfigurationId
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceConfigurationSummary
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSnapshot
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSnapshotMetadata
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetGraphBuilder
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey
import org.jetbrains.bazel.test.framework.target.TestBuildTarget
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.SourceFileCollection
import org.junit.jupiter.api.Test
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.time.Duration.Companion.seconds

@TestApplication
class WorkspaceSnapshotServiceTest {

  private val tempDir by tempPathFixture()

  private suspend fun <T> withProject(
    isNew: Boolean = false,
    saveOnClose: Boolean = false,
    action: suspend (Project) -> T,
  ): T {
    val projectDir = tempDir.resolve("project").createDirectories()
    val options = OpenProjectTask {
      isNewProject = isNew
      runConfigurators = false
      useDefaultProjectAsTemplate = false
    }
    val project = ProjectManagerEx.getInstanceEx().openProjectAsync(projectDir, options)
                  ?: error("failed to open project")
    try {
      return action(project)
    }
    finally {
      ProjectManagerEx.getInstanceEx().forceCloseProjectAsync(project, save = saveOnClose)
    }
  }

  private suspend fun awaitLoadedSnapshot(project: Project): WorkspaceSnapshot {
    val service = project.getService(WorkspaceSnapshotService::class.java)
    return withTimeout(60.seconds) { service.snapshot.first { it !== WorkspaceSnapshot.EMPTY } }
  }

  private suspend fun WorkspaceSnapshotService.save() = (this as SettingsSavingComponent).save()

  private fun key(label: String): WorkspaceTargetKey = WorkspaceTargetKey(label = Label.parse(label))

  private fun sources(vararg paths: Path): SourceFileCollection =
    SourceFileCollectionBuilder.build(relativeRoot = Path.of("/workspace"), paths = paths.toList())

  private fun rawTarget(key: WorkspaceTargetKey, index: Int): TestBuildTarget =
    TestBuildTarget(
      key = key,
      dependencies = listOf(DependencyLabel(targetKey = key("@//deps:dep$index"))),
      kind = TargetKind(
        kind = "java_library",
        languageClasses = setOf(LanguageClass("java", setOf("java"))),
        ruleType = RuleType.LIBRARY,
      ),
      sources = sources(Path.of("/workspace/pkg$index/Main.java"), Path.of("/workspace/shared/Shared.java")),
      generatedSources = SourceFileCollection.EMPTY,
      resources = SourceFileCollection.EMPTY,
      baseDirectory = Path.of("/workspace/pkg$index"),
      data = listOf(
        JvmBuildTarget(javacOpts = listOf("-parameters"), mainClass = "com.example.Main$index"),
        JavaToolchainData(sourceVersion = "17", targetVersion = "17"),
      ),
      generatorName = null,
      isWorkspace = true,
      isTestOnly = false,
    )

  private fun buildSnapshot(): WorkspaceSnapshot {
    val targets = (0 until 3).map { n ->
      val targetKey = key("@//pkg$n:target")
      rawTarget(targetKey, n)
    }
    val configurationId = WorkspaceConfigurationId.of("cafe4242")
    return WorkspaceSnapshot(
      targets = InMemoryWorkspaceTargetMap(targets.associateBy { it.key }),
      workspaceName = "_main",
      configurations = mapOf(
        configurationId to WorkspaceConfiguration(
          id = configurationId,
          summary = WorkspaceConfigurationSummary(
            hash = "123456789",
            mnemonic = "darwin_arm64-fastbuild",
            platformName = "darwin",
            cpu = "arm64",
            isTool = false,
          ),
          fragments = listOf(),
        ),
      ),
      targetGraph = WorkspaceTargetGraphBuilder.build(
        rootTargets = setOf(targets.first().key),
        targets = targets,
      ),
      fileToTarget = FileToTargetMap.EMPTY,
      executableTargets = ExecutableTargetsIndex.EMPTY,
      syncConfigs = listOf(CommonWorkspaceSyncConfig(projectRootDir = Path.of("/workspace"), projectName = "e2e", importDepth = -1)),
      repoMapping = BzlmodRepoMapping(
        canonicalRepoNameToLocalPath = mapOf("rules_jvm~" to Path.of("/cache/rules_jvm")),
        apparentRepoNameToCanonicalName = mapOf("rules_jvm" to "rules_jvm~"),
        canonicalRepoNameToPath = mapOf("rules_jvm~" to Path.of("/external/rules_jvm")),
        nonLocalCanonicalRepoNames = setOf(),
      ),
      metadata = WorkspaceSnapshotMetadata(version = 1),
    )
  }

  private fun assertSnapshotEquals(actual: WorkspaceSnapshot, expected: WorkspaceSnapshot) {
    actual.metadata.version shouldBeGreaterThan WorkspaceSnapshot.EMPTY.metadata.version
    actual.workspaceName shouldBe expected.workspaceName
    actual.configurations shouldBe expected.configurations
    actual.syncConfigs shouldBe expected.syncConfigs
    actual.repoMapping shouldBe expected.repoMapping

    val expectedTargets = expected.targets.allTargets().toList()
    actual.targetGraph.allTargets.map { it.targetKey }.toSet() shouldBe expectedTargets.map { it.key }.toSet()
    for (expectedRaw in expectedTargets) {
      val actualRaw = actual.targets.findTargetByKey(expectedRaw.key)
      actualRaw.shouldNotBeNull()
      actualRaw.kind shouldBe expectedRaw.kind
      actualRaw.baseDirectory shouldBe expectedRaw.baseDirectory
      actualRaw.dependencies shouldBe expectedRaw.dependencies
      actualRaw.sources shouldBe expectedRaw.sources
      actualRaw.data.toSet() shouldBe expectedRaw.data.toSet()
    }

    actual.fileToTarget.getTargetsByFile(Path.of("/workspace/pkg1/Main.java")) shouldBe listOf(key("@//pkg1:target"))
    actual.fileToTarget.getTargetsByFile(Path.of("/workspace/shared/Shared.java")).toSet() shouldBe
      expectedTargets.map { it.key }.toSet()
  }

  @Test
  fun `snapshot survives project restarts and re-persist cycles`(): Unit = runBlocking {
    val expected = buildSnapshot()

    withProject(isNew = true, saveOnClose = true) { project ->
      project.getService(WorkspaceSnapshotService::class.java).update { expected to Unit }
    }

    val firstReloadVersion = withProject(saveOnClose = true) { project ->
      val loaded = awaitLoadedSnapshot(project)
      assertSnapshotEquals(loaded, expected)
      project.getService(WorkspaceSnapshotService::class.java).update { it.copy(targets = expected.targets) to Unit }
      loaded.metadata.version
    }

    withProject { project ->
      val restored = awaitLoadedSnapshot(project)
      assertSnapshotEquals(restored, expected)
      restored.metadata.version shouldBeGreaterThan firstReloadVersion
    }
  }

  @Test
  fun `stored snapshot is swapped to its persistent-backed twin`(): Unit = runBlocking {
    val expected = buildSnapshot()

    withProject(isNew = true) { project ->
      val service = project.getService(WorkspaceSnapshotService::class.java)
      service.update { expected to Unit }
      val swapped = withTimeout(60.seconds) { service.snapshot.first { it.targets !is InMemoryWorkspaceTargetMap } }
      assertSnapshotEquals(swapped, expected)
    }
  }

  @Test
  fun `currentSnapshot awaits the initial load without any flow collector`(): Unit = runBlocking {
    val expected = buildSnapshot()

    withProject(isNew = true, saveOnClose = true) { project ->
      val service = project.getService(WorkspaceSnapshotService::class.java)
      withTimeout(60.seconds) { service.currentSnapshot() } shouldBe WorkspaceSnapshot.EMPTY
      service.update { expected to Unit }
    }

    withProject { project ->
      val service = project.getService(WorkspaceSnapshotService::class.java)
      val current = withTimeout(60.seconds) { service.currentSnapshot() }
      assertSnapshotEquals(current, expected)
    }
  }

  @Test
  fun `an update that changes, removes, and adds targets survives restart`(): Unit = runBlocking {
    val expected = buildSnapshot()

    withProject(isNew = true, saveOnClose = true) { project ->
      project.getService(WorkspaceSnapshotService::class.java).update { expected to Unit }
    }

    val changedKey = key("@//pkg1:target")
    val changed = rawTarget(changedKey, 1).copy(isTestOnly = true)
    val removedKey = key("@//pkg2:target")
    val addedKey = key("@//pkg3:target")
    val added = rawTarget(addedKey, 3)
    val pkg0 = expected.targets.findTargetByKey(key("@//pkg0:target"))
    pkg0.shouldNotBeNull()

    withProject(saveOnClose = true) { project ->
      val service = project.getService(WorkspaceSnapshotService::class.java)
      awaitLoadedSnapshot(project)
      service.update { snapshot ->
        snapshot.copy(
          targets = InMemoryWorkspaceTargetMap(
            mapOf(pkg0.key to pkg0, changedKey to changed, addedKey to added),
          ),
        ) to Unit
      }
    }

    withProject { project ->
      val restored = awaitLoadedSnapshot(project)

      val changedTarget = restored.targets.findTargetByKey(changedKey)
      changedTarget.shouldNotBeNull()
      changedTarget.isTestOnly shouldBe true

      restored.targets.findTargetByKey(removedKey) shouldBe null

      val addedTarget = restored.targets.findTargetByKey(addedKey)
      addedTarget.shouldNotBeNull()
      addedTarget.baseDirectory shouldBe Path.of("/workspace/pkg3")

      val target = restored.targets.findTargetByKey(key("@//pkg0:target"))
      target.shouldNotBeNull()
      target.sources shouldBe sources(Path.of("/workspace/pkg0/Main.java"), Path.of("/workspace/shared/Shared.java"))

      restored.fileToTarget.getTargetsByFile(Path.of("/workspace/pkg2/Main.java")) shouldBe emptyList()
      restored.fileToTarget.getTargetsByFile(Path.of("/workspace/pkg3/Main.java")) shouldBe listOf(addedKey)
      restored.fileToTarget.getTargetsByFile(Path.of("/workspace/shared/Shared.java")).toSet() shouldBe
        setOf(key("@//pkg0:target"), changedKey, addedKey)
    }
  }

  @Test
  fun `re-store drops file rows for removed sources`(): Unit = runBlocking {
    val changedKey = key("@//pkg1:target")
    val generatedFile = Path.of("/workspace/gen/pkg1/Gen.java")

    // pkg1 carries a generated source G in addition to its normal sources
    val base = buildSnapshot()
    base.targets.findTargetByKey(changedKey).shouldNotBeNull()
    // build the variant from the same recipe buildSnapshot() used, rather than reading one back to copy it
    val withGeneratedSource = rawTarget(changedKey, 1).copy(generatedSources = sources(generatedFile))
    val expected = base.copy(
      targets = InMemoryWorkspaceTargetMap(base.targets.allTargets().associateBy { it.key } + (changedKey to withGeneratedSource)),
    )

    withProject(isNew = true, saveOnClose = true) { project ->
      project.getService(WorkspaceSnapshotService::class.java).update { expected to Unit }
    }

    val changed = withGeneratedSource.copy(generatedSources = SourceFileCollection.EMPTY)
    val reStoredTargets = expected.targets.allTargets().associateBy { it.key } + (changedKey to changed)
    withProject(saveOnClose = true) { project ->
      val service = project.getService(WorkspaceSnapshotService::class.java)
      awaitLoadedSnapshot(project)
      service.update { snapshot ->
        snapshot.copy(targets = InMemoryWorkspaceTargetMap(reStoredTargets)) to Unit
      }
    }

    withProject { project ->
      val restored = awaitLoadedSnapshot(project)
      restored.fileToTarget.getTargetsByFile(generatedFile) shouldBe emptyList()
      restored.fileToTarget.getTargetsByFile(Path.of("/workspace/pkg1/Main.java")) shouldBe listOf(changedKey)
      checkNotNull(restored.targets.findTargetByKey(changedKey))
    }
  }

  @Test
  fun `save before anything is stored does not create the database`(): Unit = runBlocking {
    withProject(isNew = true) { project ->
      val service = project.getService(WorkspaceSnapshotService::class.java)
      service.save()
      project.getProjectDataPath("snapshot_db.data").exists() shouldBe false
    }
  }

  @Test
  fun `pending snapshot is flushed when the project closes with save`(): Unit = runBlocking {
    val expected = buildSnapshot()

    withProject(isNew = true, saveOnClose = true) { project ->
      project.getService(WorkspaceSnapshotService::class.java).update { expected to Unit }
    }

    withProject { project ->
      assertSnapshotEquals(awaitLoadedSnapshot(project), expected)
    }
  }

  @Test
  fun `storing a snapshot backed by an older generation keeps all targets`(): Unit = runBlocking {
    val expected = buildSnapshot()

    withProject(isNew = true, saveOnClose = true) { project ->
      project.getService(WorkspaceSnapshotService::class.java).update { expected to Unit }
    }

    withProject(saveOnClose = true) { project ->
      val service = project.getService(WorkspaceSnapshotService::class.java)
      val loaded = awaitLoadedSnapshot(project)

      val (snapshot, _) = service.update { snapshot ->
        val id = WorkspaceConfigurationId.of("abcdefg")
        val config = WorkspaceConfiguration(
          id = id,
          summary = WorkspaceConfigurationSummary(
            hash = "abcdefg123456789",
            mnemonic = "darwin-arm64",
            platformName = "macos",
            cpu = "arm64",
            isTool = false,
          ),
          fragments = listOf(),
        )
        snapshot.copy(configurations = snapshot.configurations + mapOf(id to config)) to Unit
      }
      withTimeout(60.seconds) { service.snapshot.first { it !== snapshot && it !== loaded } }

      service.update { loaded.copy(configurations = emptyMap()) to Unit }
    }

    withProject { project ->
      val restored = awaitLoadedSnapshot(project)
      for (n in 0 until 3) {
        restored.targets.findTargetByKey(key("@//pkg$n:target")).shouldNotBeNull()
      }
    }
  }

  private fun executableTarget(
    key: WorkspaceTargetKey,
    deps: List<WorkspaceTargetKey> = emptyList(),
    executable: Boolean = false,
  ): TestBuildTarget =
    TestBuildTarget(
      key = key,
      dependencies = deps.map { DependencyLabel(targetKey = it) },
      kind = TargetKind(
        kind = if (executable) "java_binary" else "java_library",
        languageClasses = setOf(JavaLanguageClass.JAVA),
        ruleType = if (executable) RuleType.BINARY else RuleType.LIBRARY,
      ),
      sources = SourceFileCollection.EMPTY,
      generatedSources = SourceFileCollection.EMPTY,
      resources = SourceFileCollection.EMPTY,
      baseDirectory = Path.of("/workspace"),
    )

  private fun binDependsOnLibSnapshot(): WorkspaceSnapshot {
    val libKey = key("@//lib:lib")
    val binKey = key("@//app:bin")
    val lib = executableTarget(libKey)
    val bin = executableTarget(binKey, deps = listOf(libKey), executable = true)
    return WorkspaceSnapshot(
      targets = InMemoryWorkspaceTargetMap(listOf(bin, lib).associateBy { it.key }),
      workspaceName = null,
      configurations = mapOf(),
      targetGraph = WorkspaceTargetGraphBuilder.build(rootTargets = setOf(binKey), targets = listOf(bin, lib)),
      fileToTarget = FileToTargetMap.EMPTY,
      executableTargets = ExecutableTargetsIndex.EMPTY,
      syncConfigs = listOf(CommonWorkspaceSyncConfig(projectRootDir = Path.of("/workspace"), projectName = "e2e", importDepth = -1)),
      repoMapping = RepoMappingDisabled,
      metadata = WorkspaceSnapshotMetadata(version = 1),
    )
  }

  @Test
  fun `executable targets index is recomputed on store and readable after reload`(): Unit = runBlocking {
    val libLabel = key("@//lib:lib").label
    val binLabel = key("@//app:bin").label
    val expected = binDependsOnLibSnapshot()

    withProject(isNew = true, saveOnClose = true) { project ->
      project.getService(WorkspaceSnapshotService::class.java).update { expected to Unit }
    }

    withProject(saveOnClose = true) { project ->
      val restored = awaitLoadedSnapshot(project)
      restored.executableTargets.executableTargetsFor(libLabel).shouldContainExactly(binLabel)

      project.getService(WorkspaceSnapshotService::class.java).update { restored.copy() to Unit }
    }

    withProject { project ->
      val restored = awaitLoadedSnapshot(project)
      restored.executableTargets.executableTargetsFor(libLabel).shouldContainExactly(binLabel)
    }
  }

  @Test
  fun `executable index survives restart when one label is imported under several configurations`(): Unit = runBlocking {
    val libKey = key("@//lib:lib")
    val binKey = key("@//app:bin")
    val altConfiguration = WorkspaceConfigurationId.of("deadbeef")
    val libAltKey = WorkspaceTargetKey(label = libKey.label, configuration = altConfiguration)
    val binAltKey = WorkspaceTargetKey(label = binKey.label, configuration = altConfiguration)
    val targets = listOf(
      executableTarget(libKey),
      executableTarget(binKey, deps = listOf(libKey), executable = true),
      executableTarget(libAltKey),
      executableTarget(binAltKey, deps = listOf(libAltKey), executable = true),
    )
    val expected = WorkspaceSnapshot(
      targets = InMemoryWorkspaceTargetMap(targets.associateBy { it.key }),
      workspaceName = null,
      configurations = mapOf(),
      targetGraph = WorkspaceTargetGraphBuilder.build(rootTargets = setOf(binKey, binAltKey), targets = targets),
      fileToTarget = FileToTargetMap.EMPTY,
      executableTargets = ExecutableTargetsIndex.EMPTY,
      syncConfigs = listOf(CommonWorkspaceSyncConfig(projectRootDir = Path.of("/workspace"), projectName = "e2e", importDepth = -1)),
      repoMapping = RepoMappingDisabled,
      metadata = WorkspaceSnapshotMetadata(version = 1),
    )

    withProject(isNew = true, saveOnClose = true) { project ->
      project.getService(WorkspaceSnapshotService::class.java).update { expected to Unit }
    }

    withProject { project ->
      val restored = awaitLoadedSnapshot(project)
      restored.executableTargets.executableTargetsFor(libKey.label).shouldContainExactly(binKey.label)
    }
  }

  @Test
  fun `file mapping point updates are instantly visible and survive save and reopen`(): Unit = runBlocking {
    val binKey = key("@//app:bin")
    val expected = binDependsOnLibSnapshot()
    val file = Path.of("/workspace/src/Extra.kt")

    withProject(isNew = true, saveOnClose = true) { project ->
      val service = project.getService(WorkspaceSnapshotService::class.java)
      service.update { expected to Unit }
      service.save()

      val persisted = service.currentSnapshot()
      persisted.fileToTarget.getTargetsByFile(file) shouldBe emptyList()

      persisted.fileToTarget.addMapping(file, listOf(binKey))
      persisted.fileToTarget.getTargetsByFile(file).shouldContainExactly(binKey)

      service.save()
    }

    withProject { project ->
      val restored = awaitLoadedSnapshot(project)
      restored.fileToTarget.getTargetsByFile(file).shouldContainExactly(binKey)

      restored.fileToTarget.removeMapping(file)
      restored.fileToTarget.getTargetsByFile(file) shouldBe emptyList()
    }
  }
}
