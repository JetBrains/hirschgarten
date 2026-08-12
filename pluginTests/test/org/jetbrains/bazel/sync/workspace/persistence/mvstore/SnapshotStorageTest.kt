package org.jetbrains.bazel.sync.workspace.persistence.mvstore

import com.intellij.testFramework.junit5.TestApplication
import com.intellij.testFramework.junit5.fixture.projectFixture
import com.intellij.testFramework.junit5.fixture.tempPathFixture
import com.intellij.util.ref.GCWatcher
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import it.unimi.dsi.fastutil.ints.IntArrayList
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
import kotlinx.coroutines.runBlocking
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.commons.RepoMappingDisabled
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.label.DependencyLabel
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync.workspace.languages.jvm.JavaToolchainData
import org.jetbrains.bazel.sync.workspace.languages.jvm.JvmBuildTarget
import org.jetbrains.bazel.sync.workspace.persistence.BuildTargetLoadHint
import org.jetbrains.bazel.sync.workspace.persistence.TargetLoadOptions
import org.jetbrains.bazel.sync.workspace.persistence.TargetSection
import org.jetbrains.bazel.sync.workspace.persistence.WorkspaceTypeContributor
import org.jetbrains.bazel.sync.workspace.snapshot.SourceFileCollectionBuilder
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSnapshotMetadata
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetGraph
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey
import org.jetbrains.bazel.test.framework.target.TestBuildTarget
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.SourceFileCollection
import org.jetbrains.bsp.protocol.isFull
import org.junit.jupiter.api.Test
import java.nio.file.Path

@TestApplication
class SnapshotStorageTest {

  private val tempDir by tempPathFixture()
  private val project by projectFixture()

  private fun buildKryo(): SnapshotKryo {
    val entries = WorkspaceTypeContributor.EP_NAME.extensionList
      .flatMap { contributor -> contributor.contribute(project) }
    return SnapshotKryo(SnapshotTypeUniverse.build(entries))
  }

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

  private class SavedState(
    val targets: Map<WorkspaceTargetKey, BuildTarget>,
    val partial: PersistentWorkspaceSnapshot,
  )

  private fun saveAll(generation: SnapshotGeneration, count: Int): SavedState {
    val keyId2Target = Int2ObjectBiMap<WorkspaceTargetKey>()
    val labelId2Label = Int2ObjectBiMap<Label>()
    val targets = LinkedHashMap<WorkspaceTargetKey, BuildTarget>()
    val hash2KeyIds = Long2ObjectOpenHashMap<IntArrayList>()

    for (n in 0 until count) {
      val targetKey = key("@//pkg$n:target")
      val raw = rawTarget(targetKey, n)
      val keyId = n + 1
      keyId2Target[keyId] = targetKey
      labelId2Label[keyId] = targetKey.label
      targets[targetKey] = raw
      generation.saveTarget(WorkspaceTargetToSave(keyId = keyId, target = raw))
      raw.sources.getFiles().forEach { file ->
        hash2KeyIds.computeIfAbsent(hashFilePath(file)) { IntArrayList() }.add(keyId)
      }
    }
    generation.saveFileToTargetMap(hash2KeyIds)

    val partial = PersistentWorkspaceSnapshot(
      configurations = emptyMap(),
      targetGraph = WorkspaceTargetGraph.EMPTY,
      syncConfigs = emptyList(),
      repoMapping = RepoMappingDisabled,
      metadata = WorkspaceSnapshotMetadata(version = 7),
      keyId2Target = keyId2Target,
      labelId2Label = labelId2Label,
    )
    return SavedState(targets, partial)
  }

  private fun assertComposedEquals(actual: BuildTarget?, expected: BuildTarget) {
    actual.shouldNotBeNull()
    actual.kind shouldBe expected.kind
    actual.baseDirectory shouldBe expected.baseDirectory
    actual.dependencies shouldBe expected.dependencies
    actual.sources shouldBe expected.sources
    // composition orders data by registration id
    actual.data.toSet() shouldBe expected.data.toSet()
  }

