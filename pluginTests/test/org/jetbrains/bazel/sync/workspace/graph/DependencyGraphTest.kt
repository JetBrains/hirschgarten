package org.jetbrains.bazel.sync.workspace.graph

import com.google.devtools.intellij.aspect.Common.ArtifactLocation
import com.google.devtools.intellij.ideinfo.IntellijIdeInfo.TargetIdeInfo
import com.google.devtools.intellij.ideinfo.IntellijIdeInfo.TargetKey
import com.google.devtools.intellij.ideinfo.IntellijIdeInfo.Dependency
import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

// graphs generated using: https://arthursonzogni.com/Diagon/#GraphDAG
class DependencyGraphTest {
  @Nested
  @DisplayName("DependencyGraph.allTargetsAtDepth")
  inner class DependenciesAtDepthTest {
    @Test
    fun `should return only root targets for importing depth 0`() {
      // graph:
      // '?' - queried target
      // '+' - should be returned
      // '-' - shouldn't be returned
      // capital letter - root target
      // ┌──────────┐
      // │    A     │
      // │    ?     │
      // └┬────────┬┘
      // ┌▽──────┐┌▽┐
      // │   b   ││c│
      // │   -   ││-│
      // └┬─────┬┘└─┘
      // ┌▽───┐┌▽┐
      // │ D  ││e│
      // │ -  ││-│
      // └┬──┬┘└─┘
      // ┌▽┐┌▽┐
      // │f││g│
      // │-││-│
      // └─┘└─┘

      // given
      val a = targetInfo("//A", listOf("//b", "//c"))
      val b = targetInfo("//b", listOf("//D", "//e"))
      val c = targetInfo("//c", listOf())
      val d = targetInfo("//D", listOf("//f", "//g"))
      val e = targetInfo("//e", listOf())
      val f = targetInfo("//f", listOf())
      val g = targetInfo("//g", listOf())
      val idToTargetInfo = toIdToTargetInfoMap(a, b, c, d, e, f, g)
      val rootTargets = setOf(Label.parse("//A"), Label.parse("//D"))
      val dependencyGraph = createDependencyGraph(rootTargets, idToTargetInfo)

      // when
      val dependencies = dependencyGraph.allTargetsAtDepth(0)

      // then
      val expectedDependencies = setOf(a, d)
      dependencies shouldBe expectedDependencies
    }

    @Test
    fun `should return root targets and their direct dependencies for importing depth 1`() {
      // graph:
      // '?' - queried target
      // '+' - should be returned
      // '-' - shouldn't be returned
      // capital letter - root target
      // ┌──────────┐
      // │    A     │
      // │    ?     │
      // └┬────────┬┘
      // ┌▽──────┐┌▽┐
      // │   b   ││c│
      // │   +   ││+│
      // └┬─────┬┘└─┘
      // ┌▽───┐┌▽┐
      // │ D  ││e│
      // │ +  ││-│
      // └┬──┬┘└─┘
      // ┌▽┐┌▽┐
      // │f││g│
      // │+││+│
      // └─┘└─┘

      // given
      val a = targetInfo("//A", listOf("//b", "//c"))
      val b = targetInfo("//b", listOf("//D", "//e"))
      val c = targetInfo("//c", listOf())
      val d = targetInfo("//D", listOf("//f", "//g"))
      val e = targetInfo("//e", listOf())
      val f = targetInfo("//f", listOf())
      val g = targetInfo("//g", listOf())
      val idToTargetInfo = toIdToTargetInfoMap(a, b, c, d, e, f, g)
      val rootTargets = setOf(Label.parse("//A"), Label.parse("//D"))
      val dependencyGraph = createDependencyGraph(rootTargets, idToTargetInfo)

      // when
      val dependencies = dependencyGraph.allTargetsAtDepth(1)

      // then
      val expectedDependencies = setOf(a, b, c, d, f, g)
      dependencies shouldBe expectedDependencies
    }

    @Test
    fun `should return two levels of dependencies for importing depth 2`() {
      // graph:
      // '?' - queried target
      // '+' - should be returned
      // '-' - shouldn't be returned
      // capital letter - root target
      // ┌──────────┐
      // │    A     │
      // │    ?     │
      // └┬────────┬┘
      // ┌▽──────┐┌▽┐
      // │   b   ││c│
      // │   +   ││+│
      // └┬─────┬┘└─┘
      // ┌▽───┐┌▽┐
      // │ D  ││e│
      // │ +  ││+│
      // └┬──┬┘└─┘
      // ┌▽┐┌▽┐
      // │f││g│
      // │-││-│
      // └─┘└─┘

      // given
      val a = targetInfo("//A", listOf("//b", "//c"))
      val b = targetInfo("//b", listOf("//D", "//e"))
      val c = targetInfo("//c", listOf())
      val d = targetInfo("//D", listOf("//f", "//g"))
      val e = targetInfo("//e", listOf())
      val f = targetInfo("//f", listOf())
      val g = targetInfo("//g", listOf())
      val idToTargetInfo = toIdToTargetInfoMap(a, b, c, d, e, f, g)
      val rootTargets = setOf(Label.parse("//A"))
      val dependencyGraph = createDependencyGraph(rootTargets, idToTargetInfo)

      // when
      val dependencies = dependencyGraph.allTargetsAtDepth(2)

      // then
      val expectedDependencies = setOf(a, b, c, d, e)
      dependencies shouldBe expectedDependencies
    }

    @Test
    fun `should return 11 targets for importing depth 10`() {
      // graph:
      // '?' - queried target
      // '+' - should be returned
      // '-' - shouldn't be returned
      // capital letter - root target
      // ┌───┐
      // │A00│
      // │ + │
      // └┬──┘
      // ┌▽──┐
      // │a01│
      // │ + │
      // └┬──┘
      // ┌▽──┐
      // │a02│
      // │ + │
      // └┬──┘
      // ┌▽──┐
      // │a03│
      // │ + │
      // └┬──┘
      // ┌▽──┐
      // │a04│
      // │ + │
      // └┬──┘
      // ┌▽──┐
      // │a05│
      // │ + │
      // └┬──┘
      // ┌▽──┐
      // │a06│
      // │ + │
      // └┬──┘
      // ┌▽──┐
      // │a07│
      // │ + │
      // └┬──┘
      // ┌▽──┐
      // │a08│
      // │ + │
      // └┬──┘
      // ┌▽──┐
      // │a09│
      // │ + │
      // └┬──┘
      // ┌▽──┐
      // │a10│
      // │ + │
      // └┬──┘
      // ┌▽──┐
      // │a11│
      // │ - │
      // └───┘

      // given
      val a0 = targetInfo("//A00", listOf("//a01"))
      val a1 = targetInfo("//a01", listOf("//a02"))
      val a2 = targetInfo("//a02", listOf("//a03"))
      val a3 = targetInfo("//a03", listOf("//a04"))
      val a4 = targetInfo("//a04", listOf("//a05"))
      val a5 = targetInfo("//a05", listOf("//a06"))
      val a6 = targetInfo("//a06", listOf("//a07"))
      val a7 = targetInfo("//a07", listOf("//a08"))
      val a8 = targetInfo("//a08", listOf("//a09"))
      val a9 = targetInfo("//a09", listOf("//a10"))
      val a10 = targetInfo("//a10", listOf("//a11"))
      val a11 = targetInfo("//a11")
      val rootTargets = setOf(Label.parse("//A00"))
      val idToTargetInfo =
        toIdToTargetInfoMap(a0, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11)
      val dependencyGraph = createDependencyGraph(rootTargets, idToTargetInfo)

      // when
      val dependencies = dependencyGraph.allTargetsAtDepth(10)

      // then
      val expectedDependencies = setOf(a0, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10)
      dependencies shouldBe expectedDependencies
    }

    @Test
    fun `should return all targets for importing depth -1`() {
      // graph:
      // '?' - queried target
      // '+' - should be returned
      // '-' - shouldn't be returned
      // capital letter - root target
      // ┌──────────┐
      // │    A     │
      // │    ?     │
      // └┬────────┬┘
      // ┌▽──────┐┌▽┐
      // │   b   ││c│
      // │   +   ││+│
      // └┬─────┬┘└─┘
      // ┌▽───┐┌▽┐
      // │ D  ││e│
      // │ +  ││+│
      // └┬──┬┘└─┘
      // ┌▽┐┌▽┐
      // │f││g│
      // │+││+│
      // └─┘└─┘

      // given
      val a = targetInfo("//A", listOf("//b", "//c"))
      val b = targetInfo("//b", listOf("//D", "//e"))
      val c = targetInfo("//c", listOf())
      val d = targetInfo("//D", listOf("//f", "//g"))
      val e = targetInfo("//e", listOf())
      val f = targetInfo("//f", listOf())
      val g = targetInfo("//g", listOf())
      val idToTargetInfo = toIdToTargetInfoMap(a, b, c, d, e, f, g)
      val rootTargets = setOf(Label.parse("//A"))
      val dependencyGraph = createDependencyGraph(rootTargets, idToTargetInfo)

      // when
      val dependencies = dependencyGraph.allTargetsAtDepth(-1)

      // then
      val expectedDependencies = setOf(a, b, c, d, e, f, g)
      dependencies shouldBe expectedDependencies
    }
  }

