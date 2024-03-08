package org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import ch.epfl.scala.bsp4j.BuildTarget
import ch.epfl.scala.bsp4j.BuildTargetCapabilities
import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import io.kotest.matchers.shouldBe
import org.jetbrains.plugins.bsp.magicmetamodel.DefaultModuleNameProvider
import org.jetbrains.plugins.bsp.magicmetamodel.impl.toDefaultTargetsMap
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.BuildTargetId
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.IntermediateModuleDependency
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("buildTargetToModuleDependencyTransformer.transform(buildTarget) tests")
class BuildTargetToIntermediateModuleDependencyTransformerTest {
  @Test
  fun `should return no module dependencies for no dependencies`() {
    // given
    val emptyBuildTargets = listOf<BuildTarget>()
    val allTargets = setOf<BuildTargetId>()

    // when
    val buildTargetToModuleDependencyTransformer =
      BuildTargetToModuleDependencyTransformer(allTargets, mapOf(), DefaultModuleNameProvider)
    val moduleDependencies = buildTargetToModuleDependencyTransformer.transform(emptyBuildTargets)

    // then
    moduleDependencies shouldBe emptyList()
  }

  @Test
  fun `should return no module dependencies for no all targets`() {
    // given
    val buildTarget = BuildTarget(
      BuildTargetIdentifier("//target1"),
      emptyList(),
      emptyList(),
      listOf(
        BuildTargetIdentifier("//target2"),
        BuildTargetIdentifier("//target3"),
        BuildTargetIdentifier("@maven//:lib1"),
      ),
      BuildTargetCapabilities(),
    )
    val allTargets = setOf<BuildTargetId>()

    // when
    val buildTargetToModuleDependencyTransformer =
      BuildTargetToModuleDependencyTransformer(allTargets, mapOf(), DefaultModuleNameProvider)
    val moduleDependencies = buildTargetToModuleDependencyTransformer.transform(buildTarget)

    // then
    moduleDependencies shouldBe emptyList()
  }

  @Test
  fun `should return single module dependency for target with one dependency`() {
    // given
    val buildTarget = BuildTarget(
      BuildTargetIdentifier("//target1"),
      emptyList(),
      emptyList(),
      listOf(
        BuildTargetIdentifier("//target2"),
        BuildTargetIdentifier("@maven//:lib1"),
      ),
      BuildTargetCapabilities(),
    )
    val allTargets = setOf(
      "//target1",
      "//target2",
    )

    val targetsMap = allTargets.toDefaultTargetsMap()

    // when
    val buildTargetToModuleDependencyTransformer =
      BuildTargetToModuleDependencyTransformer(allTargets, targetsMap, DefaultModuleNameProvider)
    val moduleDependencies = buildTargetToModuleDependencyTransformer.transform(buildTarget)

    // then
    val expectedIntermediateModuleDependency = IntermediateModuleDependency(
      moduleName = "//target2",
    )

    moduleDependencies shouldBe listOf(expectedIntermediateModuleDependency)
  }

  @Test
  fun `should return multiple modules dependencies for target with multiple dependencies`() {
    // given
    val buildTarget = BuildTarget(
      BuildTargetIdentifier("//target1"),
      emptyList(),
      emptyList(),
      listOf(
        BuildTargetIdentifier("//target2"),
        BuildTargetIdentifier("//target3"),
        BuildTargetIdentifier("//target4"),
        BuildTargetIdentifier("@maven//:lib1"),
        BuildTargetIdentifier("@maven//:lib2"),
      ),
      BuildTargetCapabilities(),
    )
    val allTargets = setOf(
      "//target1",
      "//target2",
      "//target3",
      "//target4",
      "//target5",
    )

    val targetsMap = allTargets.toDefaultTargetsMap()

    // when
    val buildTargetToModuleDependencyTransformer =
      BuildTargetToModuleDependencyTransformer(allTargets, targetsMap, DefaultModuleNameProvider)
    val moduleDependencies = buildTargetToModuleDependencyTransformer.transform(buildTarget)

    // then
    val expectedIntermediateModuleDependency1 = IntermediateModuleDependency(
      moduleName = "//target2",
    )
    val expectedIntermediateModuleDependency2 = IntermediateModuleDependency(
      moduleName = "//target3",
    )
    val expectedIntermediateModuleDependency3 = IntermediateModuleDependency(
      moduleName = "//target4",
    )

    moduleDependencies shouldBe listOf(expectedIntermediateModuleDependency1, expectedIntermediateModuleDependency2, expectedIntermediateModuleDependency3)
  }

  @Test
  fun `should return multiple modules dependencies for multiple targets`() {
    // given
    val buildTarget1 = BuildTarget(
      BuildTargetIdentifier("//target1"),
      emptyList(),
      emptyList(),
      listOf(
        BuildTargetIdentifier("//target2"),
        BuildTargetIdentifier("//target3"),
        BuildTargetIdentifier("@maven//:lib1"),
        BuildTargetIdentifier("@maven//:lib2"),
      ),
      BuildTargetCapabilities(),
    )
    val buildTarget2 = BuildTarget(
      BuildTargetIdentifier("//target1"),
      emptyList(),
      emptyList(),
      listOf(
        BuildTargetIdentifier("//target3"),
        BuildTargetIdentifier("//target4"),
        BuildTargetIdentifier("@maven//:lib2"),
      ),
      BuildTargetCapabilities(),
    )

    val allTargets = setOf(
      "//target1",
      "//target2",
      "//target3",
      "//target4",
      "//target5",
    )

    val buildTargets = listOf(buildTarget1, buildTarget2)

    val targetsMap = allTargets.toDefaultTargetsMap()

    // when
    val buildTargetToModuleDependencyTransformer =
      BuildTargetToModuleDependencyTransformer(allTargets, targetsMap, DefaultModuleNameProvider)
    val moduleDependencies = buildTargetToModuleDependencyTransformer.transform(buildTargets)

    // then
    val expectedIntermediateModuleDependency1 = IntermediateModuleDependency(
      moduleName = "//target2",
    )
    val expectedIntermediateModuleDependency2 = IntermediateModuleDependency(
      moduleName = "//target3",
    )
    val expectedIntermediateModuleDependency3 = IntermediateModuleDependency(
      moduleName = "//target4",
    )

    moduleDependencies shouldBe listOf(expectedIntermediateModuleDependency1, expectedIntermediateModuleDependency2, expectedIntermediateModuleDependency3)
  }
}