  private suspend fun saveAllBulk(generation: SnapshotGeneration, count: Int): SavedState {
    val keyId2Target = Int2ObjectBiMap<WorkspaceTargetKey>()
    val labelId2Label = Int2ObjectBiMap<Label>()
    val targets = LinkedHashMap<WorkspaceTargetKey, BuildTarget>()
    val hash2KeyIds = Long2ObjectOpenHashMap<IntArrayList>()
    val toSave = ArrayList<WorkspaceTargetToSave>(count)

    for (n in 0 until count) {
      val targetKey = key("@//pkg$n:target")
      val raw = rawTarget(targetKey, n)
      val keyId = n + 1
      keyId2Target[keyId] = targetKey
      labelId2Label[keyId] = targetKey.label
      targets[targetKey] = raw
      toSave += WorkspaceTargetToSave(keyId = keyId, target = raw)
      raw.sources.getFiles().forEach { file ->
        hash2KeyIds.computeIfAbsent(hashFilePath(file)) { IntArrayList() }.add(keyId)
      }
    }
    generation.saveTargets(toSave)
    generation.saveFileToTargetMap(hash2KeyIds)

    val partial = PersistentWorkspaceSnapshot(
      configurations = emptyMap(),
      targetGraph = WorkspaceTargetGraph.EMPTY,
      syncConfigs = emptyList(),
      repoMapping = RepoMappingDisabled,
      metadata = WorkspaceSnapshotMetadata(version = 7),
      keyId2Target = keyId2Target,
      labelId2Label = labelId2Label,
    )
    return SavedState(targets, partial)
  }

  @Test
  fun `bulk-saved targets through the string table after reopen`(): Unit = runBlocking {
    val dbFile = tempDir.resolve("snapshot_db.data")

    val state = SnapshotStorage(dbFile, buildKryo()).let { storage ->
      val generation = storage.createGeneration(1)
      val state = saveAllBulk(generation, count = 3)
      storage.commit()
      storage.close()
      state
    }

    val reopenedStorage = SnapshotStorage(dbFile, buildKryo())
    try {
      val reopened = reopenedStorage.openGeneration(1)
      for ((targetKey, raw) in state.targets) {
        assertComposedEquals(reopened.findOrLoadTarget(state.partial, targetKey, TargetLoadOptions.ALL), raw)
      }
      reopened.findTargetsByFile(state.partial, Path.of("/workspace/shared/Shared.java")).toSet() shouldBe state.targets.keys
    }
    finally {
      reopenedStorage.close()
    }
  }

  @Test
  fun `targets survive commit and reopen`() {
    val dbFile = tempDir.resolve("snapshot_db.data")

    val state = SnapshotStorage(dbFile, buildKryo()).let { storage ->
      val generation = storage.createGeneration(1)
      val state = saveAll(generation, count = 3)
      storage.commit()
      storage.close()
      state
    }

    val reopenedStorage = SnapshotStorage(dbFile, buildKryo())
    val reopened = reopenedStorage.openGeneration(1)
    try {
      for ((targetKey, raw) in state.targets) {
        assertComposedEquals(reopened.findOrLoadTarget(state.partial, targetKey, TargetLoadOptions.ALL), raw)
      }
      reopened.findTargetsByFile(state.partial, Path.of("/workspace/shared/Shared.java")).toSet() shouldBe state.targets.keys
      reopened.findTargetsByFile(state.partial, Path.of("/workspace/pkg1/Main.java")).toList() shouldBe listOf(key("@//pkg1:target"))
    }
    finally {
      reopenedStorage.close()
    }
  }

