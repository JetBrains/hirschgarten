package org.jetbrains.bazel.sync.workspace.snapshot

import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.label.DependencyLabel
import org.jetbrains.bazel.label.DependencyLabelKind
import org.jetbrains.bazel.label.Label
import org.jetbrains.bsp.protocol.SourceFileCollection
import org.jetbrains.bsp.protocol.RawBuildTarget
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.nio.file.Path

class WorkspaceTargetGraphTest {
  @Nested
  @DisplayName("WorkspaceTargetGraph.findAllTransitiveSuccessors")
  inner class FindAllTransitiveSuccessorsTest {
    @Test
    fun `should return empty sequence for non-existing target key`() {
      val graph = buildGraph()

      val result = graph.findAllTransitiveSuccessors(WorkspaceTargetKey(Label.parse("//does/not/exist")))

      result.toSet() shouldBe emptySet()
    }

    @Test
    fun `should return empty sequence for target without successors`() {
      val a = workspaceTarget("//A")
      val graph = buildGraph(listOf(a), a)

      val result = graph.findAllTransitiveSuccessors(a.targetKey)

      result.toSet() shouldBe emptySet()
    }

    @Test
    fun `should return only direct successors for target without transitive dependencies`() {
      val b = workspaceTarget("//b")
      val c = workspaceTarget("//c")
      val d = workspaceTarget("//d")
      val a = workspaceTarget("//A", compileDeps = listOf("//b" to null, "//c" to null, "//d" to null))
      val graph = buildGraph(listOf(a), a, b, c, d)

      val result = graph.findAllTransitiveSuccessors(a.targetKey)

      result.toSet() shouldBe setOf(b, c, d)
    }

    @Test
    fun `should return direct and transitive successors`() {
      val d = workspaceTarget("//d")
      val e = workspaceTarget("//e")
      val b = workspaceTarget("//b", compileDeps = listOf("//d" to null, "//e" to null))
      val c = workspaceTarget("//c")
      val a = workspaceTarget("//A", compileDeps = listOf("//b" to null, "//c" to null))
      val graph = buildGraph(listOf(a), a, b, c, d, e)

      val result = graph.findAllTransitiveSuccessors(a.targetKey)

      result.toSet() shouldBe setOf(b, c, d, e)
    }

    @Test
    fun `should handle diamond dependency`() {
      val d = workspaceTarget("//d")
      val e = workspaceTarget("//e")
      val b = workspaceTarget("//b", compileDeps = listOf("//d" to null, "//e" to null))
      val c = workspaceTarget("//c", compileDeps = listOf("//e" to null))
      val a = workspaceTarget("//A", compileDeps = listOf("//b" to null, "//c" to null))
      val graph = buildGraph(listOf(a), a, b, c, d, e)

      val result = graph.findAllTransitiveSuccessors(a.targetKey)

      result.toSet() shouldBe setOf(b, c, d, e)
    }

    @Test
    fun `should return same result when called twice (caching)`() {
      val b = workspaceTarget("//b")
      val c = workspaceTarget("//c")
      val a = workspaceTarget("//A", compileDeps = listOf("//b" to null, "//c" to null))
      val graph = buildGraph(listOf(a), a, b, c)

      val first = graph.findAllTransitiveSuccessors(a.targetKey).toSet()
      val second = graph.findAllTransitiveSuccessors(a.targetKey).toSet()

      first shouldBe second
      first shouldBe setOf(b, c)
    }

    @Test
    fun `should not follow runtime dependencies`() {
      val runtimeDep = workspaceTarget("//runtimeDep")
      val compileDep = workspaceTarget("//compileDep")
      val a = workspaceTarget(
        "//A",
        compileDeps = listOf("//compileDep" to null),
        runtimeDeps = listOf("//runtimeDep"),
      )
      val graph = buildGraph(listOf(a), a, compileDep, runtimeDep)

      val result = graph.findAllTransitiveSuccessors(a.targetKey)

      result.toSet() shouldBe setOf(compileDep)
    }
  }

