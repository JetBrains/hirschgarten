@file:Suppress("LongMethod", "MaxLineLength")

package org.jetbrains.magicmetamodel.impl

import ch.epfl.scala.bsp4j.SourceItemKind
import com.intellij.openapi.command.WriteCommandAction
import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import org.jetbrains.magicmetamodel.DocumentTargetsDetails
import org.jetbrains.magicmetamodel.MagicMetaModelProjectConfig
import org.jetbrains.magicmetamodel.ProjectDetails
import org.jetbrains.workspace.model.constructors.BuildTarget
import org.jetbrains.workspace.model.constructors.BuildTargetId
import org.jetbrains.workspace.model.constructors.SourceItem
import org.jetbrains.workspace.model.constructors.SourcesItem
import org.jetbrains.workspace.model.constructors.TextDocumentId
import org.jetbrains.workspace.model.test.framework.WorkspaceModelBaseTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

// TODO add checking workspacemodel
// TODO extract 'given' to separate objects
@DisplayName("MagicMetaModelImpl tests")
class MagicMetaModelImplTest : WorkspaceModelBaseTest() {

  private lateinit var testMagicMetaModelProjectConfig: MagicMetaModelProjectConfig

  @BeforeEach
  override fun beforeEach() {
    // given
    super.beforeEach()

    testMagicMetaModelProjectConfig =
      MagicMetaModelProjectConfig(workspaceModel, virtualFileUrlManager, projectBaseDirPath)
  }