  @Test
  fun `partial load options are honored after reopen`() {
    val dbFile = tempDir.resolve("snapshot_db.data")

    val state = SnapshotStorage(dbFile, buildKryo()).let { storage ->
      val generation = storage.createGeneration(1)
      val state = saveAll(generation, count = 3)
      storage.commit()
      storage.close()
      state
    }

    val reopenedStorage = SnapshotStorage(dbFile, buildKryo())
    val reopened = reopenedStorage.openGeneration(1)
    try {
      val skipFiles = reopened.findOrLoadTarget(
        state.partial,
        key("@//pkg0:target"),
        TargetLoadOptions(TargetSection.INFO, TargetSection.DEPS, targetData = BuildTargetLoadHint.All),
      )
      skipFiles.shouldNotBeNull()
      skipFiles.loaded.sections shouldBe setOf(TargetSection.INFO, TargetSection.DEPS)
      skipFiles.isFull shouldBe false
      skipFiles.dependencies shouldBe state.targets.getValue(key("@//pkg0:target")).dependencies
      skipFiles.sources shouldBe state.targets.getValue(key("@//pkg0:target")).sources
      skipFiles.loaded.sections shouldBe setOf(TargetSection.INFO, TargetSection.DEPS)

      val skipDeps = reopened.findOrLoadTarget(
        state.partial,
        key("@//pkg1:target"),
        TargetLoadOptions(TargetSection.INFO, TargetSection.FILE_SETS, targetData = BuildTargetLoadHint.All),
      )
      skipDeps.shouldNotBeNull()
      skipDeps.dependencies shouldBe state.targets.getValue(key("@//pkg1:target")).dependencies

      val onlyToolchain = reopened.findOrLoadTarget(
        state.partial,
        key("@//pkg2:target"),
        TargetLoadOptions(
          TargetSection.INFO,
          TargetSection.DEPS,
          TargetSection.FILE_SETS,
          targetData = BuildTargetLoadHint.Subset(setOf(JavaToolchainData::class.java)),
        ),
      )
      onlyToolchain.shouldNotBeNull()
      onlyToolchain.data shouldBe listOf(JavaToolchainData(sourceVersion = "17", targetVersion = "17"))
      onlyToolchain.data shouldBe listOf(JavaToolchainData(sourceVersion = "17", targetVersion = "17"))
    }
    finally {
      reopenedStorage.close()
    }
  }

  @Test
  fun `target data left out of the load options is materialized on first read`() {
    val dbFile = tempDir.resolve("snapshot_db.data")

    val state = SnapshotStorage(dbFile, buildKryo()).let { storage ->
      val generation = storage.createGeneration(1)
      val state = saveAll(generation, count = 1)
      storage.commit()
      storage.close()
      state
    }

    val reopenedStorage = SnapshotStorage(dbFile, buildKryo())
    val reopened = reopenedStorage.openGeneration(1)
    try {
      val targetKey = key("@//pkg0:target")
      val summary = reopened.findOrLoadTarget(state.partial, targetKey, TargetLoadOptions.SUMMARY)
      summary.shouldNotBeNull()
      summary.loaded.targetData shouldBe BuildTargetLoadHint.None

      summary.data.toSet() shouldBe state.targets.getValue(targetKey).data.toSet()
      summary.loaded shouldBe TargetLoadOptions.SUMMARY
    }
    finally {
      reopenedStorage.close()
    }
  }

  @Test
  fun `sections of a retired generation resolve to empty instead of failing`() {
    val dbFile = tempDir.resolve("snapshot_db.data")

    val state = SnapshotStorage(dbFile, buildKryo()).let { storage ->
      val generation = storage.createGeneration(1)
      val state = saveAll(generation, count = 1)
      storage.commit()
      storage.close()
      state
    }

    val reopenedStorage = SnapshotStorage(dbFile, buildKryo())
    val summary = reopenedStorage.openGeneration(1)
      .findOrLoadTarget(state.partial, key("@//pkg0:target"), TargetLoadOptions.SUMMARY)
    summary.shouldNotBeNull()
    reopenedStorage.close()

    summary.dependencies shouldBe emptyList()
    summary.data shouldBe emptyList()
    summary.sources.isEmpty() shouldBe true
  }