  @Nested
  @DisplayName("WorkspaceTargetGraph.findAllTransitiveSuccessorsWithoutRootTargets")
  inner class FindAllTransitiveSuccessorsWithoutRootTargetsTest {
    @Test
    fun `should return direct and transitive successors including root targets that are not direct successors`() {
      val f = workspaceTarget("//f")
      val g = workspaceTarget("//g")
      val e = workspaceTarget("//e")
      val d = workspaceTarget("//D", compileDeps = listOf("//f" to null, "//g" to null))
      val c = workspaceTarget("//c")
      val b = workspaceTarget("//b", compileDeps = listOf("//D" to null, "//e" to null))
      val a = workspaceTarget("//A", compileDeps = listOf("//b" to null, "//c" to null))
      val graph = buildGraph(listOf(a, d), a, b, c, d, e, f, g)

      val result = graph.findAllTransitiveSuccessorsWithoutRootTargets(a.targetKey)

      result.toSet() shouldBe setOf(b, c, d, e, f, g)
    }

    @Test
    fun `should exclude direct root target successors and their subtrees`() {
      val l = workspaceTarget("//L")
      val k = workspaceTarget("//k", compileDeps = listOf("//L" to null))
      val j = workspaceTarget("//j", compileDeps = listOf("//k" to null))
      val i = workspaceTarget("//i")
      val h = workspaceTarget("//h")
      val g = workspaceTarget("//g")
      val f = workspaceTarget("//F", compileDeps = listOf("//i" to null, "//j" to null))
      val e = workspaceTarget("//e")
      val d = workspaceTarget("//d")
      val b = workspaceTarget("//B", compileDeps = listOf("//d" to null, "//e" to null))
      val c = workspaceTarget("//c", compileDeps = listOf("//F" to null, "//g" to null, "//h" to null))
      val a = workspaceTarget("//A", compileDeps = listOf("//B" to null, "//c" to null))
      val graph = buildGraph(listOf(a, b, f, l), a, b, c, d, e, f, g, h, i, j, k, l)

      val result = graph.findAllTransitiveSuccessorsWithoutRootTargets(a.targetKey)

      result.toSet() shouldBe setOf(c, f, g, h, i, j, k, l)
    }

    @Test
    fun `should return transitive successors of a root target excluding only direct root successors`() {
      val l = workspaceTarget("//L")
      val k = workspaceTarget("//k", compileDeps = listOf("//L" to null))
      val j = workspaceTarget("//j", compileDeps = listOf("//k" to null))
      val i = workspaceTarget("//i")
      val h = workspaceTarget("//h")
      val g = workspaceTarget("//g")
      val f = workspaceTarget("//F", compileDeps = listOf("//i" to null, "//j" to null))
      val e = workspaceTarget("//e")
      val d = workspaceTarget("//d")
      val b = workspaceTarget("//B", compileDeps = listOf("//d" to null, "//e" to null))
      val c = workspaceTarget("//c", compileDeps = listOf("//F" to null, "//g" to null, "//h" to null))
      val a = workspaceTarget("//A", compileDeps = listOf("//B" to null, "//c" to null))
      val graph = buildGraph(listOf(a, b, f, l), a, b, c, d, e, f, g, h, i, j, k, l)

      val result = graph.findAllTransitiveSuccessorsWithoutRootTargets(f.targetKey)

      result.toSet() shouldBe setOf(i, j, k, l)
    }
  }

