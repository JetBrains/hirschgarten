package org.jetbrains.bazel.target

import com.intellij.configurationStore.SettingsSavingComponent
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.kotest.matchers.types.shouldNotBeSameInstanceAs
import kotlinx.coroutines.runBlocking
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.python.lang.PythonBuildTarget
import org.jetbrains.bazel.sync.workspace.languages.jvm.JavaToolchainData
import org.jetbrains.bazel.sync.workspace.languages.jvm.JvmBuildTarget
import org.jetbrains.bazel.sync.workspace.persistence.WorkspaceSnapshotService
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceConfigurationId
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSnapshot
import org.jetbrains.bazel.sync.workspace.snapshot.workspaceSnapshotOrNull
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.testproject.javaBinary
import org.jetbrains.bazel.test.framework.testproject.javaLibrary
import org.jetbrains.bazel.test.framework.testproject.sync
import org.jetbrains.bazel.test.framework.testproject.testBazelProjectFixture
import org.jetbrains.bazel.test.framework.testproject.workspaceRoot
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.BuildTargetInfo
import org.jetbrains.bsp.protocol.BuildTargetTag
import org.jetbrains.bsp.protocol.StringsCollection
import org.jetbrains.bsp.protocol.data
import org.jetbrains.bsp.protocol.id
import org.junit.jupiter.api.Test

@BazelTestApplication
class TargetStorageTest {

  private val testBazel by testBazelProjectFixture(openAfterCreation = false, persistentSnapshots = true)
  private val project: Project get() = testBazel.project

  private suspend fun WorkspaceSnapshotService.save() = (this as SettingsSavingComponent).save()

  @Test
  fun `empty snapshot answers empty everywhere`(): Unit = runBlocking {
    val targetUtils = project.targetStorage
    targetUtils.getTotalTargetCount() shouldBe 0
    targetUtils.allTargetInfos() shouldBe emptyList()
    targetUtils.getTargetInfo(Label.parse("//nothing:nothing")) shouldBe null
    targetUtils.getTargetsForPath(testBazel.workspaceRoot.resolve("Nothing.java")) shouldBe emptyList()
  }

  @Test
  fun `target infos expose the depth-filtered import set only`(): Unit = runBlocking {
    val bin = javaBinary("//app:bin") {
      deps("//lib:lib")
      tags(BuildTargetTag.MANUAL)
    }
    val lib = javaLibrary("//lib:lib") { deps("//deep:deep") }
    val deep = javaLibrary("//deep:deep")
    testBazel.sync {
      targets(bin, lib, deep)
      roots(bin)
      importDepth(1)
    }

    val infos = project.targetStorage.allTargetInfos().associateBy { it.id }
    infos.keys shouldBe setOf(bin.key.label, lib.key.label)
    infos.getValue(bin.key.label) shouldBe bin.info
  }

  @Test
  fun `getTargetDataForLabel loads language data before and after persist`(): Unit = runBlocking {
    val jvmData = JvmBuildTarget(javacOpts = StringsCollection.of(listOf("-parameters")), mainClass = "com.example.Main")
    val lib = javaLibrary("//lib:lib") {
      deps("//other:other")
      data(jvmData)
    }
    val other = javaLibrary("//other:other")
    testBazel.sync { targets(lib, other) }

    val targetUtils = project.targetStorage

    // in-memory (clean resync) snapshot holds full targets, language data is present
    targetUtils.getTargetDataForLabel(lib.key.label, JvmBuildTarget::class.java) shouldBe jvmData

    project.service<WorkspaceSnapshotService>().save()

    // store-backed snapshot, the requested data frame is still loaded exactly, with no sentinel leaking
    targetUtils.getTargetDataForLabel(lib.key.label, JvmBuildTarget::class.java) shouldBe jvmData
  }