  @Test
  fun `a section stays readable once materialized, even after its generation is retired`() {
    val dbFile = tempDir.resolve("snapshot_db.data")

    val state = SnapshotStorage(dbFile, buildKryo()).let { storage ->
      val generation = storage.createGeneration(1)
      val state = saveAll(generation, count = 1)
      storage.commit()
      storage.close()
      state
    }

    val targetKey = key("@//pkg0:target")
    val expected = state.targets.getValue(targetKey)
    val reopenedStorage = SnapshotStorage(dbFile, buildKryo())
    val summary = reopenedStorage.openGeneration(1)
      .findOrLoadTarget(state.partial, targetKey, TargetLoadOptions.SUMMARY)
    summary.shouldNotBeNull()

    // materialize while the generation is still live
    summary.sources shouldBe expected.sources
    summary.dependencies shouldBe expected.dependencies
    reopenedStorage.close()

    // the section was cached, retiring the generation must not turn it back into an empty one
    summary.sources shouldBe expected.sources
    summary.dependencies shouldBe expected.dependencies
  }

  @Test
  fun `sections deferred by a scan resolve per target once the scan is over`() {
    val dbFile = tempDir.resolve("snapshot_db.data")

    val state = SnapshotStorage(dbFile, buildKryo()).let { storage ->
      val generation = storage.createGeneration(1)
      val state = saveAll(generation, count = 20)
      storage.commit()
      storage.close()
      state
    }

    val storage = SnapshotStorage(dbFile, buildKryo())
    try {
      val depsOnly = storage.openGeneration(1)
        .scanAllTargets(state.partial, TargetLoadOptions(TargetSection.INFO, TargetSection.DEPS))
        .toList()
      depsOnly.size shouldBe 20

      for (target in depsOnly.reversed()) {
        val expected = state.targets.getValue(target.key)
        target.sources shouldBe expected.sources
        target.data.toSet() shouldBe expected.data.toSet()
      }
    }
    finally {
      storage.close()
    }
  }

  @Test
  fun `full cached target is served for partial requests, partial loads are not cached`() {
    val dbFile = tempDir.resolve("snapshot_db.data")
    val state = SnapshotStorage(dbFile, buildKryo()).let { storage ->
      val generation = storage.createGeneration(1)
      val state = saveAll(generation, count = 1)
      storage.commit()
      storage.close()
      state
    }

    val reopenedStorage = SnapshotStorage(dbFile, buildKryo())
    val reopened = reopenedStorage.openGeneration(1)
    try {
      val skipFileSets = TargetLoadOptions(TargetSection.INFO, TargetSection.DEPS, targetData = BuildTargetLoadHint.All)
      val partial1 = reopened.findOrLoadTarget(state.partial, key("@//pkg0:target"), skipFileSets)
      val full = reopened.findOrLoadTarget(state.partial, key("@//pkg0:target"), TargetLoadOptions.ALL)
      val partial2 = reopened.findOrLoadTarget(state.partial, key("@//pkg0:target"), skipFileSets)

      full.shouldNotBeNull()
      full.isFull shouldBe true
      partial2 shouldBe full
      partial1.shouldNotBeNull()
      partial1.isFull shouldBe false
    }
    finally {
      reopenedStorage.close()
    }
  }

  @Test
  fun `pages holding many values decode correctly`() {
    val dbFile = tempDir.resolve("snapshot_db.data")
    val count = 400

    val state = SnapshotStorage(dbFile, buildKryo()).let { storage ->
      val generation = storage.createGeneration(1)
      val state = saveAll(generation, count = count)
      storage.commit()
      storage.close()
      state
    }

    val reopenedStorage = SnapshotStorage(dbFile, buildKryo())
    val reopened = reopenedStorage.openGeneration(1)
    try {
      reopened.findTargetsByFile(state.partial, Path.of("/workspace/shared/Shared.java")).toSet() shouldBe state.targets.keys
      for (n in 0 until count) {
        reopened.findTargetsByFile(state.partial, Path.of("/workspace/pkg$n/Main.java")).toList() shouldBe listOf(key("@//pkg$n:target"))
      }
      for ((targetKey, raw) in state.targets) {
        assertComposedEquals(reopened.findOrLoadTarget(state.partial, targetKey, TargetLoadOptions.ALL), raw)
      }
    }
    finally {
      reopenedStorage.close()
    }
  }