  @Test
  fun `should not follow runtime deps`() {
    // given
    val target = targetInfo("//target", dependenciesIds = listOf("//target1"), runtimeDependenciesIds = listOf("//target4"))
    val target1 = targetInfo("//target1", dependenciesIds = listOf("//target2"), runtimeDependenciesIds = listOf("//target3"))
    val target2 = targetInfo("//target2")
    val target3 = targetInfo("//target3")
    val target4 = targetInfo("//target4")
    val rootTargets = setOf(Label.parse("//target"))
    val idToTargetInfo =
      toIdToTargetInfoMap(target, target1, target2, target3, target4)
    val dependencyGraph = createDependencyGraph(rootTargets, idToTargetInfo)

    // when
    val dependencies = dependencyGraph.allTargetsAtDepth(1)

    // then
    val expectedDependencies = setOf(target, target1)
    dependencies shouldBe expectedDependencies
  }

  @Test
  fun `should support macros that generate a target and refer to it by an alias`() {
    // given
    val generatedByAlias = targetInfo("//mypackage:generatedByAlias", generatorName = "alias", dependenciesIds = listOf("//dependency"))
    val dependency = targetInfo("//dependency")

    val generatedByImportedTarget = targetInfo("//anotherpackage:generatedByImportedTarget", generatorName = "importedTarget")
    val importedTarget = targetInfo("//anotherpackage:importedTarget")

    val nonImportedTarget = targetInfo("//nonImportedTarget", generatorName = "somethingRandom")

    // Alias target is in rootTargets, but not in idToTargetInfo, because the aspect is not run on aliases
    val rootTargets = setOf(Label.parse("//mypackage:alias"), Label.parse("//anotherpackage:importedTarget"))
    val idToTargetInfo = toIdToTargetInfoMap(generatedByAlias, dependency, generatedByImportedTarget, importedTarget, nonImportedTarget)
    val dependencyGraph = createDependencyGraph(rootTargets, idToTargetInfo)

    // when
    val dependenciesAtDepth0 = dependencyGraph.allTargetsAtDepth(0)
    val dependenciesAtDepth1 = dependencyGraph.allTargetsAtDepth(1)
    val transitiveDependencies = dependencyGraph.allTargetsAtDepth(-1)

    // then
    val expectedDependenciesAtDepth0 = setOf(importedTarget, generatedByAlias)
    val expectedDependenciesAtDepth1 = setOf(importedTarget, generatedByAlias, dependency)
    val expectedTransitiveDependencies = setOf(importedTarget, generatedByAlias, dependency)

    dependenciesAtDepth0 shouldBe expectedDependenciesAtDepth0
    dependenciesAtDepth1 shouldBe expectedDependenciesAtDepth1
    transitiveDependencies shouldBe expectedTransitiveDependencies
  }

