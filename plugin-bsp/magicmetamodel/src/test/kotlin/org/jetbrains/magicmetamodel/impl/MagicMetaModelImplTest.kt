package org.jetbrains.magicmetamodel.impl

import ch.epfl.scala.bsp4j.SourceItemKind
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import org.jetbrains.magicmetamodel.DocumentTargetsDetails
import org.jetbrains.magicmetamodel.MagicMetaModelProjectConfig
import org.jetbrains.magicmetamodel.ProjectDetails
import org.jetbrains.magicmetamodel.impl.workspacemodel.BuildTargetInfo
import org.jetbrains.magicmetamodel.impl.workspacemodel.ModuleCapabilities
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
import java.nio.file.Path
import kotlin.io.path.createTempDirectory

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
      MagicMetaModelProjectConfig(workspaceModel, virtualFileUrlManager, null, projectBasePath, project)
  }

  private fun createBuildTargetInfo(target: BuildTarget) = with(target) {
    BuildTargetInfo(
      id = id.uri,
      displayName = displayName,
      dependencies = dependencies.map { it.uri },
      capabilities = with(capabilities) {
        ModuleCapabilities(
          canRun == true,
          canTest == true,
          canCompile == true,
          canDebug == true
        )
      },
      languageIds = languageIds,
    )
  }

  @Nested
  @DisplayName("MagicMetaModelImpl 'real world' flow tests")
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
        javacOptions = emptyList(),
        pythonOptions = emptyList(),
        outputPathUris = emptyList(),
        libraries = emptyList(),
        scalacOptions = emptyList(),
      )

      // when 1
      val magicMetaModel = MagicMetaModelImpl(testMagicMetaModelProjectConfig, projectDetails)

      // then 1
      // showing loaded and not loaded targets to user (e.g. at the sidebar)
      magicMetaModel.getAllLoadedTargets() shouldBe emptyList()
      magicMetaModel.getAllNotLoadedTargets() shouldBe emptyList()

      // when 2
      val diff = magicMetaModel.loadDefaultTargets()

      // then 2
      // showing loaded and not loaded targets to user (e.g. at the sidebar)
      runBlocking { diff.applyOnWorkspaceModel() }

      magicMetaModel.getAllLoadedTargets() shouldBe emptyList()
      magicMetaModel.getAllNotLoadedTargets() shouldBe emptyList()
    }

    @Test
    fun `should handle project without shared sources (like a simple kotlin project)`() {
      // given
      val projectRoot = createTempDirectory("root")
      projectRoot.toFile().deleteOnExit()

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

      val targetA1Package1 = createTempDirectory(projectRoot, "targetA1")
      targetA1Package1.toFile().deleteOnExit()
      val targetA1Package2 = createTempDirectory(targetA1Package1, "package")
      targetA1Package2.toFile().deleteOnExit()

      val targetA1Source1 = SourceItem(
        uri = targetA1Package2.toUri().toString(),
        kind = SourceItemKind.DIRECTORY,
      )
      val targetA1Sources = SourcesItem(
        target = targetA1.id,
        sources = listOf(targetA1Source1),
      )

      val targetB1Package1 = createTempDirectory(projectRoot, "targetB1")
      targetB1Package1.toFile().deleteOnExit()
      val targetB1Package2 = createTempDirectory(targetB1Package1, "package")
      targetB1Package2.toFile().deleteOnExit()
      val targetB1File = kotlin.io.path.createTempFile(targetB1Package2, "File1", ".kt")
      targetB1File.toFile().deleteOnExit()

      val targetB1Source1 = SourceItem(
        uri = targetB1File.toUri().toString(),
        kind = SourceItemKind.FILE,
      )
      val targetB1Sources = SourcesItem(
        target = targetB1.id,
        sources = listOf(targetB1Source1),
      )

      val targetC1Package1 = createTempDirectory(projectRoot, "targetC1")
      targetC1Package1.toFile().deleteOnExit()
      val targetC1Package2 = createTempDirectory(targetC1Package1, "package")
      targetC1Package2.toFile().deleteOnExit()

      val targetC1Source1 = SourceItem(
        uri = targetC1Package2.toUri().toString(),
        kind = SourceItemKind.DIRECTORY,
      )
      val targetC1Sources = SourcesItem(
        target = targetC1.id,
        sources = listOf(targetC1Source1),
      )

      val targetD1Package1 = createTempDirectory(projectRoot, "targetD1")
      targetD1Package1.toFile().deleteOnExit()
      val targetD1Package2 = createTempDirectory(targetD1Package1, "package")
      targetD1Package2.toFile().deleteOnExit()
      val targetD1File = kotlin.io.path.createTempFile(targetD1Package2, "File1", ".kt")
      targetD1File.toFile().deleteOnExit()

      val targetD1Source1 = SourceItem(
        uri = targetD1File.toUri().toString(),
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
        javacOptions = emptyList(),
        pythonOptions = emptyList(),
        outputPathUris = emptyList(),
        libraries = emptyList(),
        scalacOptions = emptyList(),
      )

      // when 1
      val magicMetaModel = MagicMetaModelImpl(testMagicMetaModelProjectConfig, projectDetails)

      // then 1
      val expectedTargetA1 = createBuildTargetInfo(targetA1)
      val expectedTargetB1 = createBuildTargetInfo(targetB1)
      val expectedTargetC1 = createBuildTargetInfo(targetC1)
      val expectedTargetD1 = createBuildTargetInfo(targetD1)
      // showing loaded and not loaded targets to user (e.g. at the sidebar)
      magicMetaModel.getAllLoadedTargets() shouldBe emptyList()
      magicMetaModel.getAllNotLoadedTargets() shouldContainExactlyInAnyOrder listOf(
        expectedTargetA1,
        expectedTargetB1,
        expectedTargetC1,
        expectedTargetD1,
      )

      // user opens each file and checks the loaded target for each file (e.g. at the bottom bar widget)
      magicMetaModel.getTargetsDetailsForDocument(TextDocumentId(targetA1Source1.uri)) shouldBe DocumentTargetsDetails(
        loadedTargetId = null,
        notLoadedTargetsIds = listOf(targetA1.id.uri),
      )
      magicMetaModel.getTargetsDetailsForDocument(TextDocumentId(targetB1Source1.uri)) shouldBe DocumentTargetsDetails(
        loadedTargetId = null,
        notLoadedTargetsIds = listOf(targetB1.id.uri),
      )
      magicMetaModel.getTargetsDetailsForDocument(TextDocumentId(targetC1Source1.uri)) shouldBe DocumentTargetsDetails(
        loadedTargetId = null,
        notLoadedTargetsIds = listOf(targetC1.id.uri),
      )
      magicMetaModel.getTargetsDetailsForDocument(TextDocumentId(targetD1Source1.uri)) shouldBe DocumentTargetsDetails(
        loadedTargetId = null,
        notLoadedTargetsIds = listOf(targetD1.id.uri),
      )

      // when 2
      // after bsp importing process
      val diff = magicMetaModel.loadDefaultTargets()

      // then 2
      // showing loaded and not loaded targets to user (e.g. at the sidebar)
      runBlocking { diff.applyOnWorkspaceModel() }

      magicMetaModel.getAllLoadedTargets() shouldContainExactlyInAnyOrder listOf(
        expectedTargetA1,
        expectedTargetB1,
        expectedTargetC1,
        expectedTargetD1,
      )
      magicMetaModel.getAllNotLoadedTargets() shouldBe emptyList()

      // user opens each file and checks the loaded target for each file (e.g. at the bottom bar widget)
      magicMetaModel.getTargetsDetailsForDocument(TextDocumentId(targetA1Source1.uri)) shouldBe DocumentTargetsDetails(
        loadedTargetId = targetA1.id.uri,
        notLoadedTargetsIds = emptyList(),
      )
      magicMetaModel.getTargetsDetailsForDocument(TextDocumentId(targetB1Source1.uri)) shouldBe DocumentTargetsDetails(
        loadedTargetId = targetB1.id.uri,
        notLoadedTargetsIds = emptyList(),
      )
      magicMetaModel.getTargetsDetailsForDocument(TextDocumentId(targetC1Source1.uri)) shouldBe DocumentTargetsDetails(
        loadedTargetId = targetC1.id.uri,
        notLoadedTargetsIds = emptyList(),
      )
      magicMetaModel.getTargetsDetailsForDocument(TextDocumentId(targetD1Source1.uri)) shouldBe DocumentTargetsDetails(
        loadedTargetId = targetD1.id.uri,
        notLoadedTargetsIds = emptyList(),
      )
    }

    @Test
    fun `should handle project with shared sources (like a scala cross version project)`() {
      // given
      val projectRoot = createTempDirectory("root")
      projectRoot.toFile().deleteOnExit()

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

      val targetA1Package1 = createTempDirectory(projectRoot, "targetA1")
      targetA1Package1.toFile().deleteOnExit()
      val targetA1Package2 = createTempDirectory(targetA1Package1, "package")
      targetA1Package2.toFile().deleteOnExit()

      val targetA1Source1 = SourceItem(
        uri = targetA1Package2.toUri().toString(),
        kind = SourceItemKind.DIRECTORY,
      )
      val targetA1Sources = SourcesItem(
        target = targetA1.id,
        sources = listOf(targetA1Source1),
      )

      val targetB1B2Package1 = createTempDirectory(projectRoot, "targetB1B2")
      targetB1B2Package1.toFile().deleteOnExit()
      val targetB1B2Package2 = createTempDirectory(targetB1B2Package1, "package")
      targetB1B2Package2.toFile().deleteOnExit()
      val targetB1B2File = kotlin.io.path.createTempFile(targetB1B2Package2, "File1", ".kt")
      targetB1B2File.toFile().deleteOnExit()

      val targetB1B2Source1 = SourceItem(
        uri = targetB1B2File.toUri().toString(),
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

      val targetC1C2Package1 = createTempDirectory(projectRoot, "targetC1C2")
      targetC1C2Package1.toFile().deleteOnExit()
      val targetC1C2Package2 = createTempDirectory(targetC1C2Package1, "package")
      targetC1C2Package2.toFile().deleteOnExit()

      val targetC1C2Source1 = SourceItem(
        uri = targetC1C2Package2.toUri().toString(),
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

      val targetD1D2Package1 = createTempDirectory(projectRoot, "targetD1D2")
      targetD1D2Package1.toFile().deleteOnExit()
      val targetD1D2Package2 = createTempDirectory(targetD1D2Package1, "package")
      targetD1D2Package2.toFile().deleteOnExit()
      val targetD1D2File = kotlin.io.path.createTempFile(targetD1D2Package2, "File1", ".kt")
      targetD1D2File.toFile().deleteOnExit()

      val targetD1D2Source1 = SourceItem(
        uri = targetD1D2File.toUri().toString(),
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
        targetsId = listOf(
          targetD1.id,
          targetA1.id,
          targetC2.id,
          targetB2.id,
          targetB1.id,
          targetC1.id,
          targetD2.id,
        ),
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
        javacOptions = emptyList(),
        pythonOptions = emptyList(),
        outputPathUris = emptyList(),
        libraries = emptyList(),
        scalacOptions = emptyList(),
      )

      // when 1
      val magicMetaModel = MagicMetaModelImpl(testMagicMetaModelProjectConfig, projectDetails)

      // then 1
      val expectedTargetA1 = createBuildTargetInfo(targetA1)
      val expectedTargetB1 = createBuildTargetInfo(targetB1)
      val expectedTargetB2 = createBuildTargetInfo(targetB2)
      val expectedTargetC1 = createBuildTargetInfo(targetC1)
      val expectedTargetC2 = createBuildTargetInfo(targetC2)
      val expectedTargetD1 = createBuildTargetInfo(targetD1)
      val expectedTargetD2 = createBuildTargetInfo(targetD2)
      // showing loaded and not loaded targets to user
      magicMetaModel.getAllLoadedTargets() shouldBe emptyList()
      magicMetaModel.getAllNotLoadedTargets() shouldContainExactlyInAnyOrder listOf(
        expectedTargetA1,
        expectedTargetB2,
        expectedTargetB1,
        expectedTargetC1,
        expectedTargetC2,
        expectedTargetD2,
        expectedTargetD1,
      )

      // when 2
      // after bsp importing process
      val diff2 = magicMetaModel.loadDefaultTargets()

      // then 2
      // showing loaded and not loaded targets to user (e.g. at the sidebar)
      runBlocking { diff2.applyOnWorkspaceModel() }

      magicMetaModel.getAllLoadedTargets() shouldContainExactlyInAnyOrder listOf(
        expectedTargetA1,
        expectedTargetB1,
        expectedTargetC2,
        expectedTargetD1,
      )
      magicMetaModel.getAllNotLoadedTargets() shouldContainExactlyInAnyOrder listOf(
        expectedTargetB2,
        expectedTargetC1,
        expectedTargetD2,
      )

      // user opens each file and checks the loaded target for each file (e.g. at the bottom bar widget)
      magicMetaModel.getTargetsDetailsForDocument(TextDocumentId(targetA1Source1.uri)) shouldBe DocumentTargetsDetails(
        loadedTargetId = targetA1.id.uri,
        notLoadedTargetsIds = emptyList(),
      )
      magicMetaModel.getTargetsDetailsForDocument(TextDocumentId(targetB1B2Source1.uri)) shouldBe DocumentTargetsDetails(
        loadedTargetId = targetB1.id.uri,
        notLoadedTargetsIds = listOf(targetB2.id.uri),
      )
      magicMetaModel.getTargetsDetailsForDocument(TextDocumentId(targetC1C2Source1.uri)) shouldBe DocumentTargetsDetails(
        loadedTargetId = targetC2.id.uri,
        notLoadedTargetsIds = listOf(targetC1.id.uri),
      )
      magicMetaModel.getTargetsDetailsForDocument(TextDocumentId(targetD1D2Source1.uri)) shouldBe DocumentTargetsDetails(
        loadedTargetId = targetD1.id.uri,
        notLoadedTargetsIds = listOf(targetD2.id.uri),
      )

      // when 3
      // ------
      // user decides to load not loaded `targetD2` by default
      // ------
      val diff3 = magicMetaModel.loadTarget(targetD2.id.uri)!!

      // then 3
      // showing loaded and not loaded targets to user (e.g. at the sidebar)
      runBlocking { diff3.applyOnWorkspaceModel() }

      magicMetaModel.getAllLoadedTargets() shouldContainExactlyInAnyOrder listOf(
        expectedTargetA1,
        expectedTargetB1,
        expectedTargetC2,
        expectedTargetD2,
      )
      magicMetaModel.getAllNotLoadedTargets() shouldContainExactlyInAnyOrder listOf(
        expectedTargetB2,
        expectedTargetC1,
        expectedTargetD1,
      )

      // user opens each file and checks the loaded target for each file (e.g. at the bottom bar widget)
      magicMetaModel.getTargetsDetailsForDocument(TextDocumentId(targetA1Source1.uri)) shouldBe DocumentTargetsDetails(
        loadedTargetId = targetA1.id.uri,
        notLoadedTargetsIds = emptyList(),
      )
      magicMetaModel.getTargetsDetailsForDocument(TextDocumentId(targetB1B2Source1.uri)) shouldBe DocumentTargetsDetails(
        loadedTargetId = targetB1.id.uri,
        notLoadedTargetsIds = listOf(targetB2.id.uri),
      )
      magicMetaModel.getTargetsDetailsForDocument(TextDocumentId(targetC1C2Source1.uri)) shouldBe DocumentTargetsDetails(
        loadedTargetId = targetC2.id.uri,
        notLoadedTargetsIds = listOf(targetC1.id.uri),
      )
      magicMetaModel.getTargetsDetailsForDocument(TextDocumentId(targetD1D2Source1.uri)) shouldBe DocumentTargetsDetails(
        loadedTargetId = targetD2.id.uri,
        notLoadedTargetsIds = listOf(targetD1.id.uri),
      )

      // when 4
      // ------
      // well, now user decides to load not loaded `targetB2` by default
      // ------
      val diff4 = magicMetaModel.loadTarget(targetB2.id.uri)!!

      // then 4
      // showing loaded and not loaded targets to user (e.g. at the sidebar)
      runBlocking { diff4.applyOnWorkspaceModel() }

      magicMetaModel.getAllLoadedTargets() shouldContainExactlyInAnyOrder listOf(
        expectedTargetA1,
        expectedTargetB2,
        expectedTargetC2,
        expectedTargetD2,
      )
      magicMetaModel.getAllNotLoadedTargets() shouldContainExactlyInAnyOrder listOf(
        expectedTargetB1,
        expectedTargetC1,
        expectedTargetD1,
      )

      // user opens each file and checks the loaded target for each file (e.g. at the bottom bar widget)
      magicMetaModel.getTargetsDetailsForDocument(TextDocumentId(targetA1Source1.uri)) shouldBe DocumentTargetsDetails(
        loadedTargetId = targetA1.id.uri,
        notLoadedTargetsIds = emptyList(),
      )
      magicMetaModel.getTargetsDetailsForDocument(TextDocumentId(targetB1B2Source1.uri)) shouldBe DocumentTargetsDetails(
        loadedTargetId = targetB2.id.uri,
        notLoadedTargetsIds = listOf(targetB1.id.uri),
      )
      magicMetaModel.getTargetsDetailsForDocument(TextDocumentId(targetC1C2Source1.uri)) shouldBe DocumentTargetsDetails(
        loadedTargetId = targetC2.id.uri,
        notLoadedTargetsIds = listOf(targetC1.id.uri),
      )
      magicMetaModel.getTargetsDetailsForDocument(TextDocumentId(targetD1D2Source1.uri)) shouldBe DocumentTargetsDetails(
        loadedTargetId = targetD2.id.uri,
        notLoadedTargetsIds = listOf(targetD1.id.uri),
      )

      // when 5
      // ------
      // and, finally user decides to load the default configuration
      // ------
      val diff5 = magicMetaModel.loadDefaultTargets()

      // then 5
      // showing loaded and not loaded targets to user (e.g. at the sidebar)
      runBlocking { diff5.applyOnWorkspaceModel() }

      magicMetaModel.getAllLoadedTargets() shouldContainExactlyInAnyOrder listOf(
        expectedTargetA1,
        expectedTargetB1,
        expectedTargetC2,
        expectedTargetD1,
      )
      magicMetaModel.getAllNotLoadedTargets() shouldContainExactlyInAnyOrder listOf(
        expectedTargetB2,
        expectedTargetC1,
        expectedTargetD2,
      )

      // user opens each file and checks the loaded target for each file (e.g. at the bottom bar widget)
      magicMetaModel.getTargetsDetailsForDocument(TextDocumentId(targetA1Source1.uri)) shouldBe DocumentTargetsDetails(
        loadedTargetId = targetA1.id.uri,
        notLoadedTargetsIds = emptyList(),
      )
      magicMetaModel.getTargetsDetailsForDocument(TextDocumentId(targetB1B2Source1.uri)) shouldBe DocumentTargetsDetails(
        loadedTargetId = targetB1.id.uri,
        notLoadedTargetsIds = listOf(targetB2.id.uri),
      )
      magicMetaModel.getTargetsDetailsForDocument(TextDocumentId(targetC1C2Source1.uri)) shouldBe DocumentTargetsDetails(
        loadedTargetId = targetC2.id.uri,
        notLoadedTargetsIds = listOf(targetC1.id.uri),
      )
      magicMetaModel.getTargetsDetailsForDocument(TextDocumentId(targetD1D2Source1.uri)) shouldBe DocumentTargetsDetails(
        loadedTargetId = targetD1.id.uri,
        notLoadedTargetsIds = listOf(targetD2.id.uri),
      )
    }
  }

  @Test
  fun `should correctly load and unload targets with dependencies`() {
    // given
    val projectRoot = createTempDirectory("root")
    projectRoot.toFile().deleteOnExit()

    val createTarget = fun(name: String, deps: List<String>) =
      BuildTarget(
        id = BuildTargetId(name),
        languageIds = listOf("kotlin"),
        dependencies = deps.map { BuildTargetId(it) }
      )
    val tempDir = fun(prefix: String): Path {
      val rootPackage = createTempDirectory(projectRoot, prefix)
      rootPackage.toFile().deleteOnExit()
      val targetPackage = createTempDirectory(rootPackage, "package")
      targetPackage.toFile().deleteOnExit()
      return targetPackage
    }
    val tempFile = fun(prefix: String): Path {
      val path = tempDir(prefix)
      val file = kotlin.io.path.createTempFile(path, prefix, ".kt")
      file.toFile().deleteOnExit()
      return file
    }
    val sourceFile = fun(name: String) = SourceItem(
      uri = tempFile(name).toUri().toString(),
      kind = SourceItemKind.FILE
    )
    val sources = fun(target: BuildTarget, source: SourceItem) = SourcesItem(
      target = target.id,
      sources = listOf(source)
    )
    val sourcesWithList = fun(target: BuildTarget, sources: List<SourceItem>) = SourcesItem(
      target = target.id,
      sources = sources
    )

    val targetA1 = createTarget("A1", listOf("B1", "C2", "external1"))
    val targetB1 = createTarget("B1", listOf("C1", "external1"))
    val targetC1 = createTarget("C1", listOf("D1", "external2"))
    val targetD1 = createTarget("D1", listOf("external2"))

    val targetA2 = createTarget("A2", listOf("B2", "C1", "external2"))
    val targetB2 = createTarget("B2", listOf("C2", "external2"))
    val targetC2 = createTarget("C2", listOf("external2"))

    val a1a2SourceFile = sourceFile("targetA1")
    val b1b2SourceFile = sourceFile("targetB1B2")
    val c1c2SourceFile = sourceFile("targetC1C2")
    val d1A2SourceFile = sourceFile("targetD1A2")
    val d1B2SourceFile = sourceFile("targetD1B2")

    val a1Sources = sources(targetA1, a1a2SourceFile)
    val a2Sources = sourcesWithList(targetA2, listOf(a1a2SourceFile, d1A2SourceFile))
    val b1Sources = sources(targetB1, b1b2SourceFile)
    val b2Sources = sourcesWithList(targetB2, listOf(b1b2SourceFile, d1B2SourceFile))
    val c1Sources = sources(targetC1, c1c2SourceFile)
    val c2Sources = sources(targetC2, c1c2SourceFile)
    val d1Sources = sourcesWithList(targetD1, listOf(d1A2SourceFile, d1B2SourceFile))

    val projectDetails = ProjectDetails(
      targetsId = listOf(
        targetA1.id,
        targetB1.id,
        targetC1.id,
        targetD1.id,
        targetA2.id,
        targetB2.id,
        targetC2.id,
      ),
      targets = setOf(
        targetA1,
        targetB1,
        targetC1,
        targetD1,
        targetA2,
        targetB2,
        targetC2,
      ),
      sources = listOf(
        a1Sources,
        a2Sources,
        b1Sources,
        b2Sources,
        c1Sources,
        c2Sources,
        d1Sources,
      ),
      resources = emptyList(),
      dependenciesSources = emptyList(),
      javacOptions = emptyList(),
      pythonOptions = emptyList(),
      outputPathUris = emptyList(),
      libraries = emptyList(),
      scalacOptions = emptyList(),
    )

    val expectedTargetA1 = createBuildTargetInfo(targetA1)
    val expectedTargetA2 = createBuildTargetInfo(targetA2)
    val expectedTargetB1 = createBuildTargetInfo(targetB1)
    val expectedTargetB2 = createBuildTargetInfo(targetB2)
    val expectedTargetC1 = createBuildTargetInfo(targetC1)
    val expectedTargetD1 = createBuildTargetInfo(targetD1)

    // when
    val magicMetaModel = MagicMetaModelImpl(testMagicMetaModelProjectConfig, projectDetails)
    val diff = magicMetaModel.loadDefaultTargets()
    runBlocking { diff.applyOnWorkspaceModel() }

    magicMetaModel.getAllLoadedTargets() shouldContainExactlyInAnyOrder listOf(
      expectedTargetA1, expectedTargetB1, expectedTargetC1, expectedTargetD1)

    val diff2 = magicMetaModel.loadTargetWithDependencies(targetA2.id.uri)

    runBlocking { diff2?.applyOnWorkspaceModel() }

    // then
    magicMetaModel.getAllLoadedTargets() shouldContainExactlyInAnyOrder listOf(
      expectedTargetA2, expectedTargetB2, expectedTargetC1,
    )
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
        javacOptions = emptyList(),
        pythonOptions = emptyList(),
        outputPathUris = emptyList(),
        libraries = emptyList(),
        scalacOptions = emptyList(),
      )

      // when
      val magicMetaModel = MagicMetaModelImpl(testMagicMetaModelProjectConfig, projectDetails)
      val diff = magicMetaModel.loadDefaultTargets()

      // then
      runBlocking { diff.applyOnWorkspaceModel() }

      magicMetaModel.getAllLoadedTargets() shouldBe emptyList()
      magicMetaModel.getAllNotLoadedTargets() shouldBe emptyList()
    }

    @Test
    fun `should return no loaded and all targets as not loaded for not initialized project (before calling loadDefaultTargets())`() {
      // given
      val projectRoot = createTempDirectory("root")
      projectRoot.toFile().deleteOnExit()

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

      val targetA1Package1 = createTempDirectory(projectRoot, "targetA1")
      targetA1Package1.toFile().deleteOnExit()
      val targetA1Package2 = createTempDirectory(targetA1Package1, "package")
      targetA1Package2.toFile().deleteOnExit()
      val targetA1File1 = kotlin.io.path.createTempFile(targetA1Package2, "File1", ".kt")
      targetA1File1.toFile().deleteOnExit()

      val targetA1Source1 = SourceItem(
        uri = targetA1File1.toUri().toString(),
        kind = SourceItemKind.FILE,
      )
      val targetA1Sources = SourcesItem(
        target = targetA1.id,
        sources = listOf(targetA1Source1),
      )

      val targetB1Package1 = createTempDirectory(projectRoot, "targetB1")
      targetB1Package1.toFile().deleteOnExit()
      val targetB1Package2 = createTempDirectory(targetB1Package1, "package")
      targetB1Package2.toFile().deleteOnExit()
      val targetB1File1 = kotlin.io.path.createTempFile(targetB1Package2, "File1", ".kt")
      targetB1File1.toFile().deleteOnExit()

      val targetB1Source1 = SourceItem(
        uri = targetB1File1.toUri().toString(),
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
        javacOptions = emptyList(),
        pythonOptions = emptyList(),
        outputPathUris = emptyList(),
        libraries = emptyList(),
        scalacOptions = emptyList(),
      )

      // when
      val magicMetaModel = MagicMetaModelImpl(testMagicMetaModelProjectConfig, projectDetails)

      // then
      val expectedTargetA1 = createBuildTargetInfo(targetA1)
      val expectedTargetB1 = createBuildTargetInfo(targetB1)
      magicMetaModel.getAllLoadedTargets() shouldBe emptyList()
      magicMetaModel.getAllNotLoadedTargets() shouldContainExactlyInAnyOrder listOf(expectedTargetA1, expectedTargetB1)
    }

    @Test
    fun `should return all targets as loaded and no not loaded targets for project without shared sources`() {
      // given
      val projectRoot = createTempDirectory("root")
      projectRoot.toFile().deleteOnExit()

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

      val targetA1Package1 = createTempDirectory(projectRoot, "targetA1")
      targetA1Package1.toFile().deleteOnExit()
      val targetA1Package2 = createTempDirectory(targetA1Package1, "package")
      targetA1Package2.toFile().deleteOnExit()
      val targetA1File1 = kotlin.io.path.createTempFile(targetA1Package2, "File1", ".kt")
      targetA1File1.toFile().deleteOnExit()

      val targetA1Source1 = SourceItem(
        uri = targetA1File1.toUri().toString(),
        kind = SourceItemKind.FILE,
      )
      val targetA1Sources = SourcesItem(
        target = targetA1.id,
        sources = listOf(targetA1Source1),
      )

      val targetB1Package1 = createTempDirectory(projectRoot, "targetB1")
      targetB1Package1.toFile().deleteOnExit()
      val targetB1Package2 = createTempDirectory(targetB1Package1, "package")
      targetB1Package2.toFile().deleteOnExit()

      val targetB1Source1 = SourceItem(
        uri = targetB1Package2.toUri().toString(),
        kind = SourceItemKind.DIRECTORY,
      )
      val targetB1Sources = SourcesItem(
        target = targetB1.id,
        sources = listOf(targetB1Source1),
      )

      val targetC1Package1 = createTempDirectory(projectRoot, "targetC1")
      targetC1Package1.toFile().deleteOnExit()
      val targetC1Package2 = createTempDirectory(targetC1Package1, "package")
      targetC1Package2.toFile().deleteOnExit()
      val targetC1File1 = kotlin.io.path.createTempFile(targetC1Package2, "File1", ".kt")
      targetC1File1.toFile().deleteOnExit()
      val targetC1File2 = kotlin.io.path.createTempFile(targetC1Package2, "File2", ".kt")
      targetC1File2.toFile().deleteOnExit()

      val targetC1Source1 = SourceItem(
        uri = targetC1File1.toUri().toString(),
        kind = SourceItemKind.FILE,
      )
      val targetC1Source2 = SourceItem(
        uri = targetC1File2.toUri().toString(),
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
        javacOptions = emptyList(),
        pythonOptions = emptyList(),
        outputPathUris = emptyList(),
        libraries = emptyList(),
        scalacOptions = emptyList(),
      )

      // when
      val magicMetaModel = MagicMetaModelImpl(testMagicMetaModelProjectConfig, projectDetails)
      val diff = magicMetaModel.loadDefaultTargets()

      // then
      val expectedTargetA1 = createBuildTargetInfo(targetA1)
      val expectedTargetB1 = createBuildTargetInfo(targetB1)
      val expectedTargetC1 = createBuildTargetInfo(targetC1)
      val expectedTargetD1 = createBuildTargetInfo(targetD1)
      runBlocking { diff.applyOnWorkspaceModel() }

      magicMetaModel.getAllLoadedTargets() shouldContainExactlyInAnyOrder listOf(
        expectedTargetA1, expectedTargetB1, expectedTargetC1, expectedTargetD1,
      )
      magicMetaModel.getAllNotLoadedTargets() shouldBe emptyList()
    }

    @Test
    fun `should return non overlapping loaded targets for project with shared sources`() {
      // given
      val projectRoot = createTempDirectory("root")
      projectRoot.toFile().deleteOnExit()

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

      val targetA1Package1 = createTempDirectory(projectRoot, "targetA1")
      targetA1Package1.toFile().deleteOnExit()
      val targetA1Package2 = createTempDirectory(targetA1Package1, "package")
      targetA1Package2.toFile().deleteOnExit()
      val targetA1File1 = kotlin.io.path.createTempFile(targetA1Package2, "File1", ".kt")
      targetA1File1.toFile().deleteOnExit()

      val targetA1Source1 = SourceItem(
        uri = targetA1File1.toUri().toString(),
        kind = SourceItemKind.FILE,
      )
      val targetA1Sources = SourcesItem(
        target = targetA1.id,
        sources = listOf(targetA1Source1),
      )

      val targetB1B2Package1 = createTempDirectory(projectRoot, "targetB1B2")
      targetB1B2Package1.toFile().deleteOnExit()
      val targetB1B2Package2 = createTempDirectory(targetB1B2Package1, "package")
      targetB1B2Package2.toFile().deleteOnExit()
      val targetB1B2File1 = kotlin.io.path.createTempFile(targetB1B2Package2, "File1", ".kt")
      targetB1B2File1.toFile().deleteOnExit()

      val targetB1B2Source1 = SourceItem(
        uri = targetB1B2File1.toUri().toString(),
        kind = SourceItemKind.FILE,
      )

      val targetB1Package1 = createTempDirectory(projectRoot, "targetB1")
      targetB1Package1.toFile().deleteOnExit()
      val targetB1Package2 = createTempDirectory(targetB1Package1, "package")
      targetB1Package2.toFile().deleteOnExit()

      val targetB1Source2 = SourceItem(
        uri = targetB1Package2.toUri().toString(),
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
        javacOptions = emptyList(),
        pythonOptions = emptyList(),
        outputPathUris = emptyList(),
        libraries = emptyList(),
        scalacOptions = emptyList(),
      )

      // when
      val magicMetaModel = MagicMetaModelImpl(testMagicMetaModelProjectConfig, projectDetails)
      val diff = magicMetaModel.loadDefaultTargets()

      // then
      val expectedTargetA1 = createBuildTargetInfo(targetA1)

      val expectedTargetB1 = createBuildTargetInfo(targetB1)

      val expectedTargetB2 = createBuildTargetInfo(targetB2)

      runBlocking { diff.applyOnWorkspaceModel() }

      // TODO how to make it deterministic?
      magicMetaModel.getAllLoadedTargets() shouldContainExactlyInAnyOrder listOf(expectedTargetA1, expectedTargetB2)
      magicMetaModel.getAllNotLoadedTargets() shouldContainExactlyInAnyOrder listOf(expectedTargetB1)
    }

    @Test
    fun `should load all default targets after loading different targets (with loadTarget())`() {
      // given
      val projectRoot = createTempDirectory("root")
      projectRoot.toFile().deleteOnExit()

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

      val targetA1Package1 = createTempDirectory(projectRoot, "targetA1")
      targetA1Package1.toFile().deleteOnExit()
      val targetA1Package2 = createTempDirectory(targetA1Package1, "package")
      targetA1Package2.toFile().deleteOnExit()
      val targetA1File1 = kotlin.io.path.createTempFile(targetA1Package2, "File1", ".kt")
      targetA1File1.toFile().deleteOnExit()

      val targetA1Source1 = SourceItem(
        uri = targetA1File1.toUri().toString(),
        kind = SourceItemKind.FILE,
      )
      val targetA1Sources = SourcesItem(
        target = targetA1.id,
        sources = listOf(targetA1Source1),
      )

      val targetB1B2Package1 = createTempDirectory(projectRoot, "targetB1B2")
      targetB1B2Package1.toFile().deleteOnExit()
      val targetB1B2Package2 = createTempDirectory(targetB1B2Package1, "package")
      targetB1B2Package2.toFile().deleteOnExit()
      val targetB1B2File1 = kotlin.io.path.createTempFile(targetB1B2Package2, "File1", ".kt")
      targetB1B2File1.toFile().deleteOnExit()

      val targetB1B2Source1 = SourceItem(
        uri = targetB1B2File1.toUri().toString(),
        kind = SourceItemKind.FILE,
      )

      val targetB1Package1 = createTempDirectory(projectRoot, "targetB1")
      targetB1Package1.toFile().deleteOnExit()
      val targetB1Package2 = createTempDirectory(targetB1Package1, "package")
      targetB1Package2.toFile().deleteOnExit()

      val targetB1Source2 = SourceItem(
        uri = targetB1Package2.toUri().toString(),
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
        javacOptions = emptyList(),
        pythonOptions = emptyList(),
        outputPathUris = emptyList(),
        libraries = emptyList(),
        scalacOptions = emptyList(),
      )

      // when 1
      val magicMetaModel = MagicMetaModelImpl(testMagicMetaModelProjectConfig, projectDetails)
      val diff1 = magicMetaModel.loadDefaultTargets()

      // then 1
      val expectedTargetA1 = createBuildTargetInfo(targetA1)
      val expectedTargetB1 = createBuildTargetInfo(targetB1)
      val expectedTargetB2 = createBuildTargetInfo(targetB2)

      runBlocking { diff1.applyOnWorkspaceModel() }

      // TODO should be B1 not B2 - how to make it deterministic?
      magicMetaModel.getAllLoadedTargets() shouldContainExactlyInAnyOrder listOf(expectedTargetA1, expectedTargetB2)
      magicMetaModel.getAllNotLoadedTargets() shouldContainExactlyInAnyOrder listOf(expectedTargetB1)

      // when 2
      val diff2 = magicMetaModel.loadTarget(targetB1.id.uri)!!

      // then 2
      runBlocking { diff2.applyOnWorkspaceModel() }

      magicMetaModel.getAllLoadedTargets() shouldContainExactlyInAnyOrder listOf(expectedTargetA1, expectedTargetB1)
      magicMetaModel.getAllNotLoadedTargets() shouldContainExactlyInAnyOrder listOf(expectedTargetB2)

      // when 3
      val diff3 = magicMetaModel.loadDefaultTargets()

      // then 3
      runBlocking { diff3.applyOnWorkspaceModel() }

      magicMetaModel.getAllLoadedTargets() shouldContainExactlyInAnyOrder listOf(expectedTargetA1, expectedTargetB2)
      magicMetaModel.getAllNotLoadedTargets() shouldContainExactlyInAnyOrder listOf(expectedTargetB1)
    }
  }

  @Nested
  @DisplayName("magicMetaModelImpl.loadTarget(targetId) tests")
  inner class MagicMetaModelImplLoadTargetTest {
    @Test
    fun `should return null for not existing target`() {
      // given
      val projectRoot = createTempDirectory("root")
      projectRoot.toFile().deleteOnExit()

      val targetA1 = BuildTarget(
        id = BuildTargetId("targetA1"),
        languageIds = listOf("kotlin"),
        dependencies = listOf(
          BuildTargetId("externalDep1"),
          BuildTargetId("externalDep2"),
        ),
      )

      val targetA1Package1 = createTempDirectory(projectRoot, "targetA1")
      targetA1Package1.toFile().deleteOnExit()
      val targetA1Package2 = createTempDirectory(targetA1Package1, "package")
      targetA1Package2.toFile().deleteOnExit()
      val targetA1File1 = kotlin.io.path.createTempFile(targetA1Package2, "File1", ".kt")
      targetA1File1.toFile().deleteOnExit()

      val targetA1Source1 = SourceItem(
        uri = targetA1File1.toUri().toString(),
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
        javacOptions = emptyList(),
        pythonOptions = emptyList(),
        outputPathUris = emptyList(),
        libraries = emptyList(),
        scalacOptions = emptyList(),
      )

      // when
      val magicMetaModel = MagicMetaModelImpl(testMagicMetaModelProjectConfig, projectDetails)

      val notExistingTargetId = BuildTargetId("//not/existing/target")
      val diff = magicMetaModel.loadTarget(notExistingTargetId.uri)

      // then
      diff shouldBe null
    }

    @Test
    fun `should load target`() {
      // given
      val projectRoot = createTempDirectory("root")
      projectRoot.toFile().deleteOnExit()

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

      val targetA1Package1 = createTempDirectory(projectRoot, "targetA1")
      targetA1Package1.toFile().deleteOnExit()
      val targetA1Package2 = createTempDirectory(targetA1Package1, "package")
      targetA1Package2.toFile().deleteOnExit()
      val targetA1File1 = kotlin.io.path.createTempFile(targetA1Package2, "File1", ".kt")
      targetA1File1.toFile().deleteOnExit()

      val targetA1Source1 = SourceItem(
        uri = targetA1File1.toUri().toString(),
        kind = SourceItemKind.FILE,
      )
      val targetA1Sources = SourcesItem(
        target = targetA1.id,
        sources = listOf(targetA1Source1),
      )

      val targetB1Package1 = createTempDirectory(projectRoot, "targetB1")
      targetB1Package1.toFile().deleteOnExit()
      val targetB1Package2 = createTempDirectory(targetB1Package1, "package")
      targetB1Package2.toFile().deleteOnExit()

      val targetB1Source1 = SourceItem(
        uri = targetB1Package2.toUri().toString(),
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
        javacOptions = emptyList(),
        pythonOptions = emptyList(),
        outputPathUris = emptyList(),
        libraries = emptyList(),
        scalacOptions = emptyList(),
      )

      // when
      val magicMetaModel = MagicMetaModelImpl(testMagicMetaModelProjectConfig, projectDetails)

      val diff = magicMetaModel.loadTarget(targetA1.id.uri)!!

      // then
      val expectedTargetA1 = createBuildTargetInfo(targetA1)
      val expectedTargetB1 = createBuildTargetInfo(targetB1)
      runBlocking { diff.applyOnWorkspaceModel() }

      magicMetaModel.getAllLoadedTargets() shouldContainExactlyInAnyOrder listOf(expectedTargetA1)
      magicMetaModel.getAllNotLoadedTargets() shouldContainExactlyInAnyOrder listOf(expectedTargetB1)
    }

    @Test
    fun `should return null for already loaded target`() {
      // given
      val projectRoot = createTempDirectory("root")
      projectRoot.toFile().deleteOnExit()

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

      val targetA1Package1 = createTempDirectory(projectRoot, "targetA1")
      targetA1Package1.toFile().deleteOnExit()
      val targetA1Package2 = createTempDirectory(targetA1Package1, "package")
      targetA1Package2.toFile().deleteOnExit()
      val targetA1File1 = kotlin.io.path.createTempFile(targetA1Package2, "File1", ".kt")
      targetA1File1.toFile().deleteOnExit()

      val targetA1Source1 = SourceItem(
        uri = targetA1File1.toUri().toString(),
        kind = SourceItemKind.FILE,
      )
      val targetA1Sources = SourcesItem(
        target = targetA1.id,
        sources = listOf(targetA1Source1),
      )

      val targetB1Package1 = createTempDirectory(projectRoot, "targetB1")
      targetB1Package1.toFile().deleteOnExit()
      val targetB1Package2 = createTempDirectory(targetB1Package1, "package")
      targetB1Package2.toFile().deleteOnExit()

      val targetB1Source1 = SourceItem(
        uri = targetB1Package2.toUri().toString(),
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
        javacOptions = emptyList(),
        pythonOptions = emptyList(),
        outputPathUris = emptyList(),
        libraries = emptyList(),
        scalacOptions = emptyList(),
      )

      // when 1
      val magicMetaModel = MagicMetaModelImpl(testMagicMetaModelProjectConfig, projectDetails)

      val diff1 = magicMetaModel.loadTarget(targetA1.id.uri)!!

      // then 1
      runBlocking { diff1.applyOnWorkspaceModel() }

      // when 2
      val diff2 = magicMetaModel.loadTarget(targetA1.id.uri)

      // then 2
      val expectedTargetA1 = createBuildTargetInfo(targetA1)
      val expectedTargetB1 = createBuildTargetInfo(targetB1)
      diff2 shouldBe null

      magicMetaModel.getAllLoadedTargets() shouldContainExactlyInAnyOrder listOf(expectedTargetA1)
      magicMetaModel.getAllNotLoadedTargets() shouldContainExactlyInAnyOrder listOf(expectedTargetB1)
    }

    @Test
    fun `should add targets without overlapping`() {
      // given
      val projectRoot = createTempDirectory("root")
      projectRoot.toFile().deleteOnExit()

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

      val targetA1Package1 = createTempDirectory(projectRoot, "targetA1")
      targetA1Package1.toFile().deleteOnExit()
      val targetA1Package2 = createTempDirectory(targetA1Package1, "package")
      targetA1Package2.toFile().deleteOnExit()
      val targetA1File1 = kotlin.io.path.createTempFile(targetA1Package2, "File1", ".kt")
      targetA1File1.toFile().deleteOnExit()

      val targetA1Source1 = SourceItem(
        uri = targetA1File1.toUri().toString(),
        kind = SourceItemKind.FILE,
      )
      val targetA1Sources = SourcesItem(
        target = targetA1.id,
        sources = listOf(targetA1Source1),
      )

      val targetB1Package1 = createTempDirectory(projectRoot, "targetB1")
      targetB1Package1.toFile().deleteOnExit()
      val targetB1Package2 = createTempDirectory(targetB1Package1, "package")
      targetB1Package2.toFile().deleteOnExit()

      val targetB1Source1 = SourceItem(
        uri = targetB1Package2.toUri().toString(),
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
        javacOptions = emptyList(),
        pythonOptions = emptyList(),
        outputPathUris = emptyList(),
        libraries = emptyList(),
        scalacOptions = emptyList(),
      )

      // when 1
      val magicMetaModel = MagicMetaModelImpl(testMagicMetaModelProjectConfig, projectDetails)

      val diff1 = magicMetaModel.loadTarget(targetA1.id.uri)!!

      // then 1
      runBlocking { diff1.applyOnWorkspaceModel() }

      // when 2
      val diff2 = magicMetaModel.loadTarget(targetB1.id.uri)!!

      // then 2
      val expectedTargetA1 = createBuildTargetInfo(targetA1)
      val expectedTargetB1 = createBuildTargetInfo(targetB1)
      runBlocking { diff2.applyOnWorkspaceModel() }

      magicMetaModel.getAllLoadedTargets() shouldContainExactlyInAnyOrder listOf(expectedTargetA1, expectedTargetB1)
      magicMetaModel.getAllNotLoadedTargets() shouldBe emptyList()
    }

    @Test
    fun `should add target and remove overlapping targets`() {
      // given
      val projectRoot = createTempDirectory("root")
      projectRoot.toFile().deleteOnExit()

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

      val targetA1A2Package1 = createTempDirectory(projectRoot, "targetA1A2")
      targetA1A2Package1.toFile().deleteOnExit()
      val targetA1A2Package2 = createTempDirectory(targetA1A2Package1, "package")
      targetA1A2Package2.toFile().deleteOnExit()
      val targetA1A2File1 = kotlin.io.path.createTempFile(targetA1A2Package2, "File1", ".kt")
      targetA1A2File1.toFile().deleteOnExit()

      val targetA1A2Source1 = SourceItem(
        uri = targetA1A2File1.toUri().toString(),
        kind = SourceItemKind.FILE,
      )

      val targetA1A3Package1 = createTempDirectory(projectRoot, "targetA1A3")
      targetA1A3Package1.toFile().deleteOnExit()
      val targetA1A3Package2 = createTempDirectory(targetA1A3Package1, "package")
      targetA1A3Package2.toFile().deleteOnExit()
      val targetA1A3File1 = kotlin.io.path.createTempFile(targetA1A3Package2, "File1", ".kt")
      targetA1A3File1.toFile().deleteOnExit()

      val targetA1A3Source1 = SourceItem(
        uri = targetA1A3File1.toUri().toString(),
        SourceItemKind.FILE,
        false,
      )

      val targetA1Package1 = createTempDirectory(projectRoot, "targetA1")
      targetA1Package1.toFile().deleteOnExit()
      val targetA1Package2 = createTempDirectory(targetA1Package1, "package")
      targetA1Package2.toFile().deleteOnExit()
      val targetA1File1 = kotlin.io.path.createTempFile(targetA1Package2, "File1", ".kt")
      targetA1File1.toFile().deleteOnExit()

      val targetA1Source1 = SourceItem(
        uri = targetA1File1.toUri().toString(),
        kind = SourceItemKind.FILE,
      )
      val targetA1Sources = SourcesItem(
        target = targetA1.id,
        sources = listOf(targetA1Source1, targetA1A2Source1, targetA1A3Source1),
      )

      val targetA3Package1 = createTempDirectory(projectRoot, "targetA3")
      targetA3Package1.toFile().deleteOnExit()
      val targetA3Package2 = createTempDirectory(targetA3Package1, "package")
      targetA3Package2.toFile().deleteOnExit()
      val targetA3File1 = kotlin.io.path.createTempFile(targetA3Package2, "File1", ".kt")
      targetA3File1.toFile().deleteOnExit()

      val targetA2Source1 = SourceItem(
        uri = targetA3File1.toUri().toString(),
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
        javacOptions = emptyList(),
        pythonOptions = emptyList(),
        outputPathUris = emptyList(),
        libraries = emptyList(),
        scalacOptions = emptyList(),
      )

      // when 1
      val magicMetaModel = MagicMetaModelImpl(testMagicMetaModelProjectConfig, projectDetails)
      val diff1 = magicMetaModel.loadTarget(targetA2.id.uri)!!

      // then 1
      runBlocking { diff1.applyOnWorkspaceModel() }

      // when 2
      val diff2 = magicMetaModel.loadTarget(targetA3.id.uri)!!

      // then 2
      val expectedTargetA1 = createBuildTargetInfo(targetA1)
      val expectedTargetA2 = createBuildTargetInfo(targetA2)
      val expectedTargetA3 = createBuildTargetInfo(targetA3)
      runBlocking { diff2.applyOnWorkspaceModel() }

      magicMetaModel.getAllLoadedTargets() shouldContainExactlyInAnyOrder listOf(expectedTargetA2, expectedTargetA3)
      magicMetaModel.getAllNotLoadedTargets() shouldContainExactlyInAnyOrder listOf(expectedTargetA1)

      // when 3
      val diff3 = magicMetaModel.loadTarget(targetA1.id.uri)!!

      // then 3
      runBlocking { diff3.applyOnWorkspaceModel() }

      magicMetaModel.getAllLoadedTargets() shouldContainExactlyInAnyOrder listOf(expectedTargetA1)
      magicMetaModel.getAllNotLoadedTargets() shouldContainExactlyInAnyOrder listOf(expectedTargetA2, expectedTargetA3)
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
        javacOptions = emptyList(),
        pythonOptions = emptyList(),
        outputPathUris = emptyList(),
        libraries = emptyList(),
        scalacOptions = emptyList(),
      )

      // when
      val magicMetaModel = MagicMetaModelImpl(testMagicMetaModelProjectConfig, projectDetails)

      val documentTargetsDetails =
        magicMetaModel.getTargetsDetailsForDocument(TextDocumentId("file:///not/existing/document"))

      // then
      documentTargetsDetails shouldBe DocumentTargetsDetails(
        loadedTargetId = null,
        notLoadedTargetsIds = emptyList(),
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
        javacOptions = emptyList(),
        pythonOptions = emptyList(),
        outputPathUris = emptyList(),
        libraries = emptyList(),
        scalacOptions = emptyList(),
      )

      // when
      val magicMetaModel = MagicMetaModelImpl(testMagicMetaModelProjectConfig, projectDetails)

      val documentTargetsDetails = magicMetaModel.getTargetsDetailsForDocument(TextDocumentId(targetA1Source1.uri))

      // then
      documentTargetsDetails shouldBe DocumentTargetsDetails(
        loadedTargetId = null,
        notLoadedTargetsIds = listOf(targetA1.id.uri),
      )
    }

    @Test
    fun `should return loaded target for non overlapping targets after loading default targets (all targets)`() {
      // given
      val projectRoot = createTempDirectory("root")
      projectRoot.toFile().deleteOnExit()

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

      val targetA1Package1 = createTempDirectory(projectRoot, "targetA1")
      targetA1Package1.toFile().deleteOnExit()
      val targetA1Package2 = createTempDirectory(targetA1Package1, "package")
      targetA1Package2.toFile().deleteOnExit()
      val targetA1File1 = kotlin.io.path.createTempFile(targetA1Package2, "File1", ".kt")
      targetA1File1.toFile().deleteOnExit()

      val targetA1Source1 = SourceItem(
        uri = targetA1File1.toUri().toString(),
        kind = SourceItemKind.FILE,
      )
      val targetA1Sources = SourcesItem(
        target = targetA1.id,
        sources = listOf(targetA1Source1),
      )

      val targetB1Package1 = createTempDirectory(projectRoot, "targetB1")
      targetB1Package1.toFile().deleteOnExit()
      val targetB1Package2 = createTempDirectory(targetB1Package1, "package")
      targetB1Package2.toFile().deleteOnExit()
      val targetB1File1 = kotlin.io.path.createTempFile(targetB1Package2, "File1", ".kt")
      targetB1File1.toFile().deleteOnExit()

      val targetB1Source1 = SourceItem(
        uri = targetB1File1.toUri().toString(),
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
        javacOptions = emptyList(),
        pythonOptions = emptyList(),
        outputPathUris = emptyList(),
        libraries = emptyList(),
        scalacOptions = emptyList(),
      )

      // when
      val magicMetaModel = MagicMetaModelImpl(testMagicMetaModelProjectConfig, projectDetails)
      val diff = magicMetaModel.loadDefaultTargets()

      // then
      runBlocking { diff.applyOnWorkspaceModel() }

      magicMetaModel.getTargetsDetailsForDocument(TextDocumentId(targetA1Source1.uri)) shouldBe DocumentTargetsDetails(
        loadedTargetId = targetA1.id.uri,
        notLoadedTargetsIds = emptyList(),
      )
      magicMetaModel.getTargetsDetailsForDocument(TextDocumentId(targetB1Source1.uri)) shouldBe DocumentTargetsDetails(
        loadedTargetId = targetB1.id.uri,
        notLoadedTargetsIds = emptyList(),
      )
    }

    @Test
    fun `should return loaded target for source in loaded target and no loaded target for source in not loaded target for model with overlapping targets`() {
      // given
      val projectRoot = createTempDirectory("root")
      projectRoot.toFile().deleteOnExit()

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

      val targetA1A2Package1 = createTempDirectory(projectRoot, "targetA1A2")
      targetA1A2Package1.toFile().deleteOnExit()
      val targetA1A2Package2 = createTempDirectory(targetA1A2Package1, "package")
      targetA1A2Package2.toFile().deleteOnExit()
      val targetA1A2File1 = kotlin.io.path.createTempFile(targetA1A2Package2, "File1", ".kt")
      targetA1A2File1.toFile().deleteOnExit()

      val targetA1A2Source1 = SourceItem(
        uri = targetA1A2File1.toUri().toString(),
        kind = SourceItemKind.FILE,
      )
      val targetA1Sources = SourcesItem(
        target = targetA1.id,
        sources = listOf(targetA1A2Source1),
      )

      val targetA2Package1 = createTempDirectory(projectRoot, "targetA2")
      targetA2Package1.toFile().deleteOnExit()
      val targetA2Package2 = createTempDirectory(targetA2Package1, "package")
      targetA2Package2.toFile().deleteOnExit()

      val targetA2Source1 = SourceItem(
        uri = targetA2Package2.toUri().toString(),
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
        javacOptions = emptyList(),
        pythonOptions = emptyList(),
        outputPathUris = emptyList(),
        libraries = emptyList(),
        scalacOptions = emptyList(),
      )

      // when
      val magicMetaModel = MagicMetaModelImpl(testMagicMetaModelProjectConfig, projectDetails)
      val diff = magicMetaModel.loadTarget(targetA1.id.uri)!!

      // then
      runBlocking { diff.applyOnWorkspaceModel() }

      magicMetaModel.getTargetsDetailsForDocument(TextDocumentId(targetA1A2Source1.uri)) shouldBe DocumentTargetsDetails(
        loadedTargetId = targetA1.id.uri,
        notLoadedTargetsIds = listOf(targetA2.id.uri),
      )

      magicMetaModel.getTargetsDetailsForDocument(TextDocumentId(targetA2Source1.uri)) shouldBe DocumentTargetsDetails(
        loadedTargetId = null,
        notLoadedTargetsIds = listOf(targetA2.id.uri),
      )
    }
  }
}