  @Test
  fun `getTargetDataForLabel loads a single data type`(): Unit = runBlocking {
    val jvmData = JvmBuildTarget(javacOpts = StringsCollection.of(listOf("-parameters")), mainClass = "com.example.Main")
    val toolchainData = JavaToolchainData(sourceVersion = "17", targetVersion = "17")
    val lib = javaLibrary("//lib:lib") { data(jvmData, toolchainData) }
    testBazel.sync { targets(lib) }

    val targetUtils = project.targetStorage
    targetUtils.getTargetDataForLabel(lib.key.label, JvmBuildTarget::class.java) shouldBe jvmData
    targetUtils.getTargetDataForLabel(lib.key.label, PythonBuildTarget::class.java) shouldBe null
  }

  @Test
  fun `label collisions prefer the empty-configuration key`(): Unit = runBlocking {
    val label = Label.parse("//lib:lib")
    val configured = javaLibrary("//lib:lib") { configuration(WorkspaceConfigurationId.of("abcdef1")) }
    val plain = javaLibrary("//lib:lib")
    testBazel.sync { targets(configured, plain) }

    val summary = project.targetStorage.getTargetInfo(label)
    summary?.key shouldBe plain.key
  }

  @Test
  fun `addFileToTargetIdEntry and removeFileToTargetIdEntry are visible through getTargetsForPath`(): Unit = runBlocking {
    val bin = javaBinary("//app:bin") { deps("//lib:lib") }
    val lib = javaLibrary("//lib:lib") { deps("//deep:deep") }
    val deep = javaLibrary("//deep:deep")
    testBazel.sync {
      targets(bin, lib, deep)
      roots(bin)
      importDepth(1)
    }
    val file = testBazel.workspaceRoot.resolve("src/Extra.java")

    val targetUtils = project.targetStorage
    targetUtils.addFileToTargetIdEntry(file, listOf(bin.key.label, deep.key.label))
    targetUtils.getTargetsForPath(file) shouldBe listOf(bin.key.label)

    targetUtils.removeFileToTargetIdEntry(file)
    targetUtils.getTargetsForPath(file) shouldBe emptyList()
  }

  @Test
  fun `getExecutableTargetsForTarget answers from the snapshot index`(): Unit = runBlocking {
    val bin = javaBinary("//app:bin") { deps("//lib:lib") }
    val lib = javaLibrary("//lib:lib")
    testBazel.sync {
      targets(bin, lib)
      roots(bin)
    }

    project.targetStorage.getExecutableTargetsForTarget(lib.key.label).shouldContainExactly(bin.key.label)
  }

  @Test
  fun `answers are identical after the snapshot is persisted`(): Unit = runBlocking {
    val jvmData = JvmBuildTarget(javacOpts = StringsCollection.of(listOf("-parameters")), mainClass = "com.example.Main")
    val file = testBazel.workspaceRoot.resolve("app/Bin.java")
    val bin = javaBinary("//app:bin") {
      deps("//lib:lib")
      srcs("app/Bin.java")
    }
    val lib = javaLibrary("//lib:lib") { data(jvmData) }
    testBazel.sync {
      targets(bin, lib)
      roots(bin)
    }

    val targetUtils = project.targetStorage

    val infosBefore = targetUtils.allTargetInfos()
    val targetsForPathBefore = targetUtils.getTargetsForPath(file)
    val executableBefore = targetUtils.getExecutableTargetsForTarget(lib.key.label)
    val dataBefore = targetUtils.getTargetDataForLabel(lib.key.label, JvmBuildTarget::class.java)

    project.service<WorkspaceSnapshotService>().save()

    targetUtils.allTargetInfos() shouldBe infosBefore
    targetUtils.getTargetsForPath(file) shouldBe targetsForPathBefore
    targetUtils.getExecutableTargetsForTarget(lib.key.label) shouldBe executableBefore
    targetUtils.getTargetDataForLabel(lib.key.label, JvmBuildTarget::class.java) shouldBe dataBefore
  }