  @Nested
  @DisplayName("WorkspaceTargetGraph.findAllTargetsAtDepth")
  inner class FindAllTargetsAtDepthTest {
    @Test
    fun `should return only root targets for depth 0`() {
      val f = workspaceTarget("//f")
      val g = workspaceTarget("//g")
      val e = workspaceTarget("//e")
      val d = workspaceTarget("//D", compileDeps = listOf("//f" to null, "//g" to null))
      val c = workspaceTarget("//c")
      val b = workspaceTarget("//b", compileDeps = listOf("//D" to null, "//e" to null))
      val a = workspaceTarget("//A", compileDeps = listOf("//b" to null, "//c" to null))
      val graph = buildGraph(listOf(a, d), a, b, c, d, e, f, g)

      val result = graph.findAllTargetsAtDepth(0)

      result.toSet() shouldBe setOf(a, d)
    }

    @Test
    fun `should return root targets and their direct dependencies for depth 1`() {
      val f = workspaceTarget("//f")
      val g = workspaceTarget("//g")
      val e = workspaceTarget("//e")
      val d = workspaceTarget("//D", compileDeps = listOf("//f" to null, "//g" to null))
      val c = workspaceTarget("//c")
      val b = workspaceTarget("//b", compileDeps = listOf("//D" to null, "//e" to null))
      val a = workspaceTarget("//A", compileDeps = listOf("//b" to null, "//c" to null))
      val graph = buildGraph(listOf(a, d), a, b, c, d, e, f, g)

      val result = graph.findAllTargetsAtDepth(1)

      result.toSet() shouldBe setOf(a, b, c, d, f, g)
    }

    @Test
    fun `should return two levels of dependencies for depth 2`() {
      val f = workspaceTarget("//f")
      val g = workspaceTarget("//g")
      val e = workspaceTarget("//e")
      val d = workspaceTarget("//D", compileDeps = listOf("//f" to null, "//g" to null))
      val c = workspaceTarget("//c")
      val b = workspaceTarget("//b", compileDeps = listOf("//D" to null, "//e" to null))
      val a = workspaceTarget("//A", compileDeps = listOf("//b" to null, "//c" to null))
      val graph = buildGraph(listOf(a), a, b, c, d, e, f, g)

      val result = graph.findAllTargetsAtDepth(2)

      result.toSet() shouldBe setOf(a, b, c, d, e)
    }

    @Test
    fun `should return 11 targets for depth 10 in a linear chain of 12`() {
      val a11 = workspaceTarget("//a11")
      val a10 = workspaceTarget("//a10", compileDeps = listOf("//a11" to null))
      val a9 = workspaceTarget("//a09", compileDeps = listOf("//a10" to null))
      val a8 = workspaceTarget("//a08", compileDeps = listOf("//a09" to null))
      val a7 = workspaceTarget("//a07", compileDeps = listOf("//a08" to null))
      val a6 = workspaceTarget("//a06", compileDeps = listOf("//a07" to null))
      val a5 = workspaceTarget("//a05", compileDeps = listOf("//a06" to null))
      val a4 = workspaceTarget("//a04", compileDeps = listOf("//a05" to null))
      val a3 = workspaceTarget("//a03", compileDeps = listOf("//a04" to null))
      val a2 = workspaceTarget("//a02", compileDeps = listOf("//a03" to null))
      val a1 = workspaceTarget("//a01", compileDeps = listOf("//a02" to null))
      val a0 = workspaceTarget("//A00", compileDeps = listOf("//a01" to null))
      val graph = buildGraph(listOf(a0), a0, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11)

      val result = graph.findAllTargetsAtDepth(10)

      result.toSet() shouldBe setOf(a0, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10)
    }

    @Test
    fun `should return all targets for depth -1`() {
      val f = workspaceTarget("//f")
      val g = workspaceTarget("//g")
      val e = workspaceTarget("//e")
      val d = workspaceTarget("//D", compileDeps = listOf("//f" to null, "//g" to null))
      val c = workspaceTarget("//c")
      val b = workspaceTarget("//b", compileDeps = listOf("//D" to null, "//e" to null))
      val a = workspaceTarget("//A", compileDeps = listOf("//b" to null, "//c" to null))
      val graph = buildGraph(listOf(a), a, b, c, d, e, f, g)

      val result = graph.findAllTargetsAtDepth(-1)

      result.toSet() shouldBe setOf(a, b, c, d, e, f, g)
    }

    @Test
    fun `should not follow runtime dependencies by default`() {
      val runtimeDep = workspaceTarget("//runtimeDep")
      val compileDep = workspaceTarget("//compileDep")
      val root = workspaceTarget(
        "//root",
        compileDeps = listOf("//compileDep" to null),
        runtimeDeps = listOf("//runtimeDep"),
      )
      val graph = buildGraph(listOf(root), root, compileDep, runtimeDep)

      val result = graph.findAllTargetsAtDepth(1)

      result.toSet() shouldBe setOf(root, compileDep)
    }

    @Test
    fun `should follow runtime dependencies when runtimeDependencies=true`() {
      val runtimeDep = workspaceTarget("//runtimeDep")
      val compileDep = workspaceTarget("//compileDep")
      val root = workspaceTarget(
        "//root",
        compileDeps = listOf("//compileDep" to null),
        runtimeDeps = listOf("//runtimeDep"),
      )
      val graph = buildGraph(listOf(root), root, compileDep, runtimeDep)

      val result = graph.findAllTargetsAtDepth(1, runtimeDependencies = true)

      result.toSet() shouldBe setOf(root, compileDep, runtimeDep)
    }

    @Test
    fun `should exclude targets not matching condition and skip their subtrees`() {
      val c = workspaceTarget("//c")
      val b = workspaceTarget("//b", compileDeps = listOf("//c" to null))
      val d = workspaceTarget("//d")
      val a = workspaceTarget("//A", compileDeps = listOf("//b" to null, "//d" to null))
      val graph = buildGraph(listOf(a), a, b, c, d)

      val result = graph.findAllTargetsAtDepth(-1) { it.label != Label.parse("//d") }

      result.toSet() shouldBe setOf(a, b, c)
    }

    @Test
    fun `should return empty when root does not match condition`() {
      val b = workspaceTarget("//b")
      val a = workspaceTarget("//A", compileDeps = listOf("//b" to null))
      val graph = buildGraph(listOf(a), a, b)

      val result = graph.findAllTargetsAtDepth(-1) { it.label != Label.parse("//A") }

      result.toSet() shouldBe emptySet()
    }

    @Test
    fun `should support macros that generate a target and refer to it by an alias`() {
      val dependency = workspaceTarget("//dependency")
      val generatedByAlias =
        workspaceTarget("//mypackage:generatedByAlias", generatorName = "alias", compileDeps = listOf("//dependency" to null))
      val importedTarget = workspaceTarget("//anotherpackage:importedTarget")
      val generatedByImportedTarget = workspaceTarget("//anotherpackage:generatedByImportedTarget", generatorName = "importedTarget")
      val nonImportedTarget = workspaceTarget("//nonImportedTarget", generatorName = "somethingRandom")
      val rootTargetKeys = setOf(
        WorkspaceTargetKey(label = Label.parse("//mypackage:alias")),
        WorkspaceTargetKey(label = Label.parse("//anotherpackage:importedTarget")),
      )
      val graph = buildGraph(rootTargetKeys, generatedByAlias, dependency, generatedByImportedTarget, importedTarget, nonImportedTarget)

      val depth0 = graph.findAllTargetsAtDepth(0)
      val depth1 = graph.findAllTargetsAtDepth(1)
      val allTargets = graph.findAllTargetsAtDepth(-1)

      depth0.toSet() shouldBe setOf(importedTarget, generatedByAlias)
      depth1.toSet() shouldBe setOf(importedTarget, generatedByAlias, dependency)
      allTargets.toSet() shouldBe setOf(importedTarget, generatedByAlias, dependency)
    }
  }