  @Nested
  @DisplayName("MagicMetaModelImpl \"real world\" flow tests")
  inner class MagicMetaModelImplTest {

    @Test
    fun `should handle empty project (something strange happened)`() {
      // given
      val projectDetails = ProjectDetails(
        targetsId = emptyList(),
        targets = emptySet(),
        sources = emptyList(),
        resources = emptyList(),
        dependenciesSources = emptyList(),
      )

      // when 1
      val magicMetaModel = MagicMetaModelImpl(testMagicMetaModelProjectConfig, projectDetails)

      // then 1
      // showing loaded and not loaded targets to user (e.g. at the sidebar)
      magicMetaModel.getAllLoadedTargets() shouldBe emptyList()
      magicMetaModel.getAllNotLoadedTargets() shouldBe emptyList()

      // when 2
      WriteCommandAction.runWriteCommandAction(project) {
        magicMetaModel.loadDefaultTargets()
      }

      // then 2
      // showing loaded and not loaded targets to user (e.g. at the sidebar)
      magicMetaModel.getAllLoadedTargets() shouldBe emptyList()
      magicMetaModel.getAllNotLoadedTargets() shouldBe emptyList()
    }

    @Test
    fun `should handle project without shared sources (like a simple kotlin project)`() {
      // given
      val targetA1 = BuildTarget(
        id = BuildTargetId("targetA1"),
        languageIds = listOf("kotlin"),
        dependencies = listOf(
          BuildTargetId("externalDep1"),
          BuildTargetId("externalDep2"),
        ),
      )

      val targetB1 = BuildTarget(
        id = BuildTargetId("targetB1"),
        languageIds = listOf("kotlin"),
      )

      val targetC1 = BuildTarget(
        id = BuildTargetId("targetC1"),
        languageIds = listOf("kotlin"),
        dependencies = listOf(
          BuildTargetId("targetB1"),
          BuildTargetId("externalDep1"),
          BuildTargetId("externalDep2"),
        ),
      )

      val targetD1 = BuildTarget(
        id = BuildTargetId("targetD1"),
        languageIds = listOf("kotlin"),
        dependencies = listOf(
          BuildTargetId("targetA1"),
          BuildTargetId("targetB1"),
          BuildTargetId("targetC1"),
          BuildTargetId("externalDep1"),
        ),
      )

      val targetA1Source1 = SourceItem(
        uri = "file:///project/targetA1/src/main/kotlin/",
        kind = SourceItemKind.DIRECTORY,
      )
      val targetA1Sources = SourcesItem(
        target = targetA1.id,
        sources = listOf(targetA1Source1),
      )

      val targetB1Source1 = SourceItem(
        uri = "file:///project/targetB1/src/main/kotlin/File1.kt",
        kind = SourceItemKind.FILE,
      )
      val targetB1Sources = SourcesItem(
        target = targetB1.id,
        sources = listOf(targetB1Source1),
      )

      val targetC1Source1 = SourceItem(
        uri = "file:///project/targetC1/src/main/kotlin/",
        kind = SourceItemKind.DIRECTORY,
      )
      val targetC1Sources = SourcesItem(
        target = targetC1.id,
        sources = listOf(targetC1Source1),
      )

      val targetD1Source1 = SourceItem(
        uri = "file:///project/targetD1/src/main/kotlin/File1.kt",
        kind = SourceItemKind.FILE,
      )
      val targetD1Sources = SourcesItem(
        target = targetD1.id,
        sources = listOf(targetD1Source1),
      )

      val projectDetails = ProjectDetails(
        targetsId = listOf(targetA1.id, targetB1.id, targetC1.id, targetD1.id),
        targets = setOf(targetA1, targetB1, targetC1, targetD1),
        sources = listOf(targetA1Sources, targetB1Sources, targetC1Sources, targetD1Sources),
        resources = emptyList(),
        dependenciesSources = emptyList(),
      )

      // when 1
      val magicMetaModel = MagicMetaModelImpl(testMagicMetaModelProjectConfig, projectDetails)

      // then 1
      // showing loaded and not loaded targets to user (e.g. at the sidebar)
      magicMetaModel.getAllLoadedTargets() shouldBe emptyList()
      magicMetaModel.getAllNotLoadedTargets() shouldContainExactlyInAnyOrder listOf(
        targetA1,
        targetB1,
        targetC1,
        targetD1
      )

      // user opens each file and checks the loaded target for each file (e.g. at the bottom bar widget)
      magicMetaModel.getTargetsDetailsForDocument(TextDocumentId(targetA1Source1.uri)) shouldBe DocumentTargetsDetails(
        loadedTargetId = null,
        notLoadedTargetsIds = listOf(targetA1.id)
      )
      magicMetaModel.getTargetsDetailsForDocument(TextDocumentId(targetB1Source1.uri)) shouldBe DocumentTargetsDetails(
        loadedTargetId = null,
        notLoadedTargetsIds = listOf(targetB1.id)
      )
      magicMetaModel.getTargetsDetailsForDocument(TextDocumentId(targetC1Source1.uri)) shouldBe DocumentTargetsDetails(
        loadedTargetId = null,
        notLoadedTargetsIds = listOf(targetC1.id)
      )
      magicMetaModel.getTargetsDetailsForDocument(TextDocumentId(targetD1Source1.uri)) shouldBe DocumentTargetsDetails(
        loadedTargetId = null,
        notLoadedTargetsIds = listOf(targetD1.id)
      )

      // when 2
      // after bsp importing process
      WriteCommandAction.runWriteCommandAction(project) {
        magicMetaModel.loadDefaultTargets()
      }

      // then 2
      // showing loaded and not loaded targets to user (e.g. at the sidebar)
      magicMetaModel.getAllLoadedTargets() shouldContainExactlyInAnyOrder listOf(targetA1, targetB1, targetC1, targetD1)
      magicMetaModel.getAllNotLoadedTargets() shouldBe emptyList()

      // user opens each file and checks the loaded target for each file (e.g. at the bottom bar widget)
      magicMetaModel.getTargetsDetailsForDocument(TextDocumentId(targetA1Source1.uri)) shouldBe DocumentTargetsDetails(
        loadedTargetId = targetA1.id,
        notLoadedTargetsIds = emptyList(),
      )
      magicMetaModel.getTargetsDetailsForDocument(TextDocumentId(targetB1Source1.uri)) shouldBe DocumentTargetsDetails(
        loadedTargetId = targetB1.id,
        notLoadedTargetsIds = emptyList(),
      )
      magicMetaModel.getTargetsDetailsForDocument(TextDocumentId(targetC1Source1.uri)) shouldBe DocumentTargetsDetails(
        loadedTargetId = targetC1.id,
        notLoadedTargetsIds = emptyList(),
      )
      magicMetaModel.getTargetsDetailsForDocument(TextDocumentId(targetD1Source1.uri)) shouldBe DocumentTargetsDetails(
        loadedTargetId = targetD1.id,
        notLoadedTargetsIds = emptyList(),
      )
    }

    @Test
    fun `should handle project with shared sources (like a scala cross version project)`() {
      // given
      val targetA1 = BuildTarget(
        id = BuildTargetId("targetA1"),
        languageIds = listOf("kotlin"),
        dependencies = listOf(
          BuildTargetId("externalDep1"),
          BuildTargetId("externalDep2"),
        ),
      )

      val targetB1 = BuildTarget(
        id = BuildTargetId("targetB1"),
        languageIds = listOf("kotlin"),
      )

      val targetB2 = BuildTarget(
        id = BuildTargetId("targetB2"),
        languageIds = listOf("kotlin"),
      )

      val targetC1 = BuildTarget(
        id = BuildTargetId("targetC1"),
        languageIds = listOf("kotlin"),
        dependencies = listOf(
          BuildTargetId("targetB1"),
          BuildTargetId("externalDep1"),
          BuildTargetId("externalDep2"),
        ),
      )

      val targetC2 = BuildTarget(
        id = BuildTargetId("targetC2"),
        languageIds = listOf("kotlin"),
        dependencies = listOf(
          BuildTargetId("targetB2"),
          BuildTargetId("externalDep1"),
          BuildTargetId("externalDep2"),
        ),
      )

      val targetD1 = BuildTarget(
        id = BuildTargetId("targetD1"),
        languageIds = listOf("kotlin"),
        dependencies = listOf(
          BuildTargetId("targetA1"),
          BuildTargetId("targetC1"),
          BuildTargetId("externalDep1"),
        ),
      )

      val targetD2 = BuildTarget(
        id = BuildTargetId("targetD2"),
        languageIds = listOf("kotlin"),
        dependencies = listOf(
          BuildTargetId("targetC2"),
          BuildTargetId("externalDep1"),
        ),
      )

      val targetA1Source1 = SourceItem(
        uri = "file:///project/targetA1/src/main/kotlin/",
        kind = SourceItemKind.DIRECTORY,
      )
      val targetA1Sources = SourcesItem(
        target = targetA1.id,
        sources = listOf(targetA1Source1),
      )

      val targetB1B2Source1 = SourceItem(
        uri = "file:///project/targetB/src/main/kotlin/File1.kt",
        kind = SourceItemKind.FILE,
      )
      val targetB1Sources = SourcesItem(
        target = targetB1.id,
        sources = listOf(targetB1B2Source1),
      )
      val targetB2Sources = SourcesItem(
        target = targetB2.id,
        sources = listOf(targetB1B2Source1),
      )

      val targetC1C2Source1 = SourceItem(
        uri = "file:///project/targetC/src/main/kotlin/",
        kind = SourceItemKind.DIRECTORY,
      )
      val targetC1Sources = SourcesItem(
        target = targetC1.id,
        sources = listOf(targetC1C2Source1),
      )
      val targetC2Sources = SourcesItem(
        target = targetC2.id,
        sources = listOf(targetC1C2Source1),
      )

      val targetD1D2Source1 = SourceItem(
        uri = "file:///project/targetD/src/main/kotlin/File1.kt",
        kind = SourceItemKind.FILE,
      )
      val targetD1Sources = SourcesItem(
        target = targetD1.id,
        sources = listOf(targetD1D2Source1),
      )
      val targetD2Sources = SourcesItem(
        target = targetD2.id,
        sources = listOf(targetD1D2Source1),
      )

      val projectDetails = ProjectDetails(
        targetsId = listOf(targetD1.id, targetA1.id, targetC2.id, targetB2.id, targetB1.id, targetC1.id, targetD2.id),
        targets = setOf(targetD1, targetA1, targetC2, targetB2, targetB1, targetC1, targetD2),
        sources = listOf(
          targetA1Sources,
          targetB1Sources,
          targetB2Sources,
          targetC2Sources,
          targetC1Sources,
          targetD2Sources,
          targetD1Sources,
        ),
        resources = emptyList(),
        dependenciesSources = emptyList(),
      )

      // when 1
      val magicMetaModel = MagicMetaModelImpl(testMagicMetaModelProjectConfig, projectDetails)

      // then 1
      // showing loaded and not loaded targets to user
      magicMetaModel.getAllLoadedTargets() shouldBe emptyList()
      magicMetaModel.getAllNotLoadedTargets() shouldContainExactlyInAnyOrder listOf(
        targetA1,
        targetB2,
        targetB1,
        targetC1,
        targetC2,
        targetD2,
        targetD1,
      )

      // when 2
      // after bsp importing process
      WriteCommandAction.runWriteCommandAction(project) {
        magicMetaModel.loadDefaultTargets()
      }

      // then 2
      // showing loaded and not loaded targets to user (e.g. at the sidebar)
      magicMetaModel.getAllLoadedTargets() shouldContainExactlyInAnyOrder listOf(
        targetA1,
        targetB1,
        targetC1,
        targetD1,
      )
      magicMetaModel.getAllNotLoadedTargets() shouldContainExactlyInAnyOrder listOf(
        targetB2,
        targetC2,
        targetD2,
      )

      // user opens each file and checks the loaded target for each file (e.g. at the bottom bar widget)
      magicMetaModel.getTargetsDetailsForDocument(TextDocumentId(targetA1Source1.uri)) shouldBe DocumentTargetsDetails(
        loadedTargetId = targetA1.id,
        notLoadedTargetsIds = emptyList(),
      )
      magicMetaModel.getTargetsDetailsForDocument(TextDocumentId(targetB1B2Source1.uri)) shouldBe DocumentTargetsDetails(
        loadedTargetId = targetB1.id,
        notLoadedTargetsIds = listOf(targetB2.id),
      )
      magicMetaModel.getTargetsDetailsForDocument(TextDocumentId(targetC1C2Source1.uri)) shouldBe DocumentTargetsDetails(
        loadedTargetId = targetC1.id,
        notLoadedTargetsIds = listOf(targetC2.id),
      )
      magicMetaModel.getTargetsDetailsForDocument(TextDocumentId(targetD1D2Source1.uri)) shouldBe DocumentTargetsDetails(
        loadedTargetId = targetD1.id,
        notLoadedTargetsIds = listOf(targetD2.id),
      )

      // when 3
      // ------
      // user decides to load not loaded `targetD2` by default
      // ------
      WriteCommandAction.runWriteCommandAction(project) {
        magicMetaModel.loadTarget(targetD2.id)
      }

      // then 3
      // showing loaded and not loaded targets to user (e.g. at the sidebar)
      magicMetaModel.getAllLoadedTargets() shouldContainExactlyInAnyOrder listOf(
        targetA1,
        targetB1,
        targetC1,
        targetD2,
      )
      magicMetaModel.getAllNotLoadedTargets() shouldContainExactlyInAnyOrder listOf(
        targetB2,
        targetC2,
        targetD1,
      )

      // user opens each file and checks the loaded target for each file (e.g. at the bottom bar widget)
      magicMetaModel.getTargetsDetailsForDocument(TextDocumentId(targetA1Source1.uri)) shouldBe DocumentTargetsDetails(
        loadedTargetId = targetA1.id,
        notLoadedTargetsIds = emptyList(),
      )
      magicMetaModel.getTargetsDetailsForDocument(TextDocumentId(targetB1B2Source1.uri)) shouldBe DocumentTargetsDetails(
        loadedTargetId = targetB1.id,
        notLoadedTargetsIds = listOf(targetB2.id),
      )
      magicMetaModel.getTargetsDetailsForDocument(TextDocumentId(targetC1C2Source1.uri)) shouldBe DocumentTargetsDetails(
        loadedTargetId = targetC1.id,
        notLoadedTargetsIds = listOf(targetC2.id),
      )
      magicMetaModel.getTargetsDetailsForDocument(TextDocumentId(targetD1D2Source1.uri)) shouldBe DocumentTargetsDetails(
        loadedTargetId = targetD2.id,
        notLoadedTargetsIds = listOf(targetD1.id),
      )

      // when 4
      // ------
      // well, now user decides to load not loaded `targetB2` by default
      // ------
      WriteCommandAction.runWriteCommandAction(project) {
        magicMetaModel.loadTarget(targetB2.id)
      }

      // then 4
      // showing loaded and not loaded targets to user (e.g. at the sidebar)
      magicMetaModel.getAllLoadedTargets() shouldContainExactlyInAnyOrder listOf(
        targetA1,
        targetB2,
        targetC1,
        targetD2,
      )
      magicMetaModel.getAllNotLoadedTargets() shouldContainExactlyInAnyOrder listOf(
        targetB1,
        targetC2,
        targetD1,
      )

      // user opens each file and checks the loaded target for each file (e.g. at the bottom bar widget)
      magicMetaModel.getTargetsDetailsForDocument(TextDocumentId(targetA1Source1.uri)) shouldBe DocumentTargetsDetails(
        loadedTargetId = targetA1.id,
        notLoadedTargetsIds = emptyList(),
      )
      magicMetaModel.getTargetsDetailsForDocument(TextDocumentId(targetB1B2Source1.uri)) shouldBe DocumentTargetsDetails(
        loadedTargetId = targetB2.id,
        notLoadedTargetsIds = listOf(targetB1.id),
      )
      magicMetaModel.getTargetsDetailsForDocument(TextDocumentId(targetC1C2Source1.uri)) shouldBe DocumentTargetsDetails(
        loadedTargetId = targetC1.id,
        notLoadedTargetsIds = listOf(targetC2.id),
      )
      magicMetaModel.getTargetsDetailsForDocument(TextDocumentId(targetD1D2Source1.uri)) shouldBe DocumentTargetsDetails(
        loadedTargetId = targetD2.id,
        notLoadedTargetsIds = listOf(targetD1.id),
      )

      // when 5
      // ------
      // and, finally user decides to load the default configuration
      // ------
      WriteCommandAction.runWriteCommandAction(project) {
        magicMetaModel.loadDefaultTargets()
      }

      // then 5
      // showing loaded and not loaded targets to user (e.g. at the sidebar)
      magicMetaModel.getAllLoadedTargets() shouldContainExactlyInAnyOrder listOf(
        targetA1,
        targetB1,
        targetC1,
        targetD1,
      )
      magicMetaModel.getAllNotLoadedTargets() shouldContainExactlyInAnyOrder listOf(
        targetB2,
        targetC2,
        targetD2,
      )

      // user opens each file and checks the loaded target for each file (e.g. at the bottom bar widget)
      magicMetaModel.getTargetsDetailsForDocument(TextDocumentId(targetA1Source1.uri)) shouldBe DocumentTargetsDetails(
        loadedTargetId = targetA1.id,
        notLoadedTargetsIds = emptyList(),
      )
      magicMetaModel.getTargetsDetailsForDocument(TextDocumentId(targetB1B2Source1.uri)) shouldBe DocumentTargetsDetails(
        loadedTargetId = targetB1.id,
        notLoadedTargetsIds = listOf(targetB2.id),
      )
      magicMetaModel.getTargetsDetailsForDocument(TextDocumentId(targetC1C2Source1.uri)) shouldBe DocumentTargetsDetails(
        loadedTargetId = targetC1.id,
        notLoadedTargetsIds = listOf(targetC2.id),
      )
      magicMetaModel.getTargetsDetailsForDocument(TextDocumentId(targetD1D2Source1.uri)) shouldBe DocumentTargetsDetails(
        loadedTargetId = targetD1.id,
        notLoadedTargetsIds = listOf(targetD2.id),
      )
    }
  }

