@file:Suppress("LongMethod", "MaxLineLength")

package org.jetbrains.magicmetamodel.impl

import ch.epfl.scala.bsp4j.BuildTarget
import ch.epfl.scala.bsp4j.BuildTargetCapabilities
import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.SourceItem
import ch.epfl.scala.bsp4j.SourceItemKind
import ch.epfl.scala.bsp4j.SourcesItem
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAnyOf
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotContainAnyOf
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("magicMetaModel.loadDefaultTargets() tests")
class MagicMetaModelLoadDefaultTargetsTargetsTest {

  @Test
  fun `should return no loaded and no not loaded targets for empty project`() {
    // given
    val workspaceModel = WorkspaceModelTestMockImpl()
    val targets = emptyList<BuildTarget>()
    val sources = emptyList<SourcesItem>()

    // when
    val magicMetaModel = MagicMetaModelImpl(workspaceModel, targets, sources)
    magicMetaModel.loadDefaultTargets()

    val loadedTargets = magicMetaModel.getAllLoadedTargets()
    val notLoadedTargets = magicMetaModel.getAllNotLoadedTargets()

    // then
    loadedTargets shouldBe emptyList()
    notLoadedTargets shouldBe emptyList()
  }

  @Test
  fun `should return no loaded and all targets as not loaded for not initialized project (before calling loadDefaultTargets())`() {
    // given
    val workspaceModel = WorkspaceModelTestMockImpl()

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
    val targets = listOf(target1, target2)

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
    val sources = listOf(target1Sources, target2Sources)

    // when
    val magicMetaModel = MagicMetaModelImpl(workspaceModel, targets, sources)

    val loadedTargets = magicMetaModel.getAllLoadedTargets()
    val notLoadedTargets = magicMetaModel.getAllNotLoadedTargets()

    // then
    loadedTargets shouldBe emptyList()
    notLoadedTargets shouldContainExactlyInAnyOrder targets
  }

  @Test
  fun `should return all targets as loaded and no not loaded targets for project without shared sources`() {
    // given
    val workspaceModel = WorkspaceModelTestMockImpl()

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
    val targets = listOf(target1, target2, target3)

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
      "file:///dir1/in/target2/",
      SourceItemKind.DIRECTORY,
      false
    )
    val target2Sources = SourcesItem(
      target2Id,
      listOf(source1InTarget2),
    )

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

    val sources = listOf(target1Sources, target2Sources, target3Sources)

    // when
    val magicMetaModel = MagicMetaModelImpl(workspaceModel, targets, sources)
    magicMetaModel.loadDefaultTargets()

    val loadedTargets = magicMetaModel.getAllLoadedTargets()
    val notLoadedTargets = magicMetaModel.getAllNotLoadedTargets()

    // then
    loadedTargets shouldContainExactlyInAnyOrder targets
    notLoadedTargets shouldBe emptyList()
  }

  @Test
  fun `should return non overlapping loaded targets for project with shared sources`() {
    // given
    val workspaceModel = WorkspaceModelTestMockImpl()

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
    val targets = listOf(target1, target2, target3)

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

    val sources = listOf(target1Sources, target2Sources, target3Sources)

    // when
    val magicMetaModel = MagicMetaModelImpl(workspaceModel, targets, sources)
    magicMetaModel.loadDefaultTargets()

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
    val workspaceModel = WorkspaceModelTestMockImpl()

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
    val targets = listOf(target1, target2, target3)

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

    val sources = listOf(target1Sources, target2Sources, target3Sources)

    // when
    val magicMetaModel = MagicMetaModelImpl(workspaceModel, targets, sources)

    magicMetaModel.loadDefaultTargets()
    val loadedTargetsAfterFirstDefaultLoading = magicMetaModel.getAllLoadedTargets()
    val notLoadedTargetsAfterFirstDefaultLoading = magicMetaModel.getAllNotLoadedTargets()

    val notLoadedTargetByDefault = notLoadedTargetsAfterFirstDefaultLoading.first()
    magicMetaModel.loadTarget(notLoadedTargetByDefault.id)
    val loadedTargetsAfterLoading = magicMetaModel.getAllLoadedTargets()
    val notLoadedTargetsAfterLoading = magicMetaModel.getAllNotLoadedTargets()

    magicMetaModel.loadDefaultTargets()
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