  @Nested
  @DisplayName("WorkspaceTargetGraph: Bazel configuration/transition support")
  inner class BazelConfigurationTest {
    @Test
    fun `should treat same label with different configurations as distinct nodes in findAllTargetsAtDepth`() {
      val common = workspaceTarget("//common")
      val libConfig1 = workspaceTarget("//lib", configuration = "config1", compileDeps = listOf("//common" to null))
      val libConfig2 = workspaceTarget("//lib", configuration = "config2", compileDeps = listOf("//common" to null))
      val app = workspaceTarget(
        "//app",
        compileDeps = listOf("//lib" to "config1", "//lib" to "config2"),
      )
      val graph = buildGraph(listOf(app), app, libConfig1, libConfig2, common)

      val result = graph.findAllTargetsAtDepth(-1)

      result.toSet() shouldBe setOf(app, libConfig1, libConfig2, common)
    }

    @Test
    fun `should traverse configured targets in findAllTransitiveSuccessors`() {
      val common = workspaceTarget("//common")
      val libConfig1 = workspaceTarget("//lib", configuration = "config1", compileDeps = listOf("//common" to null))
      val libConfig2 = workspaceTarget("//lib", configuration = "config2", compileDeps = listOf("//common" to null))
      val app = workspaceTarget(
        "//app",
        compileDeps = listOf("//lib" to "config1", "//lib" to "config2"),
      )
      val graph = buildGraph(listOf(app), app, libConfig1, libConfig2, common)

      val result = graph.findAllTransitiveSuccessors(app.targetKey)

      result.toSet() shouldBe setOf(libConfig1, libConfig2, common)
    }

    @Test
    fun `findAllTransitiveSuccessors should be scoped to specific configuration`() {
      val common = workspaceTarget("//common")
      val libConfig1 = workspaceTarget("//lib", configuration = "config1", compileDeps = listOf("//common" to null))
      val libConfig2 = workspaceTarget("//lib", configuration = "config2")
      val app = workspaceTarget(
        "//app",
        compileDeps = listOf("//lib" to "config1", "//lib" to "config2"),
      )
      val graph = buildGraph(listOf(app), app, libConfig1, libConfig2, common)

      val result = graph.findAllTransitiveSuccessors(libConfig1.targetKey)

      result.toSet() shouldBe setOf(common)
    }
  }

