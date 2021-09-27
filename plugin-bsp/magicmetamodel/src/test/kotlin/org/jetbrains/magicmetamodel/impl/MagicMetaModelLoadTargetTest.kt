@file:Suppress("LongMethod")

package org.jetbrains.magicmetamodel.impl

import ch.epfl.scala.bsp4j.BuildTarget
import ch.epfl.scala.bsp4j.BuildTargetCapabilities
import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.SourceItem
import ch.epfl.scala.bsp4j.SourceItemKind
import ch.epfl.scala.bsp4j.SourcesItem
import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("magicMetaModel.loadTarget(targetId) tests")
class MagicMetaModelLoadTargetTest {

  @Test
  @Suppress
  fun `should throw IllegalArgumentException for not existing target`() {
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
    val targets = listOf(target1)

    val source1InTarget1 = SourceItem(
      "file:///file1/in/target1",
      SourceItemKind.FILE,
      false
    )
    val target1Sources = SourcesItem(
      target1Id,
      listOf(source1InTarget1),
    )
    val sources = listOf(target1Sources)

    // when
    val magicMetaModel = MagicMetaModelImpl(workspaceModel, targets, sources)

    val notExistingTargetId = BuildTargetIdentifier("//not/existing/target")

    // then
    val exception = shouldThrowExactly<IllegalArgumentException> { magicMetaModel.loadTarget(notExistingTargetId) }
    exception.message shouldBe "Target $notExistingTargetId is not included in the model."
  }

  @Test
  fun `should load target`() {
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

    magicMetaModel.loadTarget(target1Id)

    val loadedTargets = magicMetaModel.getAllLoadedTargets()
    val notLoadedTargets = magicMetaModel.getAllNotLoadedTargets()

    // then
    loadedTargets shouldBe listOf(target1)
    notLoadedTargets shouldBe listOf(target2)
  }

  @Test
  fun `should add already loaded target without state change`() {
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

    magicMetaModel.loadTarget(target1Id)
    magicMetaModel.loadTarget(target1Id)

    val loadedTargets = magicMetaModel.getAllLoadedTargets()
    val notLoadedTargets = magicMetaModel.getAllNotLoadedTargets()

    // then
    loadedTargets shouldBe listOf(target1)
    notLoadedTargets shouldBe listOf(target2)
  }

  @Test
  fun `should add targets without overlapping`() {
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

    magicMetaModel.loadTarget(target1Id)
    magicMetaModel.loadTarget(target2Id)

    val loadedTargets = magicMetaModel.getAllLoadedTargets()
    val notLoadedTargets = magicMetaModel.getAllNotLoadedTargets()

    // then
    loadedTargets shouldContainExactlyInAnyOrder listOf(target1, target2)
    notLoadedTargets shouldBe emptyList()
  }

  @Test
  fun `should add target and remove overlapping targets`() {
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
      emptyList(),
      BuildTargetCapabilities(),
    )
    val targets = listOf(target1, target2, target3)

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
    val sources = listOf(target1Sources, target2Sources, target3Sources)

    // when
    val magicMetaModel = MagicMetaModelImpl(workspaceModel, targets, sources)

    magicMetaModel.loadTarget(target2Id)
    magicMetaModel.loadTarget(target3Id)

    val loadedTargetsAfterFirstLoading = magicMetaModel.getAllLoadedTargets()
    val notLoadedTargetsAfterFirstLoading = magicMetaModel.getAllNotLoadedTargets()

    magicMetaModel.loadTarget(target1Id)

    val loadedTargetsAfterSecondLoading = magicMetaModel.getAllLoadedTargets()
    val notLoadedTargetsAfterSecondLoading = magicMetaModel.getAllNotLoadedTargets()

    // then
    loadedTargetsAfterFirstLoading shouldContainExactlyInAnyOrder listOf(target2, target3)
    notLoadedTargetsAfterFirstLoading shouldBe listOf(target1)

    loadedTargetsAfterSecondLoading shouldBe listOf(target1)
    notLoadedTargetsAfterSecondLoading shouldContainExactlyInAnyOrder listOf(target2, target3)
  }
}
