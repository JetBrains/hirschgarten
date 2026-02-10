package org.jetbrains.bazel.sync.target

import com.intellij.openapi.util.io.NioFiles
import com.intellij.testFramework.junit5.TestApplication
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.label.ResolvedLabel
import org.jetbrains.bazel.magicmetamodel.formatAsModuleName
import org.jetbrains.bazel.target.TargetsCacheStorage
import org.jetbrains.bazel.test.framework.target.TestBuildTargetFactory
import org.jetbrains.bazel.workspace.model.test.framework.WorkspaceModelBaseTest
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.LibraryItem
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path

@DisplayName("TargetsCacheStorage tests")
@TestApplication
class TargetsCacheStorageTest : WorkspaceModelBaseTest() {
  private lateinit var storage: TargetsCacheStorage
  private lateinit var tempDir: Path
  private lateinit var storeFile: Path

  @BeforeEach
  override fun beforeEach() {
    super.beforeEach()
    tempDir = Files.createTempDirectory("targets-cache-store")
    storeFile = tempDir.resolve("targets.db")
    storage = TargetsCacheStorage.openStore(storeFile, project)
  }

  @AfterEach
  fun tearDown() {
    storage.save()
    NioFiles.deleteRecursively(tempDir)
  }


  @Test
  fun `file-to-target roundtrip`() {
    val label: ResolvedLabel = Label.parse("@//pkg:lib") as ResolvedLabel
    val file = projectBasePath.resolve("pkg/File.java")

    storage.addFileToTarget(file, listOf(label))
    storage.setTargets(listOf(TestBuildTargetFactory.createSimpleJavaLibraryTarget(id = label)))

    storage.getTargetsForPath(file)!!.shouldContainExactly(listOf(label))

    storage.removeFileToTarget(file)
    storage.getTargetsForPath(file).shouldBeNull()
  }

  @Test
  fun `file-to-targets roundtrip`() {
    val label1: Label = Label.parse("@//pkg1:lib")
    val label2: Label = Label.parse("@//pkg2:lib")
    val file = projectBasePath.resolve("pkg/File.java")

    storage.addFileToTarget(file, listOf(label1, label2))
    storage.setTargets(
      listOf(
        TestBuildTargetFactory.createSimpleJavaLibraryTarget(id = label1),
        TestBuildTargetFactory.createSimpleJavaLibraryTarget(id = label2),
      )
    )

    storage.getTargetsForPath(file)!!.shouldContainExactly(listOf(label1, label2))

    storage.removeFileToTarget(file)
    storage.getTargetsForPath(file).shouldBeNull()
  }

  @Test
  fun `setTargets and getBuildTargetForLabel`() {
    val label: ResolvedLabel = Label.parse("@//a:b") as ResolvedLabel
    val target = TestBuildTargetFactory.createSimpleJavaLibraryTarget(id = label)

    storage.setTargets(listOf(target))

    val resolved = storage.getBuildTargetForLabel(label)
    resolved!!.id shouldBe label
    storage.getTotalTargetCount() shouldBe 1
  }

  @Test
  fun `reset populates caches for files, executables, libraries and modules`() {
    val label1: ResolvedLabel = Label.parse("@//lib:core") as ResolvedLabel
    val label2: ResolvedLabel = Label.parse("@//bin:tool") as ResolvedLabel

    val file = projectBasePath.resolve("lib/Core.kt")

    // minimal library item – other fields are irrelevant for mapping id -> label
    val libraryItem = LibraryItem(
      id = label1,
      dependencies = emptyList(),
      ijars = emptyList(),
      jars = emptyList(),
      sourceJars = emptyList(),
      mavenCoordinates = null,
      containsInternalJars = false,
      isLowPriority = false,
    )

    storage.reset(
      fileToTarget = mapOf<Path, List<Label>>(file to listOf(label1)),
      executableTargets = mapOf<ResolvedLabel, List<Label>>(label1 to listOf(label2)),
      libraryItems = listOf(libraryItem),
      targets = listOf(TestBuildTargetFactory.createSimpleJavaLibraryTarget(id = label1),
                       TestBuildTargetFactory.createSimpleJavaLibraryTarget(id = label2)),
    )

    // files
    storage.getTargetsForPath(file)!!.shouldContainExactly(listOf(label1))
    storage.getExecutableTargetsForTarget(label1)!!.shouldContainExactly(listOf(label2))

    // libraries and modules are stored under hashed ids derived from formatted module/library names
    val libraryId = label1.formatAsModuleName(project)
    storage.getTargetForLibraryId(libraryId) shouldBe label1

    val moduleResolved = storage.getTargetForModuleId(libraryId)
    moduleResolved shouldBe label1

    // targets
    storage.getAllTargets().toList().size shouldBe 2
    storage.getTotalTargetCount() shouldBe 2
  }

  @Test
  fun `persistence after save and reopen`() {
    val label: ResolvedLabel = Label.parse("@//persist:me") as ResolvedLabel
    val target = TestBuildTargetFactory.createSimpleJavaLibraryTarget(id = label)

    storage.setTargets(listOf(target))
    storage.save()
    storage.close()

    // reopen store
    storage = TargetsCacheStorage.openStore(storeFile, project)

    val restored = storage.getBuildTargetForLabel(label)
    restored!!.id shouldBe label
  }
}