  @Nested
  @DisplayName("WorkspaceTargetGraph: shadow-graph aspect-id handling")
  inner class ShadowGraphTest {
    @Test
    fun `findTargetByKey returns the exact shadow when aspect ids match`() {
      val shadow = shadowTarget("//other_proto", aspectIds = listOf("bazel_java_proto_aspect"))
      val graph = buildGraph(listOf(shadow), shadow)

      val result = graph.findTargetByKey(shadow.targetKey)

      result shouldBe shadow
    }

    @Test
    fun `findTargetByKey falls back to canonical shadow when only shadow exists`() {
      val shadow = shadowTarget("//proto", aspectIds = listOf("bazel_java_proto_aspect"))
      val graph = buildGraph(listOf(shadow), shadow)

      val result = graph.findTargetByKey(WorkspaceTargetKey(Label.parse("//proto")))

      result shouldBe shadow
    }

    @Test
    fun `findTargetByKey prefers the empty-aspect node over shadows`() {
      val primary = workspaceTarget("//proto")
      val shadow = shadowTarget("//proto", aspectIds = listOf("bazel_java_proto_aspect"))
      val graph = buildGraph(listOf(primary), primary, shadow)

      val result = graph.findTargetByKey(WorkspaceTargetKey(Label.parse("//proto")))

      result shouldBe primary
    }

    @Test
    fun `findTargetByKey returns the first inserted shadow when multiple shadows match`() {
      val one = shadowTarget("//proto", aspectIds = listOf("zz_aspect"))
      val two = shadowTarget("//proto", aspectIds = listOf("aa_aspect", "bb_aspect"))
      val three = shadowTarget("//proto", aspectIds = listOf("aa_aspect"))
      val graph = buildGraph(listOf(one, two, three), one, two, three)

      val result = graph.findTargetByKey(WorkspaceTargetKey(Label.parse("//proto")))

      result shouldBe one
    }

    @Test
    fun `successor edges propagate parent aspect ids when dep has none`() {
      val depShadow = shadowTarget("//dep", aspectIds = listOf("bazel_java_proto_aspect"))
      val parent = workspaceTargetWithShadowDeps(
        aspectIds = listOf("bazel_java_proto_aspect"),
        // dep edge carries no aspect ids - the aspect failed to propagate them
        rawDeps = listOf(WorkspaceTargetKey(Label.parse("//dep"))),
      )
      val graph = buildGraph(listOf(parent), parent, depShadow)

      val result = graph.findAllTransitiveSuccessors(parent.targetKey)

      result.toSet() shouldBe setOf(depShadow)
    }

    @Test
    fun `successor edges prefer exact aspect ids match over propagation`() {
      val depPlain = workspaceTarget("//dep")
      val depShadow = shadowTarget("//dep", aspectIds = listOf("bazel_java_proto_aspect"))
      val parent = workspaceTargetWithShadowDeps(
        aspectIds = listOf("bazel_java_proto_aspect"),
        rawDeps = listOf(WorkspaceTargetKey(Label.parse("//dep"))),
      )
      val graph = buildGraph(listOf(parent), parent, depPlain, depShadow)

      val result = graph.findAllTransitiveSuccessors(parent.targetKey)

      // the exact `(//dep, EMPTY, EMPTY)` key wins over the propagated `(//dep, EMPTY, [bazel_java_proto_aspect])`.
      result.toSet() shouldBe setOf(depPlain)
    }

    @Test
    fun `successor edges are dropped when neither exact nor propagated keys match`() {
      val depShadow = shadowTarget("//dep", aspectIds = listOf("other_aspect"))
      val parent = workspaceTargetWithShadowDeps(
        aspectIds = listOf("bazel_java_proto_aspect"),
        rawDeps = listOf(WorkspaceTargetKey(Label.parse("//dep"))),
      )
      val graph = buildGraph(listOf(parent), parent, depShadow)

      val result = graph.findAllTransitiveSuccessors(parent.targetKey)

      result.toSet() shouldBe emptySet()
    }
  }