  @Test
  fun `a persisted target gives its info from memory and its complete target on request`(): Unit = runBlocking {
    val jvmData = JvmBuildTarget(javacOpts = StringsCollection.of(listOf("-parameters")), mainClass = "com.example.Main")
    val bin = javaBinary("//app:bin") {
      deps("//lib:lib")
      data(jvmData)
    }
    val lib = javaLibrary("//lib:lib")
    testBazel.sync { targets(bin, lib) }
    project.service<WorkspaceSnapshotService>().save()

    project.targetStorage.getTargetInfo(bin.key.label) shouldBe bin.info

    val target = project.targetStorage.getBuildTarget(bin.key.label).shouldNotBeNull()
    target.info shouldBe bin.info
    target.sources.getOutputLocations().toList() shouldBe bin.sources.getOutputLocations().toList()
    target.dependencies shouldBe bin.dependencies
    target.data(JvmBuildTarget::class.java) shouldBe jvmData

    project.targetStorage.getTargetDataForLabel(bin.key.label, JvmBuildTarget::class.java) shouldBe jvmData
    project.targetStorage.getBuildTarget(Label.parse("//app:missing")) shouldBe null
  }

  @Test
  fun `data selects from what a target already carries`() {
    val jvmData = JvmBuildTarget(javacOpts = StringsCollection.of(listOf("-parameters")), mainClass = "com.example.Main")
    val toolchain = JavaToolchainData(sourceVersion = "17", targetVersion = "17")
    val full = javaLibrary("//lib:lib") { data(jvmData, toolchain) }

    full.data(JvmBuildTarget::class.java) shouldBe jvmData
    full.data(JavaToolchainData::class.java) shouldBe toolchain
    full.data(PythonBuildTarget::class.java) shouldBe null
  }

  @Test
  fun `a target info finds its full target in a persisted snapshot`(): Unit = runBlocking {
    val jvmData = JvmBuildTarget(javacOpts = StringsCollection.of(listOf("-parameters")), mainClass = "com.example.Main")
    val bin = javaBinary("//app:bin") { data(jvmData) }
    testBazel.sync { targets(bin) }
    project.service<WorkspaceSnapshotService>().save()

    val info = project.targetStorage.getTargetInfo(bin.key.label).shouldNotBeNull()
    info shouldBe bin.info

    val snapshot = project.workspaceSnapshotOrNull.shouldNotBeNull()
    val full = info.toFullTarget(snapshot).shouldNotBeNull()
    full.info shouldBe info
    full.data(JvmBuildTarget::class.java) shouldBe jvmData

    BuildTargetInfo(Label.parse("//app:missing"), bin.kind).toFullTarget(snapshot) shouldBe null
  }

  @Test
  fun `allTargetInfos returns the same cached List instance for the same snapshot`(): Unit = runBlocking {
    val bin = javaBinary("//app:bin")
    testBazel.sync { targets(bin) }

    val targetUtils = project.targetStorage
    val first = targetUtils.allTargetInfos()
    val second = targetUtils.allTargetInfos()
    second shouldBeSameInstanceAs first

    testBazel.sync { targets(bin) }

    val afterNewSnapshot = targetUtils.allTargetInfos()
    afterNewSnapshot shouldNotBeSameInstanceAs first
  }

  @Test
  fun `getTargetsForPath returns only one target with multiple configurations`(): Unit = runBlocking {
    val label = Label.parse("//lib:lib")
    val file = testBazel.workspaceRoot.resolve("app/Bin.java")
    val first = javaLibrary("//lib:lib") {
      configuration(WorkspaceConfigurationId.of("abcdef1"))
      srcs("app/Bin.java")
    }
    val second = javaLibrary("//lib:lib") {
      configuration(WorkspaceConfigurationId.of("abcdef2"))
      srcs("app/Bin.java")
    }
    testBazel.sync { targets(first, second) }

    val targetUtils = project.targetStorage
    targetUtils.getTargetsForPath(file) shouldBe listOf(label)
  }

  fun BuildTargetInfo.toFullTarget(snapshot: WorkspaceSnapshot): BuildTarget? =
    snapshot.targets.findTargetByKey(key)
}