  @Test
  fun `should propagate exports of non-workspace targets if someone depends on them (BAZEL-2175)`() {
    // given
    val target = targetInfo("//target", dependenciesIds = listOf("//target1"), addSrcs = true)
    val target1 = targetInfo("//target1", dependenciesIds = listOf("//target2"), addSrcs = false)
    val target2 = targetInfo("//target2", addSrcs = false)
    val rootTargets = setOf(Label.parse("//target"), Label.parse("//target1"))
    val idToTargetInfo =
      toIdToTargetInfoMap(target, target1, target2)
    val dependencyGraph = createDependencyGraph(rootTargets, idToTargetInfo)

    // when
    val dependencies =
      dependencyGraph.allTargetsAtDepth(
        1,
      )

    // then
    val expectedDependencies = setOf(target, target1, target2)
    dependencies shouldBe expectedDependencies
  }

  @Test
  fun `should propagate exports of non-workspace targets if someone depends on them (maxDepth = 2)`() {
    // given
    val target = targetInfo("//target", dependenciesIds = listOf("//target1"), addSrcs = true)
    val target1 = targetInfo("//target1", dependenciesIds = listOf("//target2"), addSrcs = false)
    val target2 = targetInfo("//target2", dependenciesIds = listOf("//target3"), addSrcs = false)
    val target3 = targetInfo("//target3", addSrcs = false)
    val rootTargets = setOf(Label.parse("//target"), Label.parse("//target1"))
    val idToTargetInfo =
      toIdToTargetInfoMap(target, target1, target2, target3)
    val dependencyGraph = createDependencyGraph(rootTargets, idToTargetInfo)

    // when
    val dependencies =
      dependencyGraph.allTargetsAtDepth(
        2,
      )

    // then
    val expectedDependencies = setOf(target, target1, target2, target3)
    dependencies shouldBe expectedDependencies
  }

