@file:Suppress("LongMethod")

package org.jetbrains.magicmetamodel

import ch.epfl.scala.bsp4j.BuildTarget
import ch.epfl.scala.bsp4j.BuildTargetCapabilities
import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.SourceItem
import ch.epfl.scala.bsp4j.SourceItemKind
import ch.epfl.scala.bsp4j.SourcesItem
import ch.epfl.scala.bsp4j.TextDocumentIdentifier
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import org.jetbrains.magicmetamodel.impl.WorkspaceModelTestMockImpl
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.test.fail

@DisplayName("MagicMetaModel \"real world\" flow tests")
class MagicMetaModelTest {

  @Test
  fun `should handle empty project (something strange happened)`() {
    // given
    val workspaceModel = WorkspaceModelTestMockImpl()
    val targets = emptyList<BuildTarget>()
    val sources = emptyList<SourcesItem>()

    // when & then
    val magicMetaModel = MagicMetaModel.create(workspaceModel, targets, sources)

    // showing loaded and not loaded targets to user (e.g. at the sidebar)
    magicMetaModel `should return given loaded and not loaded targets` Pair(targets, emptyList())

    magicMetaModel.loadDefaultTargets()

    // showing loaded and not loaded targets to user (e.g. at the sidebar)
    magicMetaModel.getAllLoadedTargets() shouldBe emptyList()
    magicMetaModel.getAllNotLoadedTargets() shouldBe emptyList()
  }

  @Test
  fun `should handle project without shared sources (like a simple kotlin project)`() {
    // given
    val workspaceModel = WorkspaceModelTestMockImpl()

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
    val targets = listOf(libA, libB, libC, app)

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
    val sources = listOf(libASources, libBSources, libCSources, appSources)

    // when & then
    val magicMetaModel = MagicMetaModel.create(workspaceModel, targets, sources)

    // showing loaded and not loaded targets to user (e.g. at the sidebar)
    magicMetaModel `should return given loaded and not loaded targets` Pair(emptyList(), targets)

    // after bsp importing process
    magicMetaModel.loadDefaultTargets()

    // showing loaded and not loaded targets to user (e.g. at the sidebar)
    magicMetaModel `should return given loaded and not loaded targets` Pair(targets, emptyList())

    // user opens each file and checks the loaded target for each file (e.g. at the bottom bar widget)
    magicMetaModel `should return valid targets details for document` Triple(sourceInLibAUri, libAId, emptyList())
    magicMetaModel `should return valid targets details for document` Triple(sourceInLibBUri, libBId, emptyList())
    magicMetaModel `should return valid targets details for document` Triple(sourceInLibCUri, libCId, emptyList())
    magicMetaModel `should return valid targets details for document` Triple(sourceInAppUri, appId, emptyList())
  }

  @Test
  fun `should handle project with shared sources (like a scala cross version project)`() {
    // given
    val workspaceModel = WorkspaceModelTestMockImpl()

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
    val targets = listOf(libA212, libB212, libB213, libC212, libC213, app212, app213)

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
    val sources = listOf(
      libA212Sources,
      libB212Sources,
      libB213Sources,
      libC212Sources,
      libC213Sources,
      app212Sources,
      app213Sources,
    )

    // when & then
    val magicMetaModel = MagicMetaModel.create(workspaceModel, targets, sources)

    // showing loaded and not loaded targets to user
    magicMetaModel `should return given loaded and not loaded targets` Pair(emptyList(), targets)

    // after bsp importing process
    magicMetaModel.loadDefaultTargets()

    // now we have only 2 valid default targets configuration (libA-2.12 has no shared sources):
    // 1. libA-2.12 + libB-2.12, libC-2.12, app-2.12
    // 2. libA-2.12 + libB-2.13, libC-2.13, app-2.13
    val appVersion = getAppTargetLoadedVersionOrThrow(app212, app213, magicMetaModel.getAllLoadedTargets())

    val (loadedLibBByDefault, notLoadedLibBByDefault) =
      getExpectedLoadedAndNotLoadedTargets(appVersion, libB212, libB213)
    val (loadedLibCByDefault, notLoadedLibCByDefault) =
      getExpectedLoadedAndNotLoadedTargets(appVersion, libC212, libC213)
    val (loadedAppByDefault, notLoadedAppByDefault) = getExpectedLoadedAndNotLoadedTargets(appVersion, app212, app213)

    // showing loaded and not loaded targets to user (e.g. at the sidebar)
    magicMetaModel `should return given loaded and not loaded targets` Pair(
      listOf(libA212, loadedLibBByDefault, loadedLibCByDefault, loadedAppByDefault),
      listOf(notLoadedLibBByDefault, notLoadedLibCByDefault, notLoadedAppByDefault),
    )

    // ------
    // user decides to load not loaded `:app-2.1*` target by default
    // (app-2.13 if app-2.12 is currently loaded or vice versa)
    // ------
    magicMetaModel.loadTarget(notLoadedAppByDefault.id)

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
    magicMetaModel.loadTarget(notLoadedLibBByDefault.id)

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
    magicMetaModel.loadDefaultTargets()

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

  private fun getAppTargetLoadedVersionOrThrow(
    app212: BuildTarget,
    app213: BuildTarget,
    loadedTargets: List<BuildTarget>,
  ): Int =
    when (Pair(loadedTargets.contains(app212), loadedTargets.contains(app213))) {
      Pair(true, false) -> 12
      Pair(false, true) -> 13
      else -> fail("Invalid loaded targets! Loaded targets should contain either ${app212.id} or ${app213.id}")
    }

  private fun getExpectedLoadedAndNotLoadedTargets(
    version: Int,
    target12: BuildTarget,
    target13: BuildTarget,
  ): Pair<BuildTarget, BuildTarget> =
    when (version) {
      12 -> Pair(target12, target13)
      13 -> Pair(target13, target12)
      else -> fail("Wrong version!")
    }

  private infix fun MagicMetaModel.`should return given loaded and not loaded targets`(
    expectedLoadedAndNotLoadedTargets: Pair<List<BuildTarget>, List<BuildTarget>>,
  ) {
    // then
    this.getAllLoadedTargets() shouldContainExactlyInAnyOrder expectedLoadedAndNotLoadedTargets.first
    this.getAllNotLoadedTargets() shouldContainExactlyInAnyOrder expectedLoadedAndNotLoadedTargets.second
  }

  private infix fun MagicMetaModel.`should return valid targets details for document`(
    documentUrlLoadedTargetAndNotLoadedTargets: Triple<String, BuildTargetIdentifier?, List<BuildTargetIdentifier>>,
  ) {
    val documentId = TextDocumentIdentifier(documentUrlLoadedTargetAndNotLoadedTargets.first)
    val targetDetails = this.getTargetsDetailsForDocument(documentId)

    // then
    targetDetails.loadedTargetId shouldBe documentUrlLoadedTargetAndNotLoadedTargets.second
    targetDetails.notLoadedTargetsIds shouldContainExactlyInAnyOrder documentUrlLoadedTargetAndNotLoadedTargets.third
  }
}
