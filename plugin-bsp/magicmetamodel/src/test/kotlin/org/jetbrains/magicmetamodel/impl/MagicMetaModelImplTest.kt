@file:Suppress("LongMethod", "MaxLineLength")

package org.jetbrains.magicmetamodel.impl

import ch.epfl.scala.bsp4j.BuildTarget
import ch.epfl.scala.bsp4j.BuildTargetCapabilities
import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.SourceItem
import ch.epfl.scala.bsp4j.SourceItemKind
import ch.epfl.scala.bsp4j.SourcesItem
import ch.epfl.scala.bsp4j.TextDocumentIdentifier
import com.intellij.openapi.command.WriteCommandAction
import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAnyOf
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotContainAnyOf
import io.kotest.matchers.shouldBe
import org.jetbrains.magicmetamodel.MagicMetaModelProjectConfig
import org.jetbrains.magicmetamodel.ProjectDetails
import org.jetbrains.workspace.model.test.framework.WorkspaceModelBaseTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import java.nio.file.Files

// TODO add checking workspacemodel
// TODO extract 'given' to separate objects
@DisplayName("MagicMetaModelImpl tests")
class MagicMetaModelImplTest : WorkspaceModelBaseTest() {

  private lateinit var testMagicMetaModelProjectConfig: MagicMetaModelProjectConfig

  @BeforeEach
  override fun beforeEach() {
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
        targets = emptyList(),
        sources = emptyList(),
        resources = emptyList(),
        dependenciesSources = emptyList(),
      )

      // when & then
      val magicMetaModel = MagicMetaModelImpl(testMagicMetaModelProjectConfig, projectDetails)

      // showing loaded and not loaded targets to user (e.g. at the sidebar)
      magicMetaModel `should return given loaded and not loaded targets` Pair(emptyList(), emptyList())

      WriteCommandAction.runWriteCommandAction(project) {
        magicMetaModel.loadDefaultTargets()
      }

