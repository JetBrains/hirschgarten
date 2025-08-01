package org.jetbrains.bazel.sync.workspace.graph

import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.info.BspTargetInfo.Dependency
import org.jetbrains.bazel.info.BspTargetInfo.TargetInfo
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.label.assumeResolved
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
        dependencyGraph.transitiveDependenciesWithoutRootTargets(Label.parse("//does/not/exist"))

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
      val rootTargets = setOf(Label.parse("//A"), Label.parse("//B"))
      val dependencyGraph = DependencyGraph(rootTargets, idToTargetInfo)

      // when
      val dependencies = dependencyGraph.transitiveDependenciesWithoutRootTargets(Label.parse("//B"))

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
      val rootTargets = setOf(Label.parse("/"))
      val dependencyGraph = DependencyGraph(rootTargets, idToTargetInfo)

      // when
      val dependencies = dependencyGraph.transitiveDependenciesWithoutRootTargets(Label.parse("//A"))

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
      val rootTargets = setOf(Label.parse("//A"))
      val dependencyGraph = DependencyGraph(rootTargets, idToTargetInfo)

      // when
      val dependencies = dependencyGraph.transitiveDependenciesWithoutRootTargets(Label.parse("//A"))

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
      val rootTargets = setOf(Label.parse("//A"))
      val dependencyGraph = DependencyGraph(rootTargets, idToTargetInfo)

      // when
      val dependencies = dependencyGraph.transitiveDependenciesWithoutRootTargets(Label.parse("//A"))

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
      val rootTargets = setOf(Label.parse("//A"), Label.parse("//D"))
      val dependencyGraph = DependencyGraph(rootTargets, idToTargetInfo)

      // when
      val dependencies = dependencyGraph.transitiveDependenciesWithoutRootTargets(Label.parse("//A"))

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
      val rootTargets = setOf(Label.parse("//A"), Label.parse("//B"), Label.parse("//F"), Label.parse("//L"))
      val dependencyGraph = DependencyGraph(rootTargets, idToTargetInfo)

      // when
      val dependencies = dependencyGraph.transitiveDependenciesWithoutRootTargets(Label.parse("//A"))

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
      val rootTargets = setOf(Label.parse("//A"), Label.parse("//B"), Label.parse("//F"), Label.parse("//L"))
      val dependencyGraph = DependencyGraph(rootTargets, idToTargetInfo)

      // when
      val dependencies = dependencyGraph.transitiveDependenciesWithoutRootTargets(Label.parse("//F"))

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
      val rootTargets = setOf(Label.parse("//A"), Label.parse("//D"))
      val dependencyGraph = DependencyGraph(rootTargets, idToTargetInfo)

      // when
      val dependencies = dependencyGraph.allTargetsAtDepth(0, setOf(Label.parse("//A"), Label.parse("//D")))

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
      val rootTargets = setOf(Label.parse("//A"), Label.parse("//D"))
      val dependencyGraph = DependencyGraph(rootTargets, idToTargetInfo)

      // when
      val dependencies = dependencyGraph.allTargetsAtDepth(1, setOf(Label.parse("//A"), Label.parse("//D")))

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
      val rootTargets = setOf(Label.parse("//A"), Label.parse("//D"))
      val dependencyGraph = DependencyGraph(rootTargets, idToTargetInfo)

      // when
      val dependencies = dependencyGraph.allTargetsAtDepth(2, setOf(Label.parse("//A")))

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
      val rootTargets = setOf(Label.parse("//A00"))
      val idToTargetInfo =
        toIdToTargetInfoMap(a0, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11)
      val dependencyGraph = DependencyGraph(rootTargets, idToTargetInfo)

      // when
      val dependencies = dependencyGraph.allTargetsAtDepth(10, setOf(Label.parse("//A00")))

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
      val rootTargets = setOf(Label.parse("//A"), Label.parse("//D"))
      val dependencyGraph = DependencyGraph(rootTargets, idToTargetInfo)

      // when
      val dependencies = dependencyGraph.allTargetsAtDepth(-1, setOf(Label.parse("//A")))

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
    val rootTargets = setOf(Label.parse("//target"))
    val idToTargetInfo =
      toIdToTargetInfoMap(target, target1, library1, library2, library3)
    val dependencyGraph = DependencyGraph(rootTargets, idToTargetInfo)

    // when
    val dependencies =
      dependencyGraph.allTargetsAtDepth(0, setOf(Label.parse("//target")), isExternalTarget = { label ->
        label.assumeResolved().repoName == "maven"
      })

    // then
    val expectedDependencies =
      DependencyGraph.TargetsAtDepth(
        targets = setOf(target, library1, library2, library3),
        directDependencies = setOf(target1),
      )
    dependencies shouldBe expectedDependencies
  }

  @Test
  fun `should not return all transitive deps for libraries if the target supports strict deps`() {
    // given
    val target = targetInfo("//target", listOf("@maven//library1", "//target1"))
    val target1 = targetInfo("//target1")
    val library1 = targetInfo("@maven//library1", listOf("@maven//library2"))
    val library2 = targetInfo("@maven//library2", listOf("@maven//library3"))
    val library3 = targetInfo("@maven//library3")
    val rootTargets = setOf(Label.parse("//target"))
    val idToTargetInfo =
      toIdToTargetInfoMap(target, target1, library1, library2, library3)
    val dependencyGraph = DependencyGraph(rootTargets, idToTargetInfo)

    // when
    val dependencies =
      dependencyGraph.allTargetsAtDepth(
        0,
        setOf(Label.parse("//target")),
        isExternalTarget = { label ->
          label.assumeResolved().repoName == "maven"
        },
        targetSupportsStrictDeps = { it.toString() == "@//target" },
      )

    // then
    val expectedDependencies =
      DependencyGraph.TargetsAtDepth(
        targets = setOf(target),
        directDependencies = setOf(library1, target1),
      )
    dependencies shouldBe expectedDependencies
  }

  @Test
  fun `should not follow runtime deps for the last layer`() {
    // given
    val target = targetInfo("//target", runtimeDependenciesIds = listOf("//target1"))
    val target1 = targetInfo("//target1", dependenciesIds = listOf("//target2"), runtimeDependenciesIds = listOf("//target3"))
    val target2 = targetInfo("//target2")
    val target3 = targetInfo("//target3")
    val rootTargets = setOf(Label.parse("//target"))
    val idToTargetInfo =
      toIdToTargetInfoMap(target, target1, target2, target3)
    val dependencyGraph = DependencyGraph(rootTargets, idToTargetInfo)

    // when
    val dependencies = dependencyGraph.allTargetsAtDepth(1, setOf(Label.parse("//target")))

    // then
    val expectedDependencies =
      DependencyGraph.TargetsAtDepth(
        targets = setOf(target, target1),
        directDependencies = setOf(target2),
      )
    dependencies shouldBe expectedDependencies
  }

  @Test
  fun `should not add non-workspace targets unless someone depends on them`() {
    // given
    val target = targetInfo("//target", dependenciesIds = listOf("//target1"))
    val target1 = targetInfo("//target1")
    val target2 = targetInfo("//target2")
    val rootTargets = setOf(Label.parse("//target"), Label.parse("//target2"))
    val idToTargetInfo =
      toIdToTargetInfoMap(target, target1, target2)
    val dependencyGraph = DependencyGraph(rootTargets, idToTargetInfo)

    // when
    val dependencies =
      dependencyGraph.allTargetsAtDepth(1, rootTargets, isWorkspaceTarget = { label ->
        label.toString() == "@//target"
      })

    // then
    val expectedDependencies =
      DependencyGraph.TargetsAtDepth(
        targets = setOf(target, target1),
        directDependencies = setOf(),
      )
    dependencies shouldBe expectedDependencies
  }

  private fun targetInfo(
    id: String,
    dependenciesIds: List<String> = listOf(),
    runtimeDependenciesIds: List<String> = listOf(),
  ): TargetInfo =
    TargetInfo
      .newBuilder()
      .setId(id)
      .addAllDependencies(
        dependenciesIds.map { dependency(it, Dependency.DependencyType.COMPILE) } +
          runtimeDependenciesIds.map { dependency(it, Dependency.DependencyType.RUNTIME) },
      ).build()

  private fun dependency(id: String, dependencyType: Dependency.DependencyType): Dependency =
    Dependency
      .newBuilder()
      .setId(id)
      .setDependencyType(dependencyType)
      .build()

  private fun toIdToTargetInfoMap(vararg targetIds: TargetInfo): Map<Label, TargetInfo> =
    targetIds.associateBy { targetId -> Label.parse(targetId.id) }
}
