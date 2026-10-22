package org.jetbrains.bazel.sync.workspace.snapshot

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.test.framework.testproject.javaBinary
import org.jetbrains.bazel.test.framework.testproject.javaLibrary
import org.jetbrains.bsp.protocol.BuildTarget
import org.junit.jupiter.api.Test

class ExecutableTargetsIndexTest {
  private fun index(targets: List<BuildTarget>, importDepth: Int = -1, roots: List<BuildTarget> = targets): ExecutableTargetsIndex {
    val graph = WorkspaceTargetGraphBuilder.build(rootTargets = roots.map { it.key }.toSet(), targets = targets)
    return ExecutableTargetsIndexBuilder.build(targetGraph = graph, importDepth = importDepth, targets = targets)
  }

  @Test
  fun `binary depending on library is its executable target`() {
    val idx = index(
      listOf(
        javaBinary("//app:bin") { deps("//lib:lib") },
        javaLibrary("//lib:lib"),
      ),
    )
    idx.executableTargetsFor(Label.parse("//lib:lib")).shouldContainExactly(Label.parse("//app:bin"))
  }

  @Test
  fun `library with no dependents has no executable targets`() {
    val idx = index(listOf(javaLibrary("//lib:lib")))
    idx.executableTargetsFor(Label.parse("//lib:lib")).shouldBeEmpty()
  }

  @Test
  fun `targets below import depth are excluded from computation`() {
    // root: bin -> lib (depth 1) -> deep (depth 2); with importDepth = 1 `deep` is out of scope
    val bin = javaBinary("//app:bin") { deps("//lib:lib") }
    val lib = javaLibrary("//lib:lib") { deps("//deep:deep") }
    val deep = javaLibrary("//deep:deep")
    val idx = index(listOf(bin, lib, deep), importDepth = 1, roots = listOf(bin))
    idx.executableTargetsFor(Label.parse("//deep:deep")).shouldBeEmpty()
    idx.executableTargetsFor(Label.parse("//lib:lib")).shouldContainExactly(Label.parse("//app:bin"))
  }
}
