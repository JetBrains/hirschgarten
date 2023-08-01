package org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import ch.epfl.scala.bsp4j.BuildTarget
import ch.epfl.scala.bsp4j.BuildTargetCapabilities
import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import io.kotest.matchers.shouldBe
import org.jetbrains.magicmetamodel.DefaultModuleNameProvider
import org.jetbrains.magicmetamodel.impl.workspacemodel.BuildTargetId
import org.jetbrains.magicmetamodel.impl.workspacemodel.ModuleDependency
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("buildTargetToModuleDependencyTransformer.transform(buildTarget) tests")
class BuildTargetToModuleDependencyTransformerTest {

  @Test
  fun `should return no module dependencies for no dependencies`() {
    // given
    val emptyBuildTargets = listOf<BuildTarget>()
    val allTargets = setOf<BuildTargetId>()

    // when
    val buildTargetToModuleDependencyTransformer =
      BuildTargetToModuleDependencyTransformer(allTargets, DefaultModuleNameProvider)
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
      BuildTargetToModuleDependencyTransformer(allTargets, DefaultModuleNameProvider)
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

    // when
    val buildTargetToModuleDependencyTransformer =
      BuildTargetToModuleDependencyTransformer(allTargets, DefaultModuleNameProvider)
    val moduleDependencies = buildTargetToModuleDependencyTransformer.transform(buildTarget)

    // then
    val expectedModuleDependency = ModuleDependency(
      moduleName = "//target2"
    )

    moduleDependencies shouldBe listOf(expectedModuleDependency)
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

    // when
    val buildTargetToModuleDependencyTransformer =
      BuildTargetToModuleDependencyTransformer(allTargets, DefaultModuleNameProvider)
    val moduleDependencies = buildTargetToModuleDependencyTransformer.transform(buildTarget)

    // then
    val expectedModuleDependency1 = ModuleDependency(
      moduleName = "//target2"
    )
    val expectedModuleDependency2 = ModuleDependency(
      moduleName = "//target3"
    )
    val expectedModuleDependency3 = ModuleDependency(
      moduleName = "//target4"
    )

    moduleDependencies shouldBe listOf(expectedModuleDependency1, expectedModuleDependency2, expectedModuleDependency3)
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

    // when
    val buildTargetToModuleDependencyTransformer =
      BuildTargetToModuleDependencyTransformer(allTargets, DefaultModuleNameProvider)
    val moduleDependencies = buildTargetToModuleDependencyTransformer.transform(buildTargets)

    // then
    val expectedModuleDependency1 = ModuleDependency(
      moduleName = "//target2"
    )
    val expectedModuleDependency2 = ModuleDependency(
      moduleName = "//target3"
    )
    val expectedModuleDependency3 = ModuleDependency(
      moduleName = "//target4"
    )

    moduleDependencies shouldBe listOf(expectedModuleDependency1, expectedModuleDependency2, expectedModuleDependency3)
  }
}
