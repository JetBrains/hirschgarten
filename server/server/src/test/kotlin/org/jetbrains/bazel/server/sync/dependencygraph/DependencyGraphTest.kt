package org.jetbrains.bazel.server.sync.dependencygraph

import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.info.Dependency
import org.jetbrains.bazel.info.DependencyType
import org.jetbrains.bazel.info.TargetInfo
import org.jetbrains.bazel.label.CanonicalLabel
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.server.dependencygraph.DependencyGraph
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

// graphs generated using: https://arthursonzogni.com/Diagon/#GraphDAG
class DependencyGraphTest {
  @Nested
  @DisplayName("DependencyGraph.transitiveDependenciesWithoutRootTargets")
  inner class TransitiveDependenciesTest {
    @Test
    fun `should return empty list for not existing target`() {
      // given
      val dependencyGraph = DependencyGraph()

      // when
      val dependencies =
        dependencyGraph.transitiveDependenciesWithoutRootTargets(Label.parseCanonical("//does/not/exist"))

      // then
      dependencies shouldBe emptySet()
    }

    @Test
    fun `should return no dependencies for target without dependencies`() {
      // graph:
      // '?' - queried target
      // '+' - should be returned
      // '-' - shouldn't be returned
      // capital letter - root target
      // ┌─┐
      // │A│
      // └┬┘
      // ┌▽┐
      // │B│
      // │?│
      // └─┘

      // given
      val a = targetInfo("//A", listOf("//B"))
      val b = targetInfo("//B")
      val idToTargetInfo = toIdToTargetInfoMap(a, b)
      val rootTargets = setOf(Label.parseCanonical("//A"), Label.parseCanonical("//B"))
      val dependencyGraph = DependencyGraph(rootTargets, idToTargetInfo)

      // when
      val dependencies = dependencyGraph.transitiveDependenciesWithoutRootTargets(Label.parseCanonical("//B"))

      // then
      dependencies shouldBe emptySet()
    }

    @Test
    fun `should return only direct dependencies for target without transitive dependencies`() {
      // graph:
      // '?' - queried target
      // '+' - should be returned
      // '-' - shouldn't be returned
      // capital letter - root target
      // ┌───────┐
      // │   A   │
      // │   ?   │
      // └┬──┬──┬┘
      // ┌▽┐┌▽┐┌▽┐
      // │b││c││d│
      // │+││+││+│
      // └─┘└─┘└─┘

      // given
      val a = targetInfo("//A", listOf("//b", "//c", "//d"))
      val b = targetInfo("//b", listOf())
      val c = targetInfo("//c", listOf())
      val d = targetInfo("//d", listOf())
      val idToTargetInfo = toIdToTargetInfoMap(a, b, c, d)
      val rootTargets = setOf(Label.parseCanonical("/"))
      val dependencyGraph = DependencyGraph(rootTargets, idToTargetInfo)

      // when
      val dependencies = dependencyGraph.transitiveDependenciesWithoutRootTargets(Label.parseCanonical("//A"))

      // then
      val expectedDependencies = setOf(b, c, d)
      dependencies shouldBe expectedDependencies
    }

    @Test
    fun `should return direct and transitive dependencies for target with transitive dependencies`() {
      // graph:
      // '?' - queried target
      // '+' - should be returned
      // '-' - shouldn't be returned
      // capital letter - root target
      // ┌───────┐
      // │   A   │
      // │   ?   │
      // └┬─────┬┘
      // ┌▽───┐┌▽┐
      // │ b  ││c│
      // │ +  ││+│
      // └┬──┬┘└─┘
      // ┌▽┐┌▽┐
      // │d││e│
      // │+││+│
      // └─┘└─┘

      // given
      val a = targetInfo("//A", listOf("//b", "//c"))
      val b = targetInfo("//b", listOf("//d", "//e"))
      val c = targetInfo("//c", listOf())
      val d = targetInfo("//d", listOf())
      val e = targetInfo("//e", listOf())
      val idToTargetInfo = toIdToTargetInfoMap(a, b, c, d, e)
      val rootTargets = setOf(Label.parseCanonical("//A"))
      val dependencyGraph = DependencyGraph(rootTargets, idToTargetInfo)

      // when
      val dependencies = dependencyGraph.transitiveDependenciesWithoutRootTargets(Label.parseCanonical("//A"))

      // then
      val expectedDependencies = setOf(b, c, d, e)
      dependencies shouldBe expectedDependencies
    }

    @Test
    fun `should return direct and transitive dependencies for target with transitive dependencies but leaf is reused by targets`() {
      // graph:
      // '?' - queried target
      // '+' - should be returned
      // '-' - shouldn't be returned
      // capital letter - root target
      // ┌───────┐
      // │   A   │
      // │   ?   │
      // └┬─────┬┘
      // ┌▽───┐┌▽┐
      // │ b  ││c│
      // │ +  ││+│
      // └┬──┬┘└┬┘
      // ┌▽┐┌▽──▽┐
      // │d││ e  │
      // │+││ +  │
      // └─┘└────┘

      // given
      val a = targetInfo("//A", listOf("//b", "//c"))
      val b = targetInfo("//b", listOf("//d", "//e"))
      val c = targetInfo("//c", listOf("//e"))
      val d = targetInfo("//d", listOf())
      val e = targetInfo("//e", listOf())
      val idToTargetInfo = toIdToTargetInfoMap(a, b, c, d, e)
      val rootTargets = setOf(Label.parseCanonical("//A"))
      val dependencyGraph = DependencyGraph(rootTargets, idToTargetInfo)

      // when
      val dependencies = dependencyGraph.transitiveDependenciesWithoutRootTargets(Label.parseCanonical("//A"))

      // then
      val expectedDependencies = setOf(b, c, d, e)
      dependencies shouldBe expectedDependencies
    }

    @Test
    fun `should return direct and transitive dependencies for target with transitive dependencies including deep root target`() {
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
      val rootTargets = setOf(Label.parseCanonical("//A"), Label.parseCanonical("//D"))
      val dependencyGraph = DependencyGraph(rootTargets, idToTargetInfo)

      // when
      val dependencies = dependencyGraph.transitiveDependenciesWithoutRootTargets(Label.parseCanonical("//A"))

      // then
      val expectedDependencies = setOf(b, c, d, e, f, g)
      dependencies shouldBe expectedDependencies
    }

    @Test
    fun `should return direct and transitive dependencies for target with transitive dependencies including deep root target 2`() {
      // graph:
      // '?' - queried target
      // '+' - should be returned
      // '-' - shouldn't be returned
      // capital letter - root target
      // ┌───────┐
      // │   A   │
      // │   ?   │
      // └┬─────┬┘
      // ┌▽───┐┌▽─────────┐
      // │ B  ││    c     │
      // │ -  ││    +     │
      // └┬──┬┘└┬─────┬──┬┘
      // ┌▽┐┌▽┐┌▽───┐┌▽┐┌▽┐
      // │d││e││ F  ││g││h│
      // │-││-││ +  ││+││+│
      // └─┘└─┘└┬──┬┘└─┘└─┘
      //       ┌▽┐┌▽┐
      //       │i││j│
      //       │+││+│
      //       └─┘└┬┘
      //          ┌▽┐
      //          │k│
      //          │+│
      //          └┬┘
      //          ┌▽┐
      //          │L│
      //          │+│
      //          └─┘

      // given
      val a = targetInfo("//A", listOf("//B", "//c"))
      val b = targetInfo("//B", listOf("//d", "//e"))
      val c = targetInfo("//c", listOf("//F", "//g", "//h"))
      val d = targetInfo("//d", listOf())
      val e = targetInfo("//e", listOf())
      val f = targetInfo("//F", listOf("//i", "//j"))
      val g = targetInfo("//g", listOf())
      val h = targetInfo("//h", listOf())
      val i = targetInfo("//i", listOf())
      val j = targetInfo("//j", listOf("//k"))
      val k = targetInfo("//k", listOf("//L"))
      val l = targetInfo("//L", listOf())
      val idToTargetInfo = toIdToTargetInfoMap(a, b, c, d, e, f, g, h, i, j, k, l)
      val rootTargets = setOf(Label.parseCanonical("//A"), Label.parseCanonical("//B"), Label.parseCanonical("//F"), Label.parseCanonical("//L"))
      val dependencyGraph = DependencyGraph(rootTargets, idToTargetInfo)

      // when
      val dependencies = dependencyGraph.transitiveDependenciesWithoutRootTargets(Label.parseCanonical("//A"))

      // then
      val expectedDependencies = setOf(c, f, g, h, i, j, k, l)
      dependencies shouldBe expectedDependencies
    }

    @Test
    fun `should return direct and transitive dependencies for target with transitive dependencies including deep root target 3`() {
      // graph:
      // '?' - queried target
      // '+' - should be returned
      // '-' - shouldn't be returned
      // capital letter - root target
      // ┌───────┐
      // │   A   │
      // └┬─────┬┘
      // ┌▽───┐┌▽─────────┐
      // │ B  ││    c     │
      // └┬──┬┘└┬─────┬──┬┘
      // ┌▽┐┌▽┐┌▽───┐┌▽┐┌▽┐
      // │d││e││ F  ││g││h│
      // │-││-││ ?  ││-││-│
      // └─┘└─┘└┬──┬┘└─┘└─┘
      //       ┌▽┐┌▽┐
      //       │i││j│
      //       │+││+│
      //       └─┘└┬┘
      //          ┌▽┐
      //          │k│
      //          │+│
      //          └┬┘
      //          ┌▽┐
      //          │L│
      //          │+│
      //          └─┘

      // given
      val a = targetInfo("//A", listOf("//B", "//c"))
      val b = targetInfo("//B", listOf("//d", "//e"))
      val c = targetInfo("//c", listOf("//F", "//g", "//h"))
      val d = targetInfo("//d", listOf())
      val e = targetInfo("//e", listOf())
      val f = targetInfo("//F", listOf("//i", "//j"))
      val g = targetInfo("//g", listOf())
      val h = targetInfo("//h", listOf())
      val i = targetInfo("//i", listOf())
      val j = targetInfo("//j", listOf("//k"))
      val k = targetInfo("//k", listOf("//L"))
      val l = targetInfo("//L", listOf())
      val idToTargetInfo = toIdToTargetInfoMap(a, b, c, d, e, f, g, h, i, j, k, l)
      val rootTargets = setOf(Label.parseCanonical("//A"), Label.parseCanonical("//B"), Label.parseCanonical("//F"), Label.parseCanonical("//L"))
      val dependencyGraph = DependencyGraph(rootTargets, idToTargetInfo)

      // when
      val dependencies = dependencyGraph.transitiveDependenciesWithoutRootTargets(Label.parseCanonical("//F"))

      // then
      val expectedDependencies = setOf(i, j, k, l)
      dependencies shouldBe expectedDependencies
    }
  }

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
      val rootTargets = setOf(Label.parseCanonical("//A"), Label.parseCanonical("//D"))
      val dependencyGraph = DependencyGraph(rootTargets, idToTargetInfo)