  @Nested
  @DisplayName("magicMetaModelImpl.loadDefaultTargets() tests")
  inner class MagicMetaModelImplLoadDefaultTargetsTest {

    @Test
    fun `should return no loaded and no not loaded targets for empty project`() {
      // given
      val projectDetails = ProjectDetails(
        targetsId = emptyList(),
        targets = setOf(),
        sources = emptyList(),
        resources = emptyList(),
        dependenciesSources = emptyList(),
      )

      // when
      val magicMetaModel = MagicMetaModelImpl(testMagicMetaModelProjectConfig, projectDetails)
      WriteCommandAction.runWriteCommandAction(project) {
        magicMetaModel.loadDefaultTargets()
      }

      // then
      magicMetaModel.getAllLoadedTargets() shouldBe emptyList()
      magicMetaModel.getAllNotLoadedTargets() shouldBe emptyList()
    }

    @Test
    fun `should return no loaded and all targets as not loaded for not initialized project (before calling loadDefaultTargets())`() {
      // given
      val targetA1 = BuildTarget(
        id = BuildTargetId("targetA1"),
        languageIds = listOf("kotlin"),
        dependencies = listOf(
          BuildTargetId("externalDep1"),
          BuildTargetId("externalDep2"),
        ),
      )

      val targetB1 = BuildTarget(
        id = BuildTargetId("targetB1"),
        languageIds = listOf("kotlin"),
        dependencies = listOf(BuildTargetId("externalDep1")),
      )

      val targetA1Source1 = SourceItem(
        uri = "file:///project/targetA1/src/main/kotlin/File1.kt",
        kind = SourceItemKind.FILE,
      )
      val targetA1Sources = SourcesItem(
        target = targetA1.id,
        sources = listOf(targetA1Source1),
      )

      val targetB1Source1 = SourceItem(
        uri = "file:///project/targetB1/src/main/kotlin/File1.kt",
        kind = SourceItemKind.FILE,
      )
      val targetB1Sources = SourcesItem(
        target = targetB1.id,
        sources = listOf(targetB1Source1),
      )

      val projectDetails = ProjectDetails(
        targetsId = listOf(targetA1.id, targetB1.id),
        targets = setOf(targetA1, targetB1),
        sources = listOf(targetA1Sources, targetB1Sources),
        resources = emptyList(),
        dependenciesSources = emptyList(),
      )

      // when
      val magicMetaModel = MagicMetaModelImpl(testMagicMetaModelProjectConfig, projectDetails)

      // then
      magicMetaModel.getAllLoadedTargets() shouldBe emptyList()
      magicMetaModel.getAllNotLoadedTargets() shouldContainExactlyInAnyOrder listOf(targetA1, targetB1)
    }

    @Test
    fun `should return all targets as loaded and no not loaded targets for project without shared sources`() {
      // given
      val targetA1 = BuildTarget(
        id = BuildTargetId("targetA1"),
        languageIds = listOf("kotlin"),
        dependencies = listOf(
          BuildTargetId("externalDep1"),
          BuildTargetId("externalDep2"),
        ),
      )

      val targetB1 = BuildTarget(
        id = BuildTargetId("targetB1"),
        languageIds = listOf("kotlin"),
        dependencies = listOf(BuildTargetId("externalDep1")),
      )

      val targetC1 = BuildTarget(
        id = BuildTargetId("targetC1"),
        languageIds = listOf("kotlin"),
        dependencies = listOf(BuildTargetId("targetA1")),
      )

      val targetD1 = BuildTarget(
        id = BuildTargetId("targetD1"),
        languageIds = listOf("kotlin"),
        dependencies = listOf(BuildTargetId("targetC1")),
      )

      val targetA1Source1 = SourceItem(
        uri = "file:///project/targetA1/src/main/kotlin/File1.kt",
        kind = SourceItemKind.FILE,
      )
      val targetA1Sources = SourcesItem(
        target = targetA1.id,
        sources = listOf(targetA1Source1),
      )

      val targetB1Source1 = SourceItem(
        uri = "file:///project/targetB1/src/main/kotlin/",
        kind = SourceItemKind.DIRECTORY,
      )
      val targetB1Sources = SourcesItem(
        target = targetB1.id,
        sources = listOf(targetB1Source1),
      )

      val targetC1Source1 = SourceItem(
        uri = "file:///project/targetC1/src/main/kotlin/File2.kt",
        kind = SourceItemKind.FILE,
      )
      val targetC1Source2 = SourceItem(
        uri = "file:///project/targetC1/src/main/kotlin/File2.kt",
        kind = SourceItemKind.FILE,
      )
      val targetC1Sources = SourcesItem(
        target = targetC1.id,
        sources = listOf(targetC1Source1, targetC1Source2),
      )
      val targetD1Sources = SourcesItem(
        target = targetD1.id,
        sources = emptyList(),
      )

      val projectDetails = ProjectDetails(
        targetsId = listOf(targetC1.id, targetB1.id, targetA1.id, targetD1.id),
        targets = setOf(targetC1, targetB1, targetA1, targetD1),
        sources = listOf(targetA1Sources, targetB1Sources, targetC1Sources, targetD1Sources),
        resources = emptyList(),
        dependenciesSources = emptyList(),
      )

      // when
      val magicMetaModel = MagicMetaModelImpl(testMagicMetaModelProjectConfig, projectDetails)
      WriteCommandAction.runWriteCommandAction(project) {
        magicMetaModel.loadDefaultTargets()
      }

      // then
      magicMetaModel.getAllLoadedTargets() shouldContainExactlyInAnyOrder listOf(targetA1, targetB1, targetC1, targetD1)
      magicMetaModel.getAllNotLoadedTargets() shouldBe emptyList()
    }

    @Test
    fun `should return non overlapping loaded targets for project with shared sources`() {
      // given
      val targetA1 = BuildTarget(
        id = BuildTargetId("targetA1"),
        languageIds = listOf("kotlin"),
        dependencies = listOf(
          BuildTargetId("externalDep1"),
          BuildTargetId("externalDep2"),
        ),
      )

      val targetB1 = BuildTarget(
        id = BuildTargetId("targetB1"),
        languageIds = listOf("kotlin"),
        dependencies = listOf(BuildTargetId("externalDep2")),
      )

      val targetB2 = BuildTarget(
        id = BuildTargetId("targetB2"),
        languageIds = listOf("kotlin"),
        dependencies = listOf(BuildTargetId("targetA1")),
      )

      val targetA1Source1 = SourceItem(
        uri = "file:///project/targetA1src/main/kotlin/File1.kt",
        kind = SourceItemKind.FILE,
      )
      val targetA1Sources = SourcesItem(
        target = targetA1.id,
        sources = listOf(targetA1Source1),
      )

      val targetB1B2Source1 = SourceItem(
        uri = "file:///project/targetB/src/main/kotlin/File1.kt",
        kind = SourceItemKind.FILE,
      )
      val targetB1Source2 = SourceItem(
        uri = "file:///project/targetB1/src/main/kotlin/",
        kind = SourceItemKind.DIRECTORY,
      )
      val targetB1Sources = SourcesItem(
        target = targetB1.id,
        sources = listOf(targetB1Source2, targetB1B2Source1),
      )
      val targetB2Sources = SourcesItem(
        target = targetB2.id,
        sources = listOf(targetB1B2Source1),
      )

      val projectDetails = ProjectDetails(
        targetsId = listOf(targetA1.id, targetB1.id, targetB2.id),
        targets = setOf(targetA1, targetB1, targetB2),
        sources = listOf(targetA1Sources, targetB1Sources, targetB2Sources),
        resources = emptyList(),
        dependenciesSources = emptyList(),
      )

      // when
      val magicMetaModel = MagicMetaModelImpl(testMagicMetaModelProjectConfig, projectDetails)
      WriteCommandAction.runWriteCommandAction(project) {
        magicMetaModel.loadDefaultTargets()
      }

      // then
      magicMetaModel.getAllLoadedTargets() shouldContainExactlyInAnyOrder listOf(targetA1, targetB1)
      magicMetaModel.getAllNotLoadedTargets() shouldContainExactlyInAnyOrder listOf(targetB2)
    }

    @Test
    fun `should load all default targets after loading different targets (with loadTarget())`() {
      // given
      val targetA1 = BuildTarget(
        id = BuildTargetId("targetA1"),
        languageIds = listOf("kotlin"),
        dependencies = listOf(
          BuildTargetId("externalDep1"),
          BuildTargetId("externalDep2"),
        ),
      )

      val targetB1 = BuildTarget(
        id = BuildTargetId("targetB1"),
        languageIds = listOf("kotlin"),
        dependencies = listOf(BuildTargetId("externalDep2")),
      )

      val targetB2 = BuildTarget(
        id = BuildTargetId("targetB2"),
        languageIds = listOf("kotlin"),
        dependencies = listOf(BuildTargetId("targetA1")),
      )

      val targetA1Source1 = SourceItem(
        uri = "file:///project/targetA1/src/main/kotlin/File1.kt",
        kind = SourceItemKind.FILE,
      )
      val targetA1Sources = SourcesItem(
        target = targetA1.id,
        sources = listOf(targetA1Source1),
      )

      val targetB1B2Source1 = SourceItem(
        uri = "file:///project/targetB/src/main/kotlin/File1.kt",
        kind = SourceItemKind.FILE,
      )
      val targetB1Source1 = SourceItem(
        uri = "file:///project/targetB1/src/main/kotlin/",
        kind = SourceItemKind.DIRECTORY,
      )
      val targetB1Sources = SourcesItem(
        target = targetB1.id,
        sources = listOf(targetB1Source1, targetB1B2Source1),
      )
      val targetB2Sources = SourcesItem(
        target = targetB2.id,
        sources = listOf(targetB1B2Source1),
      )

      val projectDetails = ProjectDetails(
        targetsId = listOf(targetA1.id, targetB1.id, targetB2.id),
        targets = setOf(targetA1, targetB1, targetB2),
        sources = listOf(targetA1Sources, targetB1Sources, targetB2Sources),
        resources = emptyList(),
        dependenciesSources = emptyList(),
      )

      // when 1
      val magicMetaModel = MagicMetaModelImpl(testMagicMetaModelProjectConfig, projectDetails)
      WriteCommandAction.runWriteCommandAction(project) {
        magicMetaModel.loadDefaultTargets()
      }

      // then 1
      magicMetaModel.getAllLoadedTargets() shouldContainExactlyInAnyOrder listOf(targetA1, targetB1)
      magicMetaModel.getAllNotLoadedTargets() shouldContainExactlyInAnyOrder listOf(targetB2)

      // when 2
      WriteCommandAction.runWriteCommandAction(project) {
        magicMetaModel.loadTarget(targetB2.id)
      }

      // then 2
      magicMetaModel.getAllLoadedTargets() shouldContainExactlyInAnyOrder listOf(targetA1, targetB2)
      magicMetaModel.getAllNotLoadedTargets() shouldContainExactlyInAnyOrder listOf(targetB1)

      // when 3
      WriteCommandAction.runWriteCommandAction(project) {
        magicMetaModel.loadDefaultTargets()
      }

      // then 3
      magicMetaModel.getAllLoadedTargets() shouldContainExactlyInAnyOrder listOf(targetA1, targetB1)
      magicMetaModel.getAllNotLoadedTargets() shouldContainExactlyInAnyOrder listOf(targetB2)
    }
  }

