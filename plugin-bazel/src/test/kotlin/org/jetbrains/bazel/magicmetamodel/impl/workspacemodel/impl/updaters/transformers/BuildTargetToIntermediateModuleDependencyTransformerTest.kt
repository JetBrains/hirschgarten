package org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.magicmetamodel.impl.toDefaultTargetsMap
import org.jetbrains.bazel.workspace.model.test.framework.WorkspaceModelBaseTest
import org.jetbrains.bazel.workspacemodel.entities.IntermediateModuleDependency
import org.jetbrains.bsp.protocol.BuildTarget
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.io.path.Path

@DisplayName("buildTargetToModuleDependencyTransformer.transform(buildTarget) tests")
class BuildTargetToIntermediateModuleDependencyTransformerTest : WorkspaceModelBaseTest() {
  @Test
  fun `should return no module dependencies for no dependencies`() {
    // given
    val emptyBuildTargets = listOf<BuildTarget>()
    val allTargets = setOf<Label>()

    // when
    val buildTargetToModuleDependencyTransformer =
      BuildTargetToModuleDependencyTransformer(allTargets, mapOf(), project)
    val moduleDependencies = buildTargetToModuleDependencyTransformer.transform(emptyBuildTargets)

    // then
    moduleDependencies shouldBe emptyList()
  }

  @Test
  fun `should return no module dependencies for no all targets`() {
    // given
    val buildTarget =
      BuildTarget(
        Label.parse("//target1"),
        emptyList(),
        emptyList(),
        listOf(
          Label.parse("//target2"),
          Label.parse("//target3"),
          Label.parse("@maven//:lib1"),
        ),
        TargetKind(
          kindString = "java_binary",
          ruleType = RuleType.BINARY,
        ),
        sources = emptyList(),
        resources = emptyList(),
        baseDirectory = Path("base/dir"),
      )
    val allTargets = setOf<Label>()

    // when
    val buildTargetToModuleDependencyTransformer =
      BuildTargetToModuleDependencyTransformer(allTargets, mapOf(), project)
    val moduleDependencies = buildTargetToModuleDependencyTransformer.transform(buildTarget)

    // then
    moduleDependencies shouldBe emptyList()
  }

  @Test
  fun `should return single module dependency for target with one dependency`() {
    // given
    val buildTarget =
      BuildTarget(
        Label.parse("//target1"),
        emptyList(),
        emptyList(),
        listOf(
          Label.parse("//target2"),
          Label.parse("@maven//:lib1"),
        ),
        TargetKind(
          kindString = "java_binary",
          ruleType = RuleType.BINARY,
        ),
        sources = emptyList(),
        resources = emptyList(),
        baseDirectory = Path("base/dir"),
      )
    val allTargets =
      setOf(
        "@//target1",
        "@//target2",
      )

    val targetsMap = allTargets.toDefaultTargetsMap()

    // when
    val buildTargetToModuleDependencyTransformer =
      BuildTargetToModuleDependencyTransformer(allTargets.toTargetIds(), targetsMap, project)
    val moduleDependencies = buildTargetToModuleDependencyTransformer.transform(buildTarget)

    // then
    val expectedIntermediateModuleDependency =
      IntermediateModuleDependency(
        moduleName = "target2.target2",
      )

    moduleDependencies shouldBe listOf(expectedIntermediateModuleDependency)
  }

  @Test
  fun `should return multiple modules dependencies for target with multiple dependencies`() {
    // given
    val buildTarget =
      BuildTarget(
        Label.parse("//target1"),
        emptyList(),
        emptyList(),
        listOf(
          Label.parse("//target2"),
          Label.parse("//target3"),
          Label.parse("//target4"),
          Label.parse("@maven//:lib1"),
          Label.parse("@maven//:lib2"),
        ),
        TargetKind(
          kindString = "java_binary",
          ruleType = RuleType.BINARY,
        ),
        sources = emptyList(),
        resources = emptyList(),
        baseDirectory = Path("base/dir"),
      )
    val allTargets =
      setOf(
        "@//target1",
        "@//target2",
        "@//target3",
        "@//target4",
        "@//target5",
      )

    val targetsMap = allTargets.toDefaultTargetsMap()

    // when
    val buildTargetToModuleDependencyTransformer =
      BuildTargetToModuleDependencyTransformer(allTargets.toTargetIds(), targetsMap, project)
    val moduleDependencies = buildTargetToModuleDependencyTransformer.transform(buildTarget)

    // then
    val expectedIntermediateModuleDependency1 =
      IntermediateModuleDependency(
        moduleName = "target2.target2",
      )
    val expectedIntermediateModuleDependency2 =
      IntermediateModuleDependency(
        moduleName = "target3.target3",
      )
    val expectedIntermediateModuleDependency3 =
      IntermediateModuleDependency(
        moduleName = "target4.target4",
      )

    moduleDependencies shouldBe
      listOf(expectedIntermediateModuleDependency1, expectedIntermediateModuleDependency2, expectedIntermediateModuleDependency3)
  }

  @Test
  fun `should return multiple modules dependencies for multiple targets`() {
    // given
    val buildTarget1 =
      BuildTarget(
        Label.parse("//target1"),
        emptyList(),
        emptyList(),
        listOf(
          Label.parse("//target2"),
          Label.parse("//target3"),
          Label.parse("@maven//:lib1"),
          Label.parse("@maven//:lib2"),
        ),
        TargetKind(
          kindString = "java_binary",
          ruleType = RuleType.BINARY,
        ),
        sources = emptyList(),
        resources = emptyList(),
        baseDirectory = Path("base/dir"),
      )
    val buildTarget2 =
      BuildTarget(
        Label.parse("//target1"),
        emptyList(),
        emptyList(),
        listOf(
          Label.parse("//target3"),
          Label.parse("//target4"),
          Label.parse("@maven//:lib2"),
        ),
        TargetKind(
          kindString = "java_binary",
          ruleType = RuleType.BINARY,
        ),
        sources = emptyList(),
        resources = emptyList(),
        baseDirectory = Path("base/dir"),
      )

    val allTargets =
      setOf(
        "@//target1",
        "@//target2",
        "@//target3",
        "@//target4",
        "@//target5",
      )

    val buildTargets = listOf(buildTarget1, buildTarget2)

    val targetsMap = allTargets.toDefaultTargetsMap()

    // when
    val buildTargetToModuleDependencyTransformer =
      BuildTargetToModuleDependencyTransformer(allTargets.toTargetIds(), targetsMap, project)
    val moduleDependencies = buildTargetToModuleDependencyTransformer.transform(buildTargets)

    // then
    val expectedIntermediateModuleDependency1 =
      IntermediateModuleDependency(
        moduleName = "target2.target2",
      )
    val expectedIntermediateModuleDependency2 =
      IntermediateModuleDependency(
        moduleName = "target3.target3",
      )
    val expectedIntermediateModuleDependency3 =
      IntermediateModuleDependency(
        moduleName = "target4.target4",
      )

    moduleDependencies shouldBe
      listOf(expectedIntermediateModuleDependency1, expectedIntermediateModuleDependency2, expectedIntermediateModuleDependency3)
  }

  private fun Set<String>.toTargetIds(): Set<Label> = mapTo(mutableSetOf()) { Label.parse(it) }
}