      // when
      val dependencies = dependencyGraph.allTargetsAtDepth(0, setOf(Label.parseCanonical("//A"), Label.parseCanonical("//D"))) { false }

      // then
      val expectedDependencies =
        DependencyGraph.TargetsAtDepth(
          targets = setOf(a, d),
          directDependencies = setOf(b, c, f, g),
        )
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
      val rootTargets = setOf(Label.parseCanonical("//A"), Label.parseCanonical("//D"))
      val dependencyGraph = DependencyGraph(rootTargets, idToTargetInfo)

      // when
      val dependencies = dependencyGraph.allTargetsAtDepth(1, setOf(Label.parseCanonical("//A"), Label.parseCanonical("//D"))) { false }

      // then
      val expectedDependencies =
        DependencyGraph.TargetsAtDepth(
          targets = setOf(a, b, c, d, f, g),
          directDependencies = setOf(e),
        )
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
      val rootTargets = setOf(Label.parseCanonical("//A"), Label.parseCanonical("//D"))
      val dependencyGraph = DependencyGraph(rootTargets, idToTargetInfo)

      // when
      val dependencies = dependencyGraph.allTargetsAtDepth(2, setOf(Label.parseCanonical("//A"))) { false }

      // then
      val expectedDependencies =
        DependencyGraph.TargetsAtDepth(
          targets = setOf(a, b, c, d, e),
          directDependencies = setOf(f, g),
        )
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
      val rootTargets = setOf(Label.parseCanonical("//A00"))
      val idToTargetInfo =
        toIdToTargetInfoMap(a0, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11)
      val dependencyGraph = DependencyGraph(rootTargets, idToTargetInfo)