  @Nested
  @DisplayName("WorkspaceTargetGraph: relaxed dependency expansion")
  inner class RelaxedDependencyExpansionTest {
    @Test
    fun `relaxed walk surfaces every shadow sharing label and configuration`() {
      val depPlain = workspaceTarget("//dep")
      val depShadow = shadowTarget("//dep", aspectIds = listOf("bazel_java_proto_aspect"))
      val parent = workspaceTargetWithShadowDeps(
        aspectIds = emptyList(),
        rawDeps = listOf(WorkspaceTargetKey(Label.parse("//dep"))),
      )
      val graph = buildGraph(listOf(parent), parent, depPlain, depShadow)

      val strict = graph.findAllTransitiveSuccessors(parent.targetKey).toSet()
      val relaxed = graph.findAllTransitiveSuccessors(parent.targetKey, useRelaxedDependencyExpansion = true).toSet()

      strict shouldBe setOf(depPlain)
      relaxed shouldBe setOf(depPlain, depShadow)
    }

    @Test
    fun `relaxed walk does not invent successors when no shadow exists`() {
      val depShadow = shadowTarget("//dep", aspectIds = listOf("bazel_java_proto_aspect"))
      val parent = workspaceTargetWithShadowDeps(
        aspectIds = listOf("bazel_java_proto_aspect"),
        rawDeps = listOf(
          WorkspaceTargetKey(label = Label.parse("//dep"), aspectIds = WorkspaceAspectIds.of(listOf("bazel_java_proto_aspect"))),
        ),
      )
      val graph = buildGraph(listOf(parent), parent, depShadow)

      val relaxed = graph.findAllTransitiveSuccessors(parent.targetKey, useRelaxedDependencyExpansion = true).toSet()

      relaxed shouldBe setOf(depShadow)
    }

    @Test
    fun `findAllTargetsAtDepth honours relaxed expansion`() {
      val depPlain = workspaceTarget("//dep")
      val depShadow = shadowTarget("//dep", aspectIds = listOf("bazel_java_proto_aspect"))
      val parent = workspaceTargetWithShadowDeps(
        aspectIds = emptyList(),
        rawDeps = listOf(WorkspaceTargetKey(Label.parse("//dep"))),
      )
      val graph = buildGraph(listOf(parent), parent, depPlain, depShadow)

      val strict = graph.findAllTargetsAtDepth(maxDepth = 1).toSet()
      val relaxed = graph.findAllTargetsAtDepth(maxDepth = 1, useRelaxedDependencyExpansion = true).toSet()

      strict shouldBe setOf(parent, depPlain)
      relaxed shouldBe setOf(parent, depPlain, depShadow)
    }

    @Test
    fun `findAllTransitiveSuccessorsWithoutRootTargets honours relaxed expansion`() {
      val depPlain = workspaceTarget("//dep")
      val depShadow = shadowTarget("//dep", aspectIds = listOf("bazel_java_proto_aspect"))
      val parent = workspaceTargetWithShadowDeps(
        aspectIds = emptyList(),
        rawDeps = listOf(WorkspaceTargetKey(Label.parse("//dep"))),
      )
      val graph = buildGraph(listOf(parent), parent, depPlain, depShadow)

      val relaxed = graph
        .findAllTransitiveSuccessorsWithoutRootTargets(parent.targetKey, useRelaxedDependencyExpansion = true)
        .toSet()

      relaxed shouldBe setOf(depPlain, depShadow)
    }
  }