  @Nested
  @DisplayName("magicMetaModelImpl.loadTarget(targetId) tests")
  inner class MagicMetaModelImplLoadTargetTest {

    @Test
    fun `should throw IllegalArgumentException for not existing target`() {
      // given
      val targetA1 = BuildTarget(
        id = BuildTargetId("targetA1"),
        languageIds = listOf("kotlin"),
        dependencies = listOf(
          BuildTargetId("externalDep1"),
          BuildTargetId("externalDep2"),
        ),
      )

      val targetA1Source1 = SourceItem(
        uri = "file:///project/targetA1/src/main/kotlin/File1.kt",
        kind = SourceItemKind.FILE,
      )
      val targetA1Sources = SourcesItem(
        target = targetA1.id,
        sources = listOf(targetA1Source1),
      )

      val projectDetails = ProjectDetails(
        targetsId = listOf(targetA1.id),
        targets = setOf(targetA1),
        sources = listOf(targetA1Sources),
        resources = emptyList(),
        dependenciesSources = emptyList(),
      )

      // when
      val magicMetaModel = MagicMetaModelImpl(testMagicMetaModelProjectConfig, projectDetails)

      val notExistingTargetId = BuildTargetId("//not/existing/target")

      // then
      val exception = shouldThrowExactly<IllegalArgumentException> { magicMetaModel.loadTarget(notExistingTargetId) }
      exception.message shouldBe "Target $notExistingTargetId is not included in the model."
    }

    @Test
    fun `should load target`() {
      // given
      val targetA1 = BuildTarget(
        id = BuildTargetId("targetA1"),
        languageIds = listOf("kotlin"),
        dependencies = listOf(
          BuildTargetId("externalDep1"),
          BuildTargetId("externalDep2"),
        ),
      )

      val targetB1 = BuildTarget(
        id = BuildTargetId("targetB1"),
        languageIds = listOf("kotlin"),
        dependencies = listOf(BuildTargetId("externalDep2")),
      )

      val targetA1Source1 = SourceItem(
        uri = "file:///project/targetA1/src/main/kotlin/File1.kt",
        kind = SourceItemKind.FILE,
      )
      val targetA1Sources = SourcesItem(
        target = targetA1.id,
        sources = listOf(targetA1Source1),
      )

      val targetB1Source1 = SourceItem(
        uri = "file:///project/targetB1/src/main/kotlin/",
        kind = SourceItemKind.DIRECTORY,
      )
      val targetB1Sources = SourcesItem(
        target = targetB1.id,
        sources = listOf(targetB1Source1),
      )

      val projectDetails = ProjectDetails(
        targetsId = listOf(targetA1.id, targetB1.id),
        targets = setOf(targetA1, targetB1),
        sources = listOf(targetA1Sources, targetB1Sources),
        resources = emptyList(),
        dependenciesSources = emptyList(),
      )

      // when
      val magicMetaModel = MagicMetaModelImpl(testMagicMetaModelProjectConfig, projectDetails)
      WriteCommandAction.runWriteCommandAction(project) {
        magicMetaModel.loadTarget(targetA1.id)
      }

      // then
      magicMetaModel.getAllLoadedTargets() shouldContainExactlyInAnyOrder listOf(targetA1)
      magicMetaModel.getAllNotLoadedTargets() shouldContainExactlyInAnyOrder listOf(targetB1)
    }

    @Test
    fun `should add already loaded target without state change`() {
      // given
      val targetA1 = BuildTarget(
        id = BuildTargetId("targetA1"),
        languageIds = listOf("kotlin"),
        dependencies = listOf(
          BuildTargetId("externalDep1"),
          BuildTargetId("externalDep2"),
        ),
      )

      val targetB1 = BuildTarget(
        id = BuildTargetId("targetB1"),
        languageIds = listOf("kotlin"),
        dependencies = listOf(BuildTargetId("externalDep2")),
      )

      val targetA1Source1 = SourceItem(
        uri = "file:///project/targetA1/src/main/kotlin/File1.kt",
        kind = SourceItemKind.FILE,
      )
      val targetA1Sources = SourcesItem(
        target = targetA1.id,
        sources = listOf(targetA1Source1),
      )

      val targetB1Source1 = SourceItem(
        uri = "file:///project/targetB1/src/main/kotlin/",
        kind = SourceItemKind.DIRECTORY,
      )
      val targetB1Sources = SourcesItem(
        target = targetB1.id,
        sources = listOf(targetB1Source1),
      )

      val projectDetails = ProjectDetails(
        targetsId = listOf(targetA1.id, targetB1.id),
        targets = setOf(targetA1, targetB1),
        sources = listOf(targetA1Sources, targetB1Sources),
        resources = emptyList(),
        dependenciesSources = emptyList(),
      )

      // when
      val magicMetaModel = MagicMetaModelImpl(testMagicMetaModelProjectConfig, projectDetails)
      WriteCommandAction.runWriteCommandAction(project) {
        magicMetaModel.loadTarget(targetA1.id)
        magicMetaModel.loadTarget(targetA1.id)
      }

      // then
      magicMetaModel.getAllLoadedTargets() shouldContainExactlyInAnyOrder listOf(targetA1)
      magicMetaModel.getAllNotLoadedTargets() shouldContainExactlyInAnyOrder listOf(targetB1)
    }

    @Test
    fun `should add targets without overlapping`() {
      // given
      val targetA1 = BuildTarget(
        id = BuildTargetId("targetA1"),
        languageIds = listOf("kotlin"),
        dependencies = listOf(
          BuildTargetId("externalDep1"),
          BuildTargetId("externalDep2"),
        ),
      )

      val targetB1 = BuildTarget(
        id = BuildTargetId("targetB1"),
        languageIds = listOf("kotlin"),
        dependencies = listOf(BuildTargetId("externalDep2")),
      )

      val targetA1Source1 = SourceItem(
        uri = "file:///project/targetA1/src/main/kotlin/File1.kt",
        kind = SourceItemKind.FILE,
      )
      val targetA1Sources = SourcesItem(
        target = targetA1.id,
        sources = listOf(targetA1Source1),
      )

      val targetB1Source1 = SourceItem(
        uri = "file:///project/targetB1/src/main/kotlin/",
        kind = SourceItemKind.DIRECTORY,
      )
      val targetB1Sources = SourcesItem(
        target = targetB1.id,
        sources = listOf(targetB1Source1),
      )

      val projectDetails = ProjectDetails(
        targetsId = listOf(targetA1.id, targetB1.id),
        targets = setOf(targetA1, targetB1),
        sources = listOf(targetA1Sources, targetB1Sources),
        resources = emptyList(),
        dependenciesSources = emptyList(),
      )

      // when
      val magicMetaModel = MagicMetaModelImpl(testMagicMetaModelProjectConfig, projectDetails)
      WriteCommandAction.runWriteCommandAction(project) {
        magicMetaModel.loadTarget(targetA1.id)
        magicMetaModel.loadTarget(targetB1.id)
      }

      // then
      magicMetaModel.getAllLoadedTargets() shouldContainExactlyInAnyOrder listOf(targetA1, targetB1)
      magicMetaModel.getAllNotLoadedTargets() shouldBe emptyList()
    }

    @Test
    fun `should add target and remove overlapping targets`() {
      // given
      val targetA1 = BuildTarget(
        id = BuildTargetId("targetA1"),
        languageIds = listOf("kotlin"),
        dependencies = listOf(
          BuildTargetId("externalDep1"),
          BuildTargetId("externalDep2"),
        ),
      )

      val targetA2 = BuildTarget(
        id = BuildTargetId("targetA2"),
        languageIds = listOf("kotlin"),
        dependencies = listOf(BuildTargetId("externalDep2")),
      )

      val targetA3 = BuildTarget(
        id = BuildTargetId("targetA3"),
        languageIds = listOf("kotlin"),
      )

      val targetA1A2Source1 = SourceItem(
        uri = "file:///project/targetA1A2/src/main/kotlin/File1.kt",
        kind = SourceItemKind.FILE,
      )
      val targetA1A3Source1 = SourceItem(
        uri = "file:///project/targetA1A3/src/main/kotlin/File1.kt",
        SourceItemKind.FILE,
        false
      )

      val targetA1Source1 = SourceItem(
        uri = "file:///project/targetA1/src/main/kotlin/File1.kt",
        kind = SourceItemKind.FILE,
      )
      val targetA1Sources = SourcesItem(
        target = targetA1.id,
        sources = listOf(targetA1Source1, targetA1A2Source1, targetA1A3Source1),
      )

      val targetA2Source1 = SourceItem(
        uri = "file:///project/targetA2/src/main/kotlin/File1.kt",
        kind = SourceItemKind.FILE,
      )
      val targetA2Sources = SourcesItem(
        target = targetA2.id,
        sources = listOf(targetA2Source1, targetA1A2Source1),
      )

      val targetA3Sources = SourcesItem(
        target = targetA3.id,
        sources = listOf(targetA1A3Source1),
      )

      val projectDetails = ProjectDetails(
        targetsId = listOf(targetA1.id, targetA2.id, targetA3.id),
        targets = setOf(targetA1, targetA2, targetA3),
        sources = listOf(targetA1Sources, targetA2Sources, targetA3Sources),
        resources = emptyList(),
        dependenciesSources = emptyList(),
      )

      // when 1
      val magicMetaModel = MagicMetaModelImpl(testMagicMetaModelProjectConfig, projectDetails)
      WriteCommandAction.runWriteCommandAction(project) {
        magicMetaModel.loadTarget(targetA2.id)
        magicMetaModel.loadTarget(targetA3.id)
      }

      // then 1
      magicMetaModel.getAllLoadedTargets() shouldContainExactlyInAnyOrder listOf(targetA2, targetA3)
      magicMetaModel.getAllNotLoadedTargets() shouldContainExactlyInAnyOrder listOf(targetA1)

      // when 2
      WriteCommandAction.runWriteCommandAction(project) {
        magicMetaModel.loadTarget(targetA1.id)
      }

      // then 2
      magicMetaModel.getAllLoadedTargets() shouldContainExactlyInAnyOrder listOf(targetA1)
      magicMetaModel.getAllNotLoadedTargets() shouldContainExactlyInAnyOrder listOf(targetA2, targetA3)
    }
  }