      // when
      val dependencies = dependencyGraph.allTargetsAtDepth(10, setOf(Label.parseCanonical("//A00"))) { false }

      // then
      val expectedDependencies =
        DependencyGraph.TargetsAtDepth(
          targets = setOf(a0, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10),
          directDependencies = setOf(a11),
        )
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
      val rootTargets = setOf(Label.parseCanonical("//A"), Label.parseCanonical("//D"))
      val dependencyGraph = DependencyGraph(rootTargets, idToTargetInfo)

      // when
      val dependencies = dependencyGraph.allTargetsAtDepth(-1, setOf(Label.parseCanonical("//A"))) { false }

      // then
      val expectedDependencies =
        DependencyGraph.TargetsAtDepth(
          targets = setOf(a, b, c, d, e, f, g),
          directDependencies = emptySet(),
        )
      dependencies shouldBe expectedDependencies
    }
  }

  @Test
  fun `should return all transitive deps for libraries`() {
    // given
    val target = targetInfo("//target", listOf("@maven//library1", "//target1"))
    val target1 = targetInfo("//target1")
    val library1 = targetInfo("@maven//library1", listOf("@maven//library2"))
    val library2 = targetInfo("@maven//library2", listOf("@maven//library3"))
    val library3 = targetInfo("@maven//library3")
    val rootTargets = setOf(Label.parseCanonical("//target"))
    val idToTargetInfo =
      toIdToTargetInfoMap(target, target1, library1, library2, library3)
    val dependencyGraph = DependencyGraph(rootTargets, idToTargetInfo)

    // when
    val dependencies =
      dependencyGraph.allTargetsAtDepth(0, setOf(Label.parseCanonical("//target"))) { label ->
        label.repoName == "maven"
      }

    // then
    val expectedDependencies =
      DependencyGraph.TargetsAtDepth(
        targets = setOf(target, library1, library2, library3),
        directDependencies = setOf(target1),
      )
    dependencies shouldBe expectedDependencies
  }

  private fun targetInfo(id: String, dependenciesIds: List<String> = listOf()): TargetInfo {
    val dependencies = dependenciesIds.map(::dependency)
    val id = Label.parseCanonical(id)
    return TargetInfo(
      id = id,
      dependencies = dependencies,
      tags = emptyList(),
      kind = "kind",
      sources = emptyList(),
      generatedSources = emptyList(),
      resources = emptyList(),
      env = emptyMap(),
      envInherit = emptyList(),
      executable = false,
      workspaceName = "workspace",
    )
  }

  private fun dependency(id: String): Dependency = Dependency(
    id = Label.parseCanonical(id),
    dependencyType = DependencyType.COMPILE
  )

  private fun toIdToTargetInfoMap(vararg targetIds: TargetInfo): Map<CanonicalLabel, TargetInfo> =
    targetIds.associateBy { targetId -> targetId.id }
}