  private fun targetInfo(
    id: String,
    dependenciesIds: List<String> = listOf(),
    runtimeDependenciesIds: List<String> = listOf(),
    addSrcs: Boolean = true,
    generatorName: String = "",
  ): TargetIdeInfo =
    TargetIdeInfo
      .newBuilder()
      .setKey(TargetKey.newBuilder().setLabel(id).build())
      .addAllDeps(
        dependenciesIds.map { dependency(it, Dependency.DependencyType.COMPILE_TIME) } +
        runtimeDependenciesIds.map { dependency(it, Dependency.DependencyType.RUNTIME) },
      )
      .apply {
        if (addSrcs) {
          addSrcs(ArtifactLocation.newBuilder().build())
        }
      }
      .setGeneratorName(generatorName).build()

  private fun dependency(id: String, dependencyType: Dependency.DependencyType): Dependency =
    Dependency
      .newBuilder()
      .setTarget(TargetKey.newBuilder().setLabel(id))
      .setDependencyType(dependencyType)
      .build()

  private fun toIdToTargetInfoMap(vararg targetIds: TargetIdeInfo): Map<Label, TargetIdeInfo> =
    targetIds.associateBy { targetId -> Label.parse(targetId.key.label) }

  private fun createDependencyGraph(
    rootTargets: Set<Label>,
    idToTargetInfo: Map<Label, TargetIdeInfo>,
  ) = DependencyGraph(
    rootTargets.map { WorkspaceTargetKey(label = it) }.toSet(),
    idToTargetInfo.mapKeys { WorkspaceTargetKey(label = it.key) },
  )
}