  @Nested
  @DisplayName("magicMetaModelImpl.getTargetsDetailsForDocument(documentId) tests")
  inner class MagicMetaModelImplGetTargetsDetailsForDocumentTest {

    @Test
    fun `should return no loaded target and no not loaded targets for not existing document`() {
      // given
      val targetA1 = BuildTarget(
        id = BuildTargetId("targetA1"),
        languageIds = listOf("kotlin"),
        dependencies = listOf(
          BuildTargetId("externalDep1"),
          BuildTargetId("externalDep2"),
        ),
      )

      val targetA1Source1 = SourceItem(
        uri = "file:///project/targetA1/src/main/kotlin/File1.kt",
        kind = SourceItemKind.FILE,
      )
      val targetA1Sources = SourcesItem(
        target = targetA1.id,
        sources = listOf(targetA1Source1),
      )

      val projectDetails = ProjectDetails(
        targetsId = listOf(targetA1.id),
        targets = setOf(targetA1),
        sources = listOf(targetA1Sources),
        resources = emptyList(),
        dependenciesSources = emptyList(),
      )

      // when
      val magicMetaModel = MagicMetaModelImpl(testMagicMetaModelProjectConfig, projectDetails)

      val documentTargetsDetails =
        magicMetaModel.getTargetsDetailsForDocument(TextDocumentId("file:///not/existing/document"))

      // then
      documentTargetsDetails shouldBe DocumentTargetsDetails(
        loadedTargetId = null,
        notLoadedTargetsIds = emptyList()
      )
    }

    @Test
    fun `should return no loaded target for model without loaded targets`() {
      // given
      val targetA1 = BuildTarget(
        id = BuildTargetId("targetA1"),
        languageIds = listOf("kotlin"),
        dependencies = listOf(
          BuildTargetId("externalDep1"),
          BuildTargetId("externalDep2"),
        ),
      )

      val targetA1Source1 = SourceItem(
        uri = "file:///project/targetA1/src/main/kotlin/File1.kt",
        kind = SourceItemKind.FILE,
      )
      val targetA1Sources = SourcesItem(
        target = targetA1.id,
        sources = listOf(targetA1Source1),
      )

      val projectDetails = ProjectDetails(
        targetsId = listOf(targetA1.id),
        targets = setOf(targetA1),
        sources = listOf(targetA1Sources),
        resources = emptyList(),
        dependenciesSources = emptyList(),
      )

      // when
      val magicMetaModel = MagicMetaModelImpl(testMagicMetaModelProjectConfig, projectDetails)

      val documentTargetsDetails = magicMetaModel.getTargetsDetailsForDocument(TextDocumentId(targetA1Source1.uri))

      // then
      documentTargetsDetails shouldBe DocumentTargetsDetails(
        loadedTargetId = null,
        notLoadedTargetsIds = listOf(targetA1.id)
      )
    }

    @Test
    fun `should return loaded target for non overlapping targets after loading default targets (all targets)`() {
      // given
      val targetA1 = BuildTarget(
        id = BuildTargetId("targetA1"),
        languageIds = listOf("kotlin"),
        dependencies = listOf(
          BuildTargetId("externalDep1"),
          BuildTargetId("externalDep2"),
        ),
      )

      val targetB1 = BuildTarget(
        id = BuildTargetId("targetB1"),
        languageIds = listOf("kotlin"),
      )

      val targetA1Source1 = SourceItem(
        uri = "file:///project/targetA1/src/main/kotlin/File1.kt",
        kind = SourceItemKind.FILE,
      )
      val targetA1Sources = SourcesItem(
        target = targetA1.id,
        sources = listOf(targetA1Source1),
      )

      val targetB1Source1 = SourceItem(
        uri = "file:///project/targetB1/src/main/kotlin/File1.kt",
        kind = SourceItemKind.FILE,
      )
      val targetB1Sources = SourcesItem(
        target = targetB1.id,
        sources = listOf(targetB1Source1),
      )

      val projectDetails = ProjectDetails(
        targetsId = listOf(targetA1.id, targetB1.id),
        targets = setOf(targetA1, targetB1),
        sources = listOf(targetA1Sources, targetB1Sources),
        resources = emptyList(),
        dependenciesSources = emptyList(),
      )

      // when
      val magicMetaModel = MagicMetaModelImpl(testMagicMetaModelProjectConfig, projectDetails)
      WriteCommandAction.runWriteCommandAction(project) {
        magicMetaModel.loadDefaultTargets()
      }

      // then
      magicMetaModel.getTargetsDetailsForDocument(TextDocumentId(targetA1Source1.uri)) shouldBe DocumentTargetsDetails(
        loadedTargetId = targetA1.id,
        notLoadedTargetsIds = emptyList()
      )
      magicMetaModel.getTargetsDetailsForDocument(TextDocumentId(targetB1Source1.uri)) shouldBe DocumentTargetsDetails(
        loadedTargetId = targetB1.id,
        notLoadedTargetsIds = emptyList()
      )
    }

    @Test
    fun `should return loaded target for source in loaded target and no loaded target for source in not loaded target for model with overlapping targets`() {
      // given
      val targetA1 = BuildTarget(
        id = BuildTargetId("targetA1"),
        languageIds = listOf("kotlin"),
        dependencies = listOf(
          BuildTargetId("externalDep1"),
          BuildTargetId("externalDep2"),
        ),
      )

      val targetA2 = BuildTarget(
        id = BuildTargetId("targetA2"),
        languageIds = listOf("kotlin"),
      )

      val targetA1A2Source1 = SourceItem(
        uri = "file:///project/targetA/src/main/kotlin/File1.kt",
        kind = SourceItemKind.FILE,
      )
      val targetA1Sources = SourcesItem(
        target = targetA1.id,
        sources = listOf(targetA1A2Source1),
      )

      val targetA2Source1 = SourceItem(
        uri = "file:///project/targetA2/src/main/kotlin/",
        kind = SourceItemKind.DIRECTORY,
      )
      val targetA2Sources = SourcesItem(
        target = targetA2.id,
        sources = listOf(targetA2Source1, targetA1A2Source1),
      )

      val projectDetails = ProjectDetails(
        targetsId = listOf(targetA1.id, targetA2.id),
        targets = setOf(targetA1, targetA2),
        sources = listOf(targetA1Sources, targetA2Sources),
        resources = emptyList(),
        dependenciesSources = emptyList(),
      )

      // when
      val magicMetaModel = MagicMetaModelImpl(testMagicMetaModelProjectConfig, projectDetails)
      WriteCommandAction.runWriteCommandAction(project) {
        magicMetaModel.loadTarget(targetA1.id)
      }

      // then
      magicMetaModel.getTargetsDetailsForDocument(TextDocumentId(targetA1A2Source1.uri)) shouldBe DocumentTargetsDetails(
        loadedTargetId = targetA1.id,
        notLoadedTargetsIds = listOf(targetA2.id)
      )

      magicMetaModel.getTargetsDetailsForDocument(TextDocumentId(targetA2Source1.uri)) shouldBe DocumentTargetsDetails(
        loadedTargetId = null,
        notLoadedTargetsIds = listOf(targetA2.id)
      )
    }
  }
}
