@file:Suppress("LongMethod")
package org.jetbrains.magicmetamodel.impl

import ch.epfl.scala.bsp4j.BuildTarget
import ch.epfl.scala.bsp4j.BuildTargetCapabilities
import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.DependencySourcesItem
import ch.epfl.scala.bsp4j.ResourcesItem
import ch.epfl.scala.bsp4j.SourceItem
import ch.epfl.scala.bsp4j.SourceItemKind
import ch.epfl.scala.bsp4j.SourcesItem
import io.kotest.matchers.shouldBe
import org.jetbrains.magicmetamodel.ProjectDetails
import org.jetbrains.magicmetamodel.impl.workspacemodel.ModuleDetails
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("TargetIdToModuleDetails(projectDetails) tests")
class TargetIdToModuleDetailsTest {

  @Test
  fun `should return empty map for empty project details`() {
    // given
    val emptyProjectDetails = ProjectDetails(
      targetsId = emptyList(),
      targets = setOf(),
      sources = emptyList(),
      resources = emptyList(),
      dependenciesSources = emptyList(),
    )

    // when
    val targetIdToModuleDetails = TargetIdToModuleDetails(emptyProjectDetails)

    // then
    targetIdToModuleDetails shouldBe emptyMap()
  }

  @Test
  fun `should return map with one element only with target for project details without matching target details`() {
    // given
    val targetId = BuildTargetIdentifier("target")
    val target = BuildTarget(
      targetId,
      emptyList(),
      emptyList(),
      emptyList(),
      BuildTargetCapabilities(),
    )
    val projectDetails = ProjectDetails(
      targetsId = listOf(targetId),
      targets = setOf(target),
      sources = emptyList(),
      resources = emptyList(),
      dependenciesSources = emptyList(),
    )

    // when
    val targetIdToModuleDetails = TargetIdToModuleDetails(projectDetails)

    // then
    val expectedModuleDetails = ModuleDetails(
      target = target,
      allTargetsIds = listOf(targetId),
      sources = emptyList(),
      resources = emptyList(),
      dependenciesSources = emptyList(),
    )

    targetIdToModuleDetails shouldBe mapOf(
      targetId to expectedModuleDetails
    )
  }

  @Test
  fun `should return map with one element for project details with one target`() {
    // given
    val targetId = BuildTargetIdentifier("target")
    val target = BuildTarget(
      targetId,
      emptyList(),
      emptyList(),
      listOf(BuildTargetIdentifier("@maven//:test")),
      BuildTargetCapabilities(),
    )
    val targetSources = SourcesItem(
      targetId,
      listOf(SourceItem("file:///root/dir/example/package/", SourceItemKind.DIRECTORY, false)),
    )
    val targetResources = ResourcesItem(
      targetId,
      listOf("file:///root/dir/resource/File.txt"),
    )
    val targetDependencySources = DependencySourcesItem(
      targetId,
      listOf("file:///lib/test/1.0.0/test-sources.jar"),
    )
    val projectDetails = ProjectDetails(
      targetsId = listOf(targetId),
      targets = setOf(target),
      sources = listOf(targetSources),
      resources = listOf(targetResources),
      dependenciesSources = listOf(targetDependencySources),
    )

    // when
    val targetIdToModuleDetails = TargetIdToModuleDetails(projectDetails)

    // then
    val expectedModuleDetails = ModuleDetails(
      target = target,
      allTargetsIds = listOf(targetId),
      sources = listOf(targetSources),
      resources = listOf(targetResources),
      dependenciesSources = listOf(targetDependencySources),
    )

    targetIdToModuleDetails shouldBe mapOf(
      targetId to expectedModuleDetails
    )
  }

  @Test
  fun `should return map with multiple elements for project details with multiple targets`() {
    // given
    val target1Id = BuildTargetIdentifier("target1")
    val target1 = BuildTarget(
      target1Id,
      emptyList(),
      emptyList(),
      listOf(BuildTargetIdentifier("target2"), BuildTargetIdentifier("@maven//:test")),
      BuildTargetCapabilities(),
    )
    val target1Sources = SourcesItem(
      target1Id,
      listOf(SourceItem("file:///root/dir1/example/package/", SourceItemKind.DIRECTORY, false)),
    )
    val target1Resources = ResourcesItem(
      target1Id,
      listOf("file:///root/dir1/resource/File.txt"),
    )
    val target1DependencySources = DependencySourcesItem(
      target1Id,
      listOf("file:///lib/test/1.0.0/test-sources.jar"),
    )

    val target2Id = BuildTargetIdentifier("target2")
    val target2 = BuildTarget(
      target2Id,
      emptyList(),
      emptyList(),
      listOf(BuildTargetIdentifier("@maven//:test")),
      BuildTargetCapabilities(),
    )
    val target2Sources1 = SourcesItem(
      target2Id,
      listOf(
        SourceItem("file:///root/dir2/example/package/File1.java", SourceItemKind.FILE, false),
      ),
    )
    val target2Sources2 = SourcesItem(
      target2Id,
      listOf(
        SourceItem("file:///root/dir2/example/package/File2.java", SourceItemKind.FILE, false),
      ),
    )
    val target2Resources = ResourcesItem(
      target2Id,
      listOf("file:///root/dir2/resource/File.txt"),
    )
    val target2DependencySources = DependencySourcesItem(
      target2Id,
      listOf("file:///lib/test/2.0.0/test-sources.jar"),
    )

    val target3Id = BuildTargetIdentifier("target3")
    val target3 = BuildTarget(
      target3Id,
      emptyList(),
      emptyList(),
      listOf(BuildTargetIdentifier("target2")),
      BuildTargetCapabilities(),
    )
    val target3Sources = SourcesItem(
      target3Id,
      emptyList(),
    )

    val projectDetails = ProjectDetails(
      targetsId = listOf(target1Id, target3Id, target2Id),
      targets = setOf(target2, target1, target3),
      sources = listOf(target3Sources, target2Sources1, target1Sources, target2Sources2),
      resources = listOf(target1Resources, target2Resources),
      dependenciesSources = listOf(target2DependencySources, target1DependencySources),
    )

    // when
    val targetIdToModuleDetails = TargetIdToModuleDetails(projectDetails)

    // then
    val expectedModuleDetails1 = ModuleDetails(
      target = target1,
      allTargetsIds = listOf(target1Id, target3Id, target2Id),
      sources = listOf(target1Sources),
      resources = listOf(target1Resources),
      dependenciesSources = listOf(target1DependencySources),
    )
    val expectedModuleDetails2 = ModuleDetails(
      target = target2,
      allTargetsIds = listOf(target1Id, target3Id, target2Id),
      sources = listOf(target2Sources1, target2Sources2),
      resources = listOf(target2Resources),
      dependenciesSources = listOf(target2DependencySources),
    )
    val expectedModuleDetails3 = ModuleDetails(
      target = target3,
      allTargetsIds = listOf(target1Id, target3Id, target2Id),
      sources = listOf(target3Sources),
      resources = emptyList(),
      dependenciesSources = emptyList(),
    )

    targetIdToModuleDetails shouldBe mapOf(
      target1Id to expectedModuleDetails1,
      target2Id to expectedModuleDetails2,
      target3Id to expectedModuleDetails3,
    )
  }
}