      // showing loaded and not loaded targets to user (e.g. at the sidebar)
      magicMetaModel.getAllLoadedTargets() shouldBe emptyList()
      magicMetaModel.getAllNotLoadedTargets() shouldBe emptyList()
    }

    @Test
    fun `should handle project without shared sources (like a simple kotlin project)`() {
      // given
      val libAId = BuildTargetIdentifier(":libA")
      val libA = BuildTarget(
        libAId,
        emptyList(),
        listOf("kotlin"),
        listOf(
          BuildTargetIdentifier("@maven//:dep1"),
          BuildTargetIdentifier("@maven//:dep2"),
        ),
        BuildTargetCapabilities(),
      )

      val libBId = BuildTargetIdentifier(":libB")
      val libB = BuildTarget(
        libBId,
        emptyList(),
        listOf("kotlin"),
        emptyList(),
        BuildTargetCapabilities(),
      )

      val libCId = BuildTargetIdentifier(":libC")
      val libC = BuildTarget(
        libCId,
        emptyList(),
        listOf("kotlin"),
        listOf(
          libBId,
          BuildTargetIdentifier("@maven//:dep1"),
          BuildTargetIdentifier("@maven//:dep3"),
        ),
        BuildTargetCapabilities(),
      )

      val appId = BuildTargetIdentifier(":app")
      val app = BuildTarget(
        appId,
        emptyList(),
        listOf("kotlin"),
        listOf(
          libAId,
          libBId,
          libCId,
          BuildTargetIdentifier("@maven//:dep1"),
        ),
        BuildTargetCapabilities(),
      )

      val sourceInLibAUri = "file:///libA/src/main/kotlin/"
      val sourceInLibA = SourceItem(
        sourceInLibAUri,
        SourceItemKind.DIRECTORY,
        false
      )
      val libASources = SourcesItem(
        libAId,
        listOf(sourceInLibA),
      )

      val sourceInLibBUri = "file:///libB/src/main/kotlin/org/jetbrains/libB/VeryImportantLibrary.kt"
      val sourceInLibB = SourceItem(
        sourceInLibBUri,
        SourceItemKind.FILE,
        false
      )
      val libBSources = SourcesItem(
        libBId,
        listOf(sourceInLibB),
      )

      val sourceInLibCUri = "file:///libC/src/main/kotlin/"
      val sourceInLibC = SourceItem(
        sourceInLibCUri,
        SourceItemKind.DIRECTORY,
        false
      )
      val libCSources = SourcesItem(
        libCId,
        listOf(sourceInLibC),
      )

      val sourceInAppUri = "file:///app/src/main/kotlin/org/jetbrains/App.kt"
      val sourceInApp = SourceItem(
        sourceInAppUri,
        SourceItemKind.FILE,
        false
      )
      val appSources = SourcesItem(
        appId,
        listOf(sourceInApp),
      )

      val projectDetails = ProjectDetails(
        targetsId = listOf(libAId, libBId, libCId, appId),
        targets = listOf(libA, libB, libC, app),
        sources = listOf(libASources, libBSources, libCSources, appSources),
        resources = emptyList(),
        dependenciesSources = emptyList(),
      )

      // when & then
      val magicMetaModel = MagicMetaModelImpl(testMagicMetaModelProjectConfig, projectDetails)

      // showing loaded and not loaded targets to user (e.g. at the sidebar)
      magicMetaModel `should return given loaded and not loaded targets` Pair(
        emptyList(),
        listOf(libA, libB, libC, app)
      )

      // after bsp importing process
      WriteCommandAction.runWriteCommandAction(project) {
        magicMetaModel.loadDefaultTargets()
      }

      // showing loaded and not loaded targets to user (e.g. at the sidebar)
      magicMetaModel `should return given loaded and not loaded targets` Pair(
        listOf(libA, libB, libC, app),
        emptyList()
      )

      // user opens each file and checks the loaded target for each file (e.g. at the bottom bar widget)
      magicMetaModel `should return valid targets details for document` Triple(sourceInLibAUri, libAId, emptyList())
      magicMetaModel `should return valid targets details for document` Triple(sourceInLibBUri, libBId, emptyList())
      magicMetaModel `should return valid targets details for document` Triple(sourceInLibCUri, libCId, emptyList())
      magicMetaModel `should return valid targets details for document` Triple(sourceInAppUri, appId, emptyList())
    }

    @Test
    fun `should handle project with shared sources (like a scala cross version project)`() {
      // given
      val libA212Id = BuildTargetIdentifier(":libA-2.12")
      val libA212 = BuildTarget(
        libA212Id,
        emptyList(),
        listOf("scala"),
        listOf(
          BuildTargetIdentifier("@maven//:dep1"),
          BuildTargetIdentifier("@maven//:dep2"),
        ),
        BuildTargetCapabilities(),
      )

      val libB212Id = BuildTargetIdentifier(":libB-2.12")
      val libB212 = BuildTarget(
        libB212Id,
        emptyList(),
        listOf("scala"),
        emptyList(),
        BuildTargetCapabilities(),
      )

      val libB213Id = BuildTargetIdentifier(":libB-2.13")
      val libB213 = BuildTarget(
        libB213Id,
        emptyList(),
        listOf("scala"),
        emptyList(),
        BuildTargetCapabilities(),
      )

      val libC212Id = BuildTargetIdentifier(":libC-2.12")
      val libC212 = BuildTarget(
        libC212Id,
        emptyList(),
        listOf("scala"),
        listOf(
          libB212Id,
          BuildTargetIdentifier("@maven//:dep1"),
          BuildTargetIdentifier("@maven//:dep3"),
        ),
        BuildTargetCapabilities(),
      )

      val libC213Id = BuildTargetIdentifier(":libC-2.13")
      val libC213 = BuildTarget(
        libC213Id,
        emptyList(),
        listOf("scala"),
        listOf(
          libB213Id,
          BuildTargetIdentifier("@maven//:dep1"),
          BuildTargetIdentifier("@maven//:dep3"),
        ),
        BuildTargetCapabilities(),
      )

      val app212Id = BuildTargetIdentifier(":app-2.12")
      val app212 = BuildTarget(
        app212Id,
        emptyList(),
        listOf("scala"),
        listOf(
          libA212Id,
          libB212Id,
          libC212Id,
          BuildTargetIdentifier("@maven//:dep1"),
        ),
        BuildTargetCapabilities(),
      )
      val app213Id = BuildTargetIdentifier(":app-2.13")
      val app213 = BuildTarget(
        app213Id,
        emptyList(),
        listOf("scala"),
        listOf(
          libB213Id,
          libC213Id,
          BuildTargetIdentifier("@maven//:dep1"),
        ),
        BuildTargetCapabilities(),
      )

      val sourceInLibAUri = "file:///libA/src/main/kotlin/"
      val sourceInLibA = SourceItem(
        sourceInLibAUri,
        SourceItemKind.DIRECTORY,
        false
      )
      val libA212Sources = SourcesItem(
        libA212Id,
        listOf(sourceInLibA),
      )

      val sourceInLibBUri = "file:///libB/src/main/kotlin/org/jetbrains/libB/VeryImportantLibrary.kt"
      val sourceInLibB = SourceItem(
        sourceInLibBUri,
        SourceItemKind.FILE,
        false
      )
      val libB212Sources = SourcesItem(
        libB212Id,
        listOf(sourceInLibB),
      )
      val libB213Sources = SourcesItem(
        libB213Id,
        listOf(sourceInLibB),
      )

      val sourceInLibCUri = "file:///libC/src/main/kotlin/"
      val sourceInLibC = SourceItem(
        sourceInLibCUri,
        SourceItemKind.DIRECTORY,
        false
      )
      val libC212Sources = SourcesItem(
        libC212Id,
        listOf(sourceInLibC),
      )
      val libC213Sources = SourcesItem(
        libC213Id,
        listOf(sourceInLibC),
      )

      val sourceInAppUri = "file:///app/src/main/kotlin/org/jetbrains/App.kt"
      val sourceInApp = SourceItem(
        sourceInAppUri,
        SourceItemKind.FILE,
        false
      )
      val app212Sources = SourcesItem(
        app212Id,
        listOf(sourceInApp),
      )
      val app213Sources = SourcesItem(
        app213Id,
        listOf(sourceInApp),
      )

      val projectDetails = ProjectDetails(
        targetsId = listOf(libA212Id, libC213Id, libB213Id, libB212Id, app212Id, libC212Id, app213Id),
        targets = listOf(libA212, libB213, libB212, libC212, libC213, app213, app212),
        sources = listOf(
          libA212Sources,
          libB212Sources,
          libB213Sources,
          libC213Sources,
          libC212Sources,
          app213Sources,
          app212Sources,
        ),
        resources = emptyList(),
        dependenciesSources = emptyList(),
      )

      // when & then
      val magicMetaModel = MagicMetaModelImpl(testMagicMetaModelProjectConfig, projectDetails)

      // showing loaded and not loaded targets to user
      magicMetaModel `should return given loaded and not loaded targets` Pair(
        emptyList(),
        listOf(libA212, libB213, libB212, libC212, libC213, app213, app212)
      )

      // after bsp importing process
      WriteCommandAction.runWriteCommandAction(project) {
        magicMetaModel.loadDefaultTargets()
      }

      // now we are collecting loaded by default targets
      val (loadedLibBByDefault, notLoadedLibBByDefault) =
        getLoadedAndNotLoadedTargetsOrThrow(libB212, libB213, magicMetaModel.getAllLoadedTargets())
      val (loadedLibCByDefault, notLoadedLibCByDefault) =
        getLoadedAndNotLoadedTargetsOrThrow(libC212, libC213, magicMetaModel.getAllLoadedTargets())
      val (loadedAppByDefault, notLoadedAppByDefault) =
        getLoadedAndNotLoadedTargetsOrThrow(app212, app213, magicMetaModel.getAllLoadedTargets())

      // showing loaded and not loaded targets to user (e.g. at the sidebar)
      magicMetaModel `should return given loaded and not loaded targets` Pair(
        listOf(libA212, loadedLibBByDefault, loadedLibCByDefault, loadedAppByDefault),
        listOf(notLoadedLibBByDefault, notLoadedLibCByDefault, notLoadedAppByDefault),
      )

      // ------
      // user decides to load not loaded `:app-2.1*` target by default
      // (app-2.13 if app-2.12 is currently loaded or vice versa)
      // ------
      WriteCommandAction.runWriteCommandAction(project) {
        magicMetaModel.loadTarget(notLoadedAppByDefault.id)
      }

      // showing loaded and not loaded targets to user (e.g. at the sidebar)
      magicMetaModel `should return given loaded and not loaded targets` Pair(
        listOf(libA212, loadedLibBByDefault, loadedLibCByDefault, notLoadedAppByDefault),
        listOf(notLoadedLibBByDefault, notLoadedLibCByDefault, loadedAppByDefault),
      )

      // user opens each file and checks the loaded target for each file (e.g. at the bottom bar widget)
      magicMetaModel `should return valid targets details for document`
        Triple(sourceInLibAUri, libA212Id, emptyList())
      magicMetaModel `should return valid targets details for document`
        Triple(sourceInLibBUri, loadedLibBByDefault.id, listOf(notLoadedLibBByDefault.id))
      magicMetaModel `should return valid targets details for document`
        Triple(sourceInLibCUri, loadedLibCByDefault.id, listOf(notLoadedLibCByDefault.id))
      // user switched this target!
      magicMetaModel `should return valid targets details for document`
        Triple(sourceInAppUri, notLoadedAppByDefault.id, listOf(loadedAppByDefault.id))

      // ------
      // well, now user decides to load not loaded `:libB-2.1*` target by default
      // ------
      WriteCommandAction.runWriteCommandAction(project) {
        magicMetaModel.loadTarget(notLoadedLibBByDefault.id)
      }

      // showing loaded and not loaded targets to user (e.g. at the sidebar)
      magicMetaModel `should return given loaded and not loaded targets` Pair(
        listOf(libA212, notLoadedLibBByDefault, loadedLibCByDefault, notLoadedAppByDefault),
        listOf(loadedLibBByDefault, notLoadedLibCByDefault, loadedAppByDefault),
      )

      // user opens each file and checks the loaded target for each file (e.g. at the bottom bar widget)
      magicMetaModel `should return valid targets details for document`
        Triple(sourceInLibAUri, libA212Id, emptyList())
      // user switched this target now!
      magicMetaModel `should return valid targets details for document`
        Triple(sourceInLibBUri, notLoadedLibBByDefault.id, listOf(loadedLibBByDefault.id))
      magicMetaModel `should return valid targets details for document`
        Triple(sourceInLibCUri, loadedLibCByDefault.id, listOf(notLoadedLibCByDefault.id))
      magicMetaModel `should return valid targets details for document`
        Triple(sourceInAppUri, notLoadedAppByDefault.id, listOf(loadedAppByDefault.id))

      // ------
      // and, finally user decides to load the default configuration
      // ------

      WriteCommandAction.runWriteCommandAction(project) {
        magicMetaModel.loadDefaultTargets()
      }

      // showing loaded and not loaded targets to user (e.g. at the sidebar)
      magicMetaModel `should return given loaded and not loaded targets` Pair(
        listOf(libA212, loadedLibBByDefault, loadedLibCByDefault, loadedAppByDefault),
        listOf(notLoadedLibBByDefault, notLoadedLibCByDefault, notLoadedAppByDefault),
      )

      // user opens each file and checks the loaded target for each file (e.g. at the bottom bar widget)
      magicMetaModel `should return valid targets details for document`
        Triple(sourceInLibAUri, libA212Id, emptyList())
      magicMetaModel `should return valid targets details for document`
        Triple(sourceInLibBUri, loadedLibBByDefault.id, listOf(notLoadedLibBByDefault.id))
      magicMetaModel `should return valid targets details for document`
        Triple(sourceInLibCUri, loadedLibCByDefault.id, listOf(notLoadedLibCByDefault.id))
      magicMetaModel `should return valid targets details for document`
        Triple(sourceInAppUri, loadedAppByDefault.id, listOf(notLoadedAppByDefault.id))
    }

    private fun getLoadedAndNotLoadedTargetsOrThrow(
      target1: BuildTarget,
      target2: BuildTarget,
      loadedTargets: List<BuildTarget>,
    ): Pair<BuildTarget, BuildTarget> =
      when (Pair(loadedTargets.contains(target1), loadedTargets.contains(target2))) {
        Pair(true, false) -> Pair(target1, target2)
        Pair(false, true) -> Pair(target2, target1)
        else -> fail("Invalid loaded targets! Loaded targets should contain either ${target1.id} or ${target2.id}")
      }

    private infix fun MagicMetaModelImpl.`should return given loaded and not loaded targets`(
      expectedLoadedAndNotLoadedTargets: Pair<List<BuildTarget>, List<BuildTarget>>,
    ) {
      // then
      this.getAllLoadedTargets() shouldContainExactlyInAnyOrder expectedLoadedAndNotLoadedTargets.first
      this.getAllNotLoadedTargets() shouldContainExactlyInAnyOrder expectedLoadedAndNotLoadedTargets.second
    }

    private infix fun MagicMetaModelImpl.`should return valid targets details for document`(
      documentUrlLoadedTargetAndNotLoadedTargets: Triple<String, BuildTargetIdentifier?, List<BuildTargetIdentifier>>,
    ) {
      val documentId = TextDocumentIdentifier(documentUrlLoadedTargetAndNotLoadedTargets.first)
      val targetDetails = this.getTargetsDetailsForDocument(documentId)

      // then
      targetDetails.loadedTargetId shouldBe documentUrlLoadedTargetAndNotLoadedTargets.second
      targetDetails.notLoadedTargetsIds shouldContainExactlyInAnyOrder documentUrlLoadedTargetAndNotLoadedTargets.third
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
        targets = emptyList(),
        sources = emptyList(),
        resources = emptyList(),
        dependenciesSources = emptyList(),
      )

      // when
      val magicMetaModel = MagicMetaModelImpl(testMagicMetaModelProjectConfig, projectDetails)
      WriteCommandAction.runWriteCommandAction(project) {
        magicMetaModel.loadDefaultTargets()
      }

      val loadedTargets = magicMetaModel.getAllLoadedTargets()
      val notLoadedTargets = magicMetaModel.getAllNotLoadedTargets()

      // then
      loadedTargets shouldBe emptyList()
      notLoadedTargets shouldBe emptyList()
    }

    @Test
    fun `should return no loaded and all targets as not loaded for not initialized project (before calling loadDefaultTargets())`() {
      // given
      val target1Id = BuildTargetIdentifier("//target1")
      val target1 = BuildTarget(
        target1Id,
        emptyList(),
        listOf("kotlin"),
        listOf(
          BuildTargetIdentifier("@maven//:dep1.1"),
          BuildTargetIdentifier("@maven//:dep1.2"),
        ),
        BuildTargetCapabilities(),
      )

      val target2Id = BuildTargetIdentifier("//target2")
      val target2 = BuildTarget(
        target2Id,
        emptyList(),
        listOf("kotlin"),
        listOf(BuildTargetIdentifier("@maven//:dep2.1")),
        BuildTargetCapabilities(),
      )

      val source1InTarget1 = SourceItem(
        "file:///file1/in/target1",
        SourceItemKind.FILE,
        false
      )
      val target1Sources = SourcesItem(
        target1Id,
        listOf(source1InTarget1),
      )

      val source1InTarget2 = SourceItem(
        "file:///file1/in/target2",
        SourceItemKind.FILE,
        false
      )
      val target2Sources = SourcesItem(
        target2Id,
        listOf(source1InTarget2),
      )

      val projectDetails = ProjectDetails(
        targetsId = listOf(target1Id, target2Id),
        targets = listOf(target1, target2),
        sources = listOf(target1Sources, target2Sources),
        resources = emptyList(),
        dependenciesSources = emptyList(),
      )

      // when
      val magicMetaModel = MagicMetaModelImpl(testMagicMetaModelProjectConfig, projectDetails)

      val loadedTargets = magicMetaModel.getAllLoadedTargets()
      val notLoadedTargets = magicMetaModel.getAllNotLoadedTargets()

      // then
      loadedTargets shouldBe emptyList()
      notLoadedTargets shouldContainExactlyInAnyOrder listOf(target1, target2)
    }

    @Test
    fun `should return all targets as loaded and no not loaded targets for project without shared sources`() {
      // given
      val target1Id = BuildTargetIdentifier("//target1")
      val target1 = BuildTarget(
        target1Id,
        emptyList(),
        listOf("kotlin"),
        listOf(
          BuildTargetIdentifier("@maven//:dep1.1"),
          BuildTargetIdentifier("@maven//:dep1.2"),
        ),
        BuildTargetCapabilities(),
      )

      val target2Id = BuildTargetIdentifier("//target2")
      val target2 = BuildTarget(
        target2Id,
        emptyList(),
        listOf("kotlin"),
        listOf(BuildTargetIdentifier("@maven//:dep2.1")),
        BuildTargetCapabilities(),
      )

      val target3Id = BuildTargetIdentifier("//target3")
      val target3 = BuildTarget(
        target3Id,
        emptyList(),
        listOf("kotlin"),
        listOf(target1Id),
        BuildTargetCapabilities(),
      )

      val target4Id = BuildTargetIdentifier("//target4")
      val target4 = BuildTarget(
        target4Id,
        emptyList(),
        listOf("kotlin"),
        listOf(target3Id),
        BuildTargetCapabilities(),
      )
      target4.baseDirectory = Files.createTempDirectory("temp").toUri().toString()

      val source1InTarget1 = SourceItem(
        "file:///file1/in/target1",
        SourceItemKind.FILE,
        false
      )
      val target1Sources = SourcesItem(
        target1Id,
        listOf(source1InTarget1),
      )
      target1Sources.roots = emptyList()

      val source1InTarget2 = SourceItem(
        "file:///dir1/in/target2/",
        SourceItemKind.DIRECTORY,
        false
      )
      val target2Sources = SourcesItem(
        target2Id,
        listOf(source1InTarget2),
      )
      // TODO
      target2Sources.roots = emptyList()

      val source1InTarget3 = SourceItem(
        "file:///file1/in/target3",
        SourceItemKind.FILE,
        false
      )
      val source2InTarget3 = SourceItem(
        "file:///file2/in/target3",
        SourceItemKind.FILE,
        false
      )
      val target3Sources = SourcesItem(
        target3Id,
        listOf(source1InTarget3, source2InTarget3),
      )
      // TODO
      target3Sources.roots = emptyList()

      val target4Sources = SourcesItem(
        target4Id,
        emptyList(),
      )

      val projectDetails = ProjectDetails(
        targetsId = listOf(target1Id, target2Id, target3Id, target4Id),
        targets = listOf(target1, target2, target3, target4),
        sources = listOf(target1Sources, target2Sources, target3Sources, target4Sources),
        resources = emptyList(),
        dependenciesSources = emptyList(),
      )

      // when
      val magicMetaModel = MagicMetaModelImpl(testMagicMetaModelProjectConfig, projectDetails)
      WriteCommandAction.runWriteCommandAction(project) {
        magicMetaModel.loadDefaultTargets()
      }

      val loadedTargets = magicMetaModel.getAllLoadedTargets()
      val notLoadedTargets = magicMetaModel.getAllNotLoadedTargets()

      // then
      loadedTargets shouldContainExactlyInAnyOrder listOf(target1, target2, target3, target4)
      notLoadedTargets shouldBe emptyList()
    }

    @Test
    fun `should return non overlapping loaded targets for project with shared sources`() {
      // given
      val target1Id = BuildTargetIdentifier("//target1")
      val target1 = BuildTarget(
        target1Id,
        emptyList(),
        listOf("kotlin"),
        listOf(
          BuildTargetIdentifier("@maven//:dep1.1"),
          BuildTargetIdentifier("@maven//:dep1.2"),
        ),
        BuildTargetCapabilities(),
      )

      val target2Id = BuildTargetIdentifier("//target2")
      val target2 = BuildTarget(
        target2Id,
        emptyList(),
        listOf("kotlin"),
        listOf(BuildTargetIdentifier("@maven//:dep2.1")),
        BuildTargetCapabilities(),
      )

      val target3Id = BuildTargetIdentifier("//target3")
      val target3 = BuildTarget(
        target3Id,
        emptyList(),
        listOf("kotlin"),
        listOf(target1Id),
        BuildTargetCapabilities(),
      )

      val source1InTarget1 = SourceItem(
        "file:///file1/in/target1",
        SourceItemKind.FILE,
        false
      )
      val target1Sources = SourcesItem(
        target1Id,
        listOf(source1InTarget1),
      )

      val sourceInTarget2Target3 = SourceItem(
        "file:///file1/in/target2/target3",
        SourceItemKind.FILE,
        false
      )

      val source1InTarget2 = SourceItem(
        "file:///dir1/in/target2/",
        SourceItemKind.DIRECTORY,
        false
      )
      val target2Sources = SourcesItem(
        target2Id,
        listOf(source1InTarget2, sourceInTarget2Target3),
      )

      val target3Sources = SourcesItem(
        target3Id,
        listOf(sourceInTarget2Target3),
      )

      val projectDetails = ProjectDetails(
        targetsId = listOf(target1Id, target2Id, target3Id),
        targets = listOf(target1, target2, target3),
        sources = listOf(target1Sources, target2Sources, target3Sources),
        resources = emptyList(),
        dependenciesSources = emptyList(),
      )

      // when
      val magicMetaModel = MagicMetaModelImpl(testMagicMetaModelProjectConfig, projectDetails)
      WriteCommandAction.runWriteCommandAction(project) {
        magicMetaModel.loadDefaultTargets()
      }

      val loadedTargets = magicMetaModel.getAllLoadedTargets()
      val notLoadedTargets = magicMetaModel.getAllNotLoadedTargets()

      // then
      val targetsWithSharedSources = listOf(target2, target3)

      loadedTargets shouldHaveSize 2
      loadedTargets shouldContain target1
      loadedTargets shouldContainAnyOf targetsWithSharedSources

      notLoadedTargets shouldHaveSize 1
      notLoadedTargets shouldContainAnyOf targetsWithSharedSources

      loadedTargets shouldNotContainAnyOf notLoadedTargets
    }

    @Test
    fun `should load all default targets after loading different targets (with loadTarget())`() {
      // given
      val target1Id = BuildTargetIdentifier("//target1")
      val target1 = BuildTarget(
        target1Id,
        emptyList(),
        listOf("kotlin"),
        listOf(
          BuildTargetIdentifier("@maven//:dep1.1"),
          BuildTargetIdentifier("@maven//:dep1.2"),
        ),
        BuildTargetCapabilities(),
      )

      val target2Id = BuildTargetIdentifier("//target2")
      val target2 = BuildTarget(
        target2Id,
        emptyList(),
        listOf("kotlin"),
        listOf(BuildTargetIdentifier("@maven//:dep2.1")),
        BuildTargetCapabilities(),
      )

      val target3Id = BuildTargetIdentifier("//target3")
      val target3 = BuildTarget(
        target3Id,
        emptyList(),
        listOf("kotlin"),
        listOf(target1Id),
        BuildTargetCapabilities(),
      )

      val source1InTarget1 = SourceItem(
        "file:///file1/in/target1",
        SourceItemKind.FILE,
        false
      )
      val target1Sources = SourcesItem(
        target1Id,
        listOf(source1InTarget1),
      )
      // TODO
      target1Sources.roots = emptyList()

      val sourceInTarget2Target3 = SourceItem(
        "file:///file1/in/target2/target3",
        SourceItemKind.FILE,
        false
      )

      val source1InTarget2 = SourceItem(
        "file:///dir1/in/target2/",
        SourceItemKind.DIRECTORY,
        false
      )
      val target2Sources = SourcesItem(
        target2Id,
        listOf(source1InTarget2, sourceInTarget2Target3),
      )
      // TODO
      target2Sources.roots = emptyList()

      val target3Sources = SourcesItem(
        target3Id,
        listOf(sourceInTarget2Target3),
      )
      // TODO
      target3Sources.roots = emptyList()

      val projectDetails = ProjectDetails(
        targetsId = listOf(target1Id, target2Id, target3Id),
        targets = listOf(target1, target2, target3),
        sources = listOf(target1Sources, target2Sources, target3Sources),
        resources = emptyList(),
        dependenciesSources = emptyList(),
      )

      // when
      val magicMetaModel = MagicMetaModelImpl(testMagicMetaModelProjectConfig, projectDetails)

      WriteCommandAction.runWriteCommandAction(project) {
        magicMetaModel.loadDefaultTargets()
      }
      val loadedTargetsAfterFirstDefaultLoading = magicMetaModel.getAllLoadedTargets()
      val notLoadedTargetsAfterFirstDefaultLoading = magicMetaModel.getAllNotLoadedTargets()

      val notLoadedTargetByDefault = notLoadedTargetsAfterFirstDefaultLoading.first()
      WriteCommandAction.runWriteCommandAction(project) {
        magicMetaModel.loadTarget(notLoadedTargetByDefault.id)
      }
      val loadedTargetsAfterLoading = magicMetaModel.getAllLoadedTargets()
      val notLoadedTargetsAfterLoading = magicMetaModel.getAllNotLoadedTargets()

      WriteCommandAction.runWriteCommandAction(project) {
        magicMetaModel.loadDefaultTargets()
      }
      val loadedTargetsAfterSecondDefaultLoading = magicMetaModel.getAllLoadedTargets()
      val notLoadedTargetsAfterSecondDefaultLoading = magicMetaModel.getAllNotLoadedTargets()

      // then
      val targetsWithSharedSources = listOf(target2, target3)

      // first .loadDefaultTargets()
      loadedTargetsAfterFirstDefaultLoading shouldHaveSize 2
      loadedTargetsAfterFirstDefaultLoading shouldContain target1
      loadedTargetsAfterFirstDefaultLoading shouldContainAnyOf targetsWithSharedSources

      notLoadedTargetsAfterFirstDefaultLoading shouldHaveSize 1
      notLoadedTargetsAfterFirstDefaultLoading shouldContainAnyOf targetsWithSharedSources

      loadedTargetsAfterFirstDefaultLoading shouldNotContainAnyOf notLoadedTargetsAfterFirstDefaultLoading

      // after .loadTarget()
      loadedTargetsAfterLoading shouldContainExactlyInAnyOrder listOf(target1, notLoadedTargetByDefault)

      notLoadedTargetsAfterLoading shouldHaveSize 1
      notLoadedTargetsAfterLoading shouldNotContainAnyOf listOf(target1, notLoadedTargetByDefault)

      // second .loadDefaultTargets(), targets should be the same as after the first call
      loadedTargetsAfterSecondDefaultLoading shouldContainExactlyInAnyOrder loadedTargetsAfterFirstDefaultLoading
      notLoadedTargetsAfterSecondDefaultLoading shouldContainExactlyInAnyOrder notLoadedTargetsAfterFirstDefaultLoading
    }
  }

  @Nested
  @DisplayName("magicMetaModelImpl.loadTarget(targetId) tests")
  inner class MagicMetaModelImplLoadTargetTest {

    @Test
    fun `should throw IllegalArgumentException for not existing target`() {
      // given
      val target1Id = BuildTargetIdentifier("//target1")
      val target1 = BuildTarget(
        target1Id,
        emptyList(),
        listOf("kotlin"),
        listOf(
          BuildTargetIdentifier("@maven//:dep1.1"),
          BuildTargetIdentifier("@maven//:dep1.2"),
        ),
        BuildTargetCapabilities(),
      )

      val source1InTarget1 = SourceItem(
        "file:///file1/in/target1",
        SourceItemKind.FILE,
        false
      )
      val target1Sources = SourcesItem(
        target1Id,
        listOf(source1InTarget1),
      )

      val projectDetails = ProjectDetails(
        targetsId = listOf(target1Id),
        targets = listOf(target1),
        sources = listOf(target1Sources),
        resources = emptyList(),
        dependenciesSources = emptyList(),
      )

      // when
      val magicMetaModel = MagicMetaModelImpl(testMagicMetaModelProjectConfig, projectDetails)

      val notExistingTargetId = BuildTargetIdentifier("//not/existing/target")

      // then
      val exception = shouldThrowExactly<IllegalArgumentException> { magicMetaModel.loadTarget(notExistingTargetId) }
      exception.message shouldBe "Target $notExistingTargetId is not included in the model."
    }

    @Test
    fun `should load target`() {
      // given
      val target1Id = BuildTargetIdentifier("//target1")
      val target1 = BuildTarget(
        target1Id,
        emptyList(),
        listOf("kotlin"),
        listOf(
          BuildTargetIdentifier("@maven//:dep1.1"),
          BuildTargetIdentifier("@maven//:dep1.2"),
        ),
        BuildTargetCapabilities(),
      )

      val target2Id = BuildTargetIdentifier("//target2")
      val target2 = BuildTarget(
        target2Id,
        emptyList(),
        listOf("kotlin"),
        listOf(BuildTargetIdentifier("@maven//:dep2.1")),
        BuildTargetCapabilities(),
      )

      val source1InTarget1 = SourceItem(
        "file:///file1/in/target1",
        SourceItemKind.FILE,
        false
      )
      val target1Sources = SourcesItem(
        target1Id,
        listOf(source1InTarget1),
      )

      val source1InTarget2 = SourceItem(
        "file:///file1/in/target2",
        SourceItemKind.FILE,
        false
      )
      val target2Sources = SourcesItem(
        target2Id,
        listOf(source1InTarget2),
      )

      val projectDetails = ProjectDetails(
        targetsId = listOf(target1Id, target2Id),
        targets = listOf(target1, target2),
        sources = listOf(target1Sources, target2Sources),
        resources = emptyList(),
        dependenciesSources = emptyList(),
      )

      // when
      val magicMetaModel = MagicMetaModelImpl(testMagicMetaModelProjectConfig, projectDetails)

      WriteCommandAction.runWriteCommandAction(project) {
        magicMetaModel.loadTarget(target1Id)
      }

      val loadedTargets = magicMetaModel.getAllLoadedTargets()
      val notLoadedTargets = magicMetaModel.getAllNotLoadedTargets()

      // then
      loadedTargets shouldBe listOf(target1)
      notLoadedTargets shouldBe listOf(target2)
    }

    @Test
    fun `should add already loaded target without state change`() {
      // given
      val target1Id = BuildTargetIdentifier("//target1")
      val target1 = BuildTarget(
        target1Id,
        emptyList(),
        listOf("kotlin"),
        listOf(
          BuildTargetIdentifier("@maven//:dep1.1"),
          BuildTargetIdentifier("@maven//:dep1.2"),
        ),
        BuildTargetCapabilities(),
      )

      val target2Id = BuildTargetIdentifier("//target2")
      val target2 = BuildTarget(
        target2Id,
        emptyList(),
        listOf("kotlin"),
        listOf(BuildTargetIdentifier("@maven//:dep2.1")),
        BuildTargetCapabilities(),
      )

      val source1InTarget1 = SourceItem(
        "file:///file1/in/target1",
        SourceItemKind.FILE,
        false
      )
      val target1Sources = SourcesItem(
        target1Id,
        listOf(source1InTarget1),
      )

      val source1InTarget2 = SourceItem(
        "file:///file1/in/target2",
        SourceItemKind.FILE,
        false
      )
      val target2Sources = SourcesItem(
        target2Id,
        listOf(source1InTarget2),
      )

      val projectDetails = ProjectDetails(
        targetsId = listOf(target1Id, target2Id),
        targets = listOf(target1, target2),
        sources = listOf(target1Sources, target2Sources),
        resources = emptyList(),
        dependenciesSources = emptyList(),
      )

      // when
      val magicMetaModel = MagicMetaModelImpl(testMagicMetaModelProjectConfig, projectDetails)

      WriteCommandAction.runWriteCommandAction(project) {
        magicMetaModel.loadTarget(target1Id)
        magicMetaModel.loadTarget(target1Id)
      }

      val loadedTargets = magicMetaModel.getAllLoadedTargets()
      val notLoadedTargets = magicMetaModel.getAllNotLoadedTargets()

      // then
      loadedTargets shouldBe listOf(target1)
      notLoadedTargets shouldBe listOf(target2)
    }

    @Test
    fun `should add targets without overlapping`() {
      // given
      val target1Id = BuildTargetIdentifier("//target1")
      val target1 = BuildTarget(
        target1Id,
        emptyList(),
        listOf("kotlin"),
        listOf(
          BuildTargetIdentifier("@maven//:dep1.1"),
          BuildTargetIdentifier("@maven//:dep1.2"),
        ),
        BuildTargetCapabilities(),
      )

      val target2Id = BuildTargetIdentifier("//target2")
      val target2 = BuildTarget(
        target2Id,
        emptyList(),
        listOf("kotlin"),
        listOf(BuildTargetIdentifier("@maven//:dep2.1")),
        BuildTargetCapabilities(),
      )

      val source1InTarget1 = SourceItem(
        "file:///file1/in/target1",
        SourceItemKind.FILE,
        false
      )
      val target1Sources = SourcesItem(
        target1Id,
        listOf(source1InTarget1),
      )

      val source1InTarget2 = SourceItem(
        "file:///file1/in/target2",
        SourceItemKind.FILE,
        false
      )
      val target2Sources = SourcesItem(
        target2Id,
        listOf(source1InTarget2),
      )

      val projectDetails = ProjectDetails(
        targetsId = listOf(target1Id, target2Id),
        targets = listOf(target1, target2),
        sources = listOf(target1Sources, target2Sources),
        resources = emptyList(),
        dependenciesSources = emptyList(),
      )

      // when
      val magicMetaModel = MagicMetaModelImpl(testMagicMetaModelProjectConfig, projectDetails)

      WriteCommandAction.runWriteCommandAction(project) {
        magicMetaModel.loadTarget(target1Id)
        magicMetaModel.loadTarget(target2Id)
      }

      val loadedTargets = magicMetaModel.getAllLoadedTargets()
      val notLoadedTargets = magicMetaModel.getAllNotLoadedTargets()

      // then
      loadedTargets shouldContainExactlyInAnyOrder listOf(target1, target2)
      notLoadedTargets shouldBe emptyList()
    }

    @Test
    fun `should add target and remove overlapping targets`() {
      // given
      val target1Id = BuildTargetIdentifier("//target1")
      val target1 = BuildTarget(
        target1Id,
        emptyList(),
        listOf("kotlin"),
        listOf(
          BuildTargetIdentifier("@maven//:dep1.1"),
          BuildTargetIdentifier("@maven//:dep1.2"),
        ),
        BuildTargetCapabilities(),
      )

      val target2Id = BuildTargetIdentifier("//target2")
      val target2 = BuildTarget(
        target2Id,
        emptyList(),
        listOf("kotlin"),
        listOf(BuildTargetIdentifier("@maven//:dep2.1")),
        BuildTargetCapabilities(),
      )
      val target3Id = BuildTargetIdentifier("//target3")
      val target3 = BuildTarget(
        target3Id,
        emptyList(),
        listOf("kotlin"),
        emptyList(),
        BuildTargetCapabilities(),
      )

      val overlappingSourceInTarget1Target2 = SourceItem(
        "file:///overlapping/file/in/target1/target2",
        SourceItemKind.FILE,
        false
      )
      val overlappingSourceInTarget1Target3 = SourceItem(
        "file:///overlapping/file/in/target1/target3",
        SourceItemKind.FILE,
        false
      )

      val source1InTarget1 = SourceItem(
        "file:///file1/in/target1",
        SourceItemKind.FILE,
        false
      )
      val target1Sources = SourcesItem(
        target1Id,
        listOf(source1InTarget1, overlappingSourceInTarget1Target2, overlappingSourceInTarget1Target3),
      )

      val source1InTarget2 = SourceItem(
        "file:///file1/in/target2",
        SourceItemKind.FILE,
        false
      )
      val target2Sources = SourcesItem(
        target2Id,
        listOf(source1InTarget2, overlappingSourceInTarget1Target2),
      )

      val target3Sources = SourcesItem(
        target3Id,
        listOf(overlappingSourceInTarget1Target3),
      )

      val projectDetails = ProjectDetails(
        targetsId = listOf(target1Id, target2Id, target3Id),
        targets = listOf(target1, target2, target3),
        sources = listOf(target1Sources, target2Sources, target3Sources),
        resources = emptyList(),
        dependenciesSources = emptyList(),
      )

      // when
      val magicMetaModel = MagicMetaModelImpl(testMagicMetaModelProjectConfig, projectDetails)

      WriteCommandAction.runWriteCommandAction(project) {
        magicMetaModel.loadTarget(target2Id)
        magicMetaModel.loadTarget(target3Id)
      }

      val loadedTargetsAfterFirstLoading = magicMetaModel.getAllLoadedTargets()
      val notLoadedTargetsAfterFirstLoading = magicMetaModel.getAllNotLoadedTargets()

      WriteCommandAction.runWriteCommandAction(project) {
        magicMetaModel.loadTarget(target1Id)
      }

      val loadedTargetsAfterSecondLoading = magicMetaModel.getAllLoadedTargets()
      val notLoadedTargetsAfterSecondLoading = magicMetaModel.getAllNotLoadedTargets()

      // then
      loadedTargetsAfterFirstLoading shouldContainExactlyInAnyOrder listOf(target2, target3)
      notLoadedTargetsAfterFirstLoading shouldBe listOf(target1)

      loadedTargetsAfterSecondLoading shouldBe listOf(target1)
      notLoadedTargetsAfterSecondLoading shouldContainExactlyInAnyOrder listOf(target2, target3)
    }
  }

  @Nested
  @DisplayName("magicMetaModelImpl.getTargetsDetailsForDocument(documentId) tests")
  inner class MagicMetaModelImplGetTargetsDetailsForDocumentTest {

    @Test
    fun `should return no loaded target and no not loaded targets for not existing document`() {
      // given
      val target1Id = BuildTargetIdentifier("//target1")
      val target1 = BuildTarget(
        target1Id,
        emptyList(),
        listOf("kotlin"),
        listOf(
          BuildTargetIdentifier("@maven//:dep1.1"),
          BuildTargetIdentifier("@maven//:dep1.2"),
        ),
        BuildTargetCapabilities(),
      )

      val source1InTarget1 = SourceItem(
        "file:///file1/in/target1",
        SourceItemKind.FILE,
        false
      )
      val target1Sources = SourcesItem(
        target1Id,
        listOf(source1InTarget1),
      )

      val projectDetails = ProjectDetails(
        targetsId = listOf(target1Id),
        targets = listOf(target1),
        sources = listOf(target1Sources),
        resources = emptyList(),
        dependenciesSources = emptyList(),
      )

      // when
      val magicMetaModel = MagicMetaModelImpl(testMagicMetaModelProjectConfig, projectDetails)

      val notExistingDocumentId = TextDocumentIdentifier("file:///not/existing/document")

      val targetsDetails = magicMetaModel.getTargetsDetailsForDocument(notExistingDocumentId)

      // then
      targetsDetails.loadedTargetId shouldBe null
      targetsDetails.notLoadedTargetsIds shouldBe emptyList()
    }

    @Test
    fun `should return no loaded target for model without loaded targets`() {
      // given
      val target1Id = BuildTargetIdentifier("//target1")
      val target1 = BuildTarget(
        target1Id,
        emptyList(),
        listOf("kotlin"),
        listOf(
          BuildTargetIdentifier("@maven//:dep1.1"),
          BuildTargetIdentifier("@maven//:dep1.2"),
        ),
        BuildTargetCapabilities(),
      )

      val target2Id = BuildTargetIdentifier("//target2")
      val target2 = BuildTarget(
        target2Id,
        emptyList(),
        listOf("kotlin"),
        emptyList(),
        BuildTargetCapabilities(),
      )

      val sourceInTarget1Target2Uri = Files.createTempFile("file-in-target1-target2", "File.java").toUri().toString()
      val sourceInTarget1Target2 = SourceItem(
        sourceInTarget1Target2Uri,
        SourceItemKind.FILE,
        false
      )
      val target1Sources = SourcesItem(
        target1Id,
        listOf(sourceInTarget1Target2),
      )
      val target2Sources = SourcesItem(
        target2Id,
        listOf(sourceInTarget1Target2),
      )

      val projectDetails = ProjectDetails(
        targetsId = listOf(target1Id, target2Id),
        targets = listOf(target1, target2),
        sources = listOf(target1Sources, target2Sources),
        resources = emptyList(),
        dependenciesSources = emptyList(),
      )

      // when
      val magicMetaModel = MagicMetaModelImpl(testMagicMetaModelProjectConfig, projectDetails)

      val sourceInTarget1Target2Id = TextDocumentIdentifier(sourceInTarget1Target2Uri)

      val targetsDetails = magicMetaModel.getTargetsDetailsForDocument(sourceInTarget1Target2Id)

      // then
      targetsDetails.loadedTargetId shouldBe null
      targetsDetails.notLoadedTargetsIds shouldContainExactlyInAnyOrder listOf(target1Id, target2Id)
    }

    @Test
    fun `should return loaded target for non overlapping targets after loading default targets (all targets)`() {
      // given
      val target1Id = BuildTargetIdentifier("//target1")
      val target1 = BuildTarget(
        target1Id,
        emptyList(),
        listOf("kotlin"),
        listOf(
          BuildTargetIdentifier("@maven//:dep1.1"),
          BuildTargetIdentifier("@maven//:dep1.2"),
        ),
        BuildTargetCapabilities(),
      )

      val target2Id = BuildTargetIdentifier("//target2")
      val target2 = BuildTarget(
        target2Id,
        emptyList(),
        listOf("kotlin"),
        emptyList(),
        BuildTargetCapabilities(),
      )

      val source1InTarget1Uri = Files.createTempFile("file-in-target1", "File.java").toUri().toString()
      val source1InTarget1 = SourceItem(
        source1InTarget1Uri,
        SourceItemKind.FILE,
        false
      )
      val target1Sources = SourcesItem(
        target1Id,
        listOf(source1InTarget1),
      )
      // TODO
      target1Sources.roots = emptyList()

      val source1InTarget2Uri = Files.createTempFile("file-in-target2", "File.java").toUri().toString()
      val source1InTarget2 = SourceItem(
        source1InTarget2Uri,
        SourceItemKind.FILE,
        false
      )
      val target2Sources = SourcesItem(
        target2Id,
        listOf(source1InTarget2),
      )
      // TODO
      target2Sources.roots = emptyList()

      val projectDetails = ProjectDetails(
        targetsId = listOf(target1Id, target2Id),
        targets = listOf(target1, target2),
        sources = listOf(target1Sources, target2Sources),
        resources = emptyList(),
        dependenciesSources = emptyList(),
      )

      // when
      val magicMetaModel = MagicMetaModelImpl(testMagicMetaModelProjectConfig, projectDetails)
      WriteCommandAction.runWriteCommandAction(project) {
        magicMetaModel.loadDefaultTargets()
      }

      val source1InTarget1Id = TextDocumentIdentifier(source1InTarget1Uri)
      val source1InTarget2Id = TextDocumentIdentifier(source1InTarget2Uri)

      val source1InTarget1TargetsDetails = magicMetaModel.getTargetsDetailsForDocument(source1InTarget1Id)
      val source1InTarget2TargetsDetails = magicMetaModel.getTargetsDetailsForDocument(source1InTarget2Id)

      // then
      source1InTarget1TargetsDetails.loadedTargetId shouldBe target1Id
      source1InTarget1TargetsDetails.notLoadedTargetsIds shouldBe emptyList()

      source1InTarget2TargetsDetails.loadedTargetId shouldBe target2Id
      source1InTarget2TargetsDetails.notLoadedTargetsIds shouldBe emptyList()
    }

    @Test
    fun `should return loaded target for source in loaded target and no loaded target for source in not loaded target for model with overlapping targets`() {
      // given
      val target1Id = BuildTargetIdentifier("//target1")
      val target1 = BuildTarget(
        target1Id,
        emptyList(),
        listOf("kotlin"),
        listOf(
          BuildTargetIdentifier("@maven//:dep1.1"),
          BuildTargetIdentifier("@maven//:dep1.2"),
        ),
        BuildTargetCapabilities(),
      )

      val target2Id = BuildTargetIdentifier("//target2")
      val target2 = BuildTarget(
        target2Id,
        emptyList(),
        listOf("kotlin"),
        emptyList(),
        BuildTargetCapabilities(),
      )

      val overlappingSource1InTarget1Target2Uri = "file:///file/in/target1/target2"
      val source1InTarget1Target2 = SourceItem(
        overlappingSource1InTarget1Target2Uri,
        SourceItemKind.FILE,
        false
      )
      val target1Sources = SourcesItem(
        target1Id,
        listOf(source1InTarget1Target2),
      )

      val source1InTarget2Uri = "file:///file1/in/target2"
      val source1InTarget2 = SourceItem(
        source1InTarget2Uri,
        SourceItemKind.FILE,
        false
      )
      val target2Sources = SourcesItem(
        target2Id,
        listOf(source1InTarget2, source1InTarget1Target2),
      )

      val projectDetails = ProjectDetails(
        targetsId = listOf(target1Id, target2Id),
        targets = listOf(target1, target2),
        sources = listOf(target1Sources, target2Sources),
        resources = emptyList(),
        dependenciesSources = emptyList(),
      )

      // when
      val magicMetaModel = MagicMetaModelImpl(testMagicMetaModelProjectConfig, projectDetails)
      WriteCommandAction.runWriteCommandAction(project) {
        magicMetaModel.loadTarget(target1Id)
      }

      val sourceInTarget1Target2Id = TextDocumentIdentifier(overlappingSource1InTarget1Target2Uri)
      val source1InTarget2UriId = TextDocumentIdentifier(source1InTarget2Uri)

      val sourceInTarget1Target2TargetsDetails = magicMetaModel.getTargetsDetailsForDocument(sourceInTarget1Target2Id)
      val source1InTarget2TargetsDetails = magicMetaModel.getTargetsDetailsForDocument(source1InTarget2UriId)

      // then
      sourceInTarget1Target2TargetsDetails.loadedTargetId shouldBe target1Id
      sourceInTarget1Target2TargetsDetails.notLoadedTargetsIds shouldBe listOf(target2Id)

      source1InTarget2TargetsDetails.loadedTargetId shouldBe null
      source1InTarget2TargetsDetails.notLoadedTargetsIds shouldBe listOf(target2Id)
    }
  }
}
