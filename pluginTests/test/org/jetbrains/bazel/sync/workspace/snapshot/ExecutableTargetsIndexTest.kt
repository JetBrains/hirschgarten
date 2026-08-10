package org.jetbrains.bazel.sync.workspace.snapshot

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.label.DependencyLabel
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync.JavaLanguageClass
import org.jetbrains.bazel.test.framework.target.TestBuildTarget
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.SourceFileCollection
import org.junit.jupiter.api.Test
import java.nio.file.Path

class ExecutableTargetsIndexTest {
  private fun target(
    label: String,
    deps: List<String> = emptyList(),
    executable: Boolean = false,
  ): TestBuildTarget {
    val key = WorkspaceTargetKey(label = Label.parse(label))
    return TestBuildTarget(
      key = key,
      dependencies = deps.map { DependencyLabel(targetKey = WorkspaceTargetKey(label = Label.parse(it))) },
      kind = TargetKind(
        kind = if (executable) "java_binary" else "java_library",
        ruleType = if (executable) RuleType.BINARY else RuleType.LIBRARY,
        languageClasses = setOf(JavaLanguageClass.JAVA),
      ),
      sources = SourceFileCollection.EMPTY,
      generatedSources = SourceFileCollection.EMPTY,
      resources = SourceFileCollection.EMPTY,
      baseDirectory = Path.of("/workspace"),
    )
  }

  private fun index(targets: List<BuildTarget>, importDepth: Int = -1, roots: List<BuildTarget> = targets): ExecutableTargetsIndex {
    val graph = WorkspaceTargetGraphBuilder.build(rootTargets = roots.map { it.key }.toSet(), targets = targets)
    return ExecutableTargetsIndexBuilder.build(targetGraph = graph, importDepth = importDepth, targets = targets)
  }

  @Test
  fun `binary depending on library is its executable target`() {
    val idx = index(
      listOf(
        target("//app:bin", deps = listOf("//lib:lib"), executable = true),
        target("//lib:lib"),
      ),
    )
    idx.executableTargetsFor(Label.parse("//lib:lib")).shouldContainExactly(Label.parse("//app:bin"))
  }

  @Test
  fun `library with no dependents has no executable targets`() {
    val idx = index(listOf(target("//lib:lib")))
    idx.executableTargetsFor(Label.parse("//lib:lib")).shouldBeEmpty()
  }

  @Test
  fun `targets below import depth are excluded from computation`() {
    // root: bin -> lib (depth 1) -> deep (depth 2); with importDepth = 1 `deep` is out of scope
    val bin = target("//app:bin", deps = listOf("//lib:lib"), executable = true)
    val lib = target("//lib:lib", deps = listOf("//deep:deep"))
    val deep = target("//deep:deep")
    val idx = index(listOf(bin, lib, deep), importDepth = 1, roots = listOf(bin))
    idx.executableTargetsFor(Label.parse("//deep:deep")).shouldBeEmpty()
    idx.executableTargetsFor(Label.parse("//lib:lib")).shouldContainExactly(Label.parse("//app:bin"))
  }
}