  private fun workspaceTarget(
    id: String,
    configuration: String? = null,
    compileDeps: List<Pair<String, String?>> = emptyList(),
    runtimeDeps: List<String> = emptyList(),
    generatorName: String? = null,
  ): WorkspaceTarget {
    val label = Label.parse(id)
    val compileConfiguredDeps = compileDeps.map { (depLabel, depConfig) ->
      DependencyLabel(
        targetKey = WorkspaceTargetKey(label = Label.parse(depLabel), configuration = WorkspaceConfigurationId.of(depConfig)),
        kind = DependencyLabelKind.COMPILE,
      )
    }
    val targetKey = WorkspaceTargetKey(label, WorkspaceConfigurationId.of(configuration))
    return WorkspaceTarget(
      targetKey = targetKey,
      rawBuildTarget = RawBuildTarget(
        key = targetKey,
        dependencies = compileConfiguredDeps + runtimeDeps.map { DependencyLabel(targetKey = WorkspaceTargetKey(label = Label.parse(it)), kind = DependencyLabelKind.RUNTIME) },
        kind = TargetKind(
          kind = "java_library",
          ruleType = RuleType.LIBRARY,
          languageClasses = setOf(LanguageClass.JAVA),
        ),
        sources = SourceFileCollection.EMPTY,
        generatedSources = SourceFileCollection.EMPTY,
        resources = SourceFileCollection.EMPTY,
        baseDirectory = Path.of("/tmp"),
        generatorName = generatorName,
      ),
    )
  }

  private fun shadowTarget(id: String, aspectIds: List<String>): WorkspaceTarget {
    val label = Label.parse(id)
    val targetKey = WorkspaceTargetKey(label, aspectIds = WorkspaceAspectIds.of(aspectIds))
    return WorkspaceTarget(
      targetKey = targetKey,
      rawBuildTarget = RawBuildTarget(
        key = targetKey,
        dependencies = emptyList(),
        kind = TargetKind(kind = "java_library", ruleType = RuleType.LIBRARY, languageClasses = setOf(LanguageClass.JAVA)),
        sources = SourceFileCollection.EMPTY,
        generatedSources = SourceFileCollection.EMPTY,
        resources = SourceFileCollection.EMPTY,
        baseDirectory = Path.of("/tmp"),
      ),
    )
  }

  private fun workspaceTargetWithShadowDeps(
    aspectIds: List<String>,
    rawDeps: List<WorkspaceTargetKey>,
  ): WorkspaceTarget {
    val label = Label.parse("//proto")
    val targetKey = WorkspaceTargetKey(label, aspectIds = WorkspaceAspectIds.of(aspectIds))
    return WorkspaceTarget(
      targetKey = targetKey,
      rawBuildTarget = RawBuildTarget(
        key = targetKey,
        dependencies = rawDeps.map { DependencyLabel(targetKey = it, kind = DependencyLabelKind.COMPILE) },
        kind = TargetKind(kind = "java_library", ruleType = RuleType.LIBRARY, languageClasses = setOf(LanguageClass.JAVA)),
        sources = SourceFileCollection.EMPTY,
        generatedSources = SourceFileCollection.EMPTY,
        resources = SourceFileCollection.EMPTY,
        baseDirectory = Path.of("/tmp"),
      ),
    )
  }

  private fun buildGraph(vararg targets: WorkspaceTarget): WorkspaceTargetGraph =
    WorkspaceTargetGraphBuilder.build(emptySet(), targets.toList())

  private fun buildGraph(rootTargets: Collection<WorkspaceTarget>, vararg targets: WorkspaceTarget): WorkspaceTargetGraph =
    WorkspaceTargetGraphBuilder.build(rootTargets.map { it.targetKey }.toSet(), targets.toList())

  private fun buildGraph(rootTargetKeys: Set<WorkspaceTargetKey>, vararg targets: WorkspaceTarget): WorkspaceTargetGraph =
    WorkspaceTargetGraphBuilder.build(rootTargetKeys, targets.toList())
}