  @Test
  fun `scanAllTargets streams every target in one pass`() {
    val dbFile = tempDir.resolve("snapshot_db.data")
    val state = SnapshotStorage(dbFile, buildKryo()).let { storage ->
      val generation = storage.createGeneration(1)
      val state = saveAll(generation, count = 50)
      storage.commit()
      storage.close()
      state
    }

    val storage = SnapshotStorage(dbFile, buildKryo())
    try {
      val generation = storage.openGeneration(1)
      val scanned = generation.scanAllTargets(state.partial, TargetLoadOptions.ALL).toList()
      scanned.map { it.key }.toSet() shouldBe state.targets.keys
      for (target in scanned) {
        target.isFull shouldBe true
        assertComposedEquals(target, state.targets.getValue(target.key))
      }

      // partial scan decodes only the requested sections
      val depsOnly = generation
        .scanAllTargets(state.partial, TargetLoadOptions(TargetSection.INFO, TargetSection.DEPS))
        .toList()
      depsOnly.size shouldBe 50
      for (target in depsOnly) {
        target.isFull shouldBe false
        target.dependencies.size shouldBe 1
        target.sources shouldBe state.targets.getValue(target.key).sources
        target.data.toSet() shouldBe state.targets.getValue(target.key).data.toSet()
      }
    }
    finally {
      storage.close()
    }
  }

  @Test
  fun `sweep removes generations whose maps were never opened in this session`() {
    val dbFile = tempDir.resolve("snapshot_db.data")
    SnapshotStorage(dbFile, buildKryo()).let { storage ->
      val generation = storage.createGeneration(1)
      saveAll(generation, count = 3)
      storage.commit()
      storage.close()
    }

    val reopened = SnapshotStorage(dbFile, buildKryo())
    try {
      reopened.hasGeneration(1) shouldBe true
      reopened.sweepGenerationsExcept(setOf(2))
      reopened.commit()
      reopened.hasGeneration(1) shouldBe false
    }
    finally {
      reopened.close()
    }
  }

  @Test
  fun `sweep defers removal while a reader still holds the generation`() {
    val dbFile = tempDir.resolve("snapshot_db.data")
    val storage = SnapshotStorage(dbFile, buildKryo())
    try {
      val gen1 = storage.createGeneration(1)
      val state = saveAll(gen1, count = 2)
      storage.commit()

      // a reader still holds gen1 while gen2 is written and gen1 is swept
      storage.sweepGenerationsExcept(setOf(2))
      val gen2 = storage.createGeneration(2)
      saveAll(gen2, count = 2)
      storage.commit()

      // removal was deferred, the held generation stays fully readable
      storage.hasGeneration(1) shouldBe true
      assertComposedEquals(
        gen1.findOrLoadTarget(state.partial, key("@//pkg0:target"), TargetLoadOptions.ALL),
        state.targets.getValue(key("@//pkg0:target")),
      )
      gen1.findTargetsByFile(state.partial, Path.of("/workspace/shared/Shared.java")).toSet() shouldBe state.targets.keys
    }
    finally {
      storage.close()
    }
  }

  @Test
  fun `sweep removes the generation once no reader holds it`() {
    val dbFile = tempDir.resolve("snapshot_db.data")
    val storage = SnapshotStorage(dbFile, buildKryo())
    try {
      val watcher = createAndDropGeneration(storage)
      watcher.ensureCollected()

      storage.sweepGenerationsExcept(setOf(2))
      storage.commit()
      storage.hasGeneration(1) shouldBe false
    }
    finally {
      storage.close()
    }
  }

  private fun createAndDropGeneration(storage: SnapshotStorage): GCWatcher {
    val generation = storage.createGeneration(1)
    saveAll(generation, count = 2)
    storage.commit()
    return GCWatcher.tracking(generation)
  }
}
