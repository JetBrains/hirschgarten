package org.jetbrains.bazel.sync.workspace.snapshot

import io.kotest.matchers.collections.shouldContainExactly
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync.JavaLanguageClass
import org.jetbrains.bazel.test.framework.target.TestBuildTarget
import org.jetbrains.bsp.protocol.SourceFileCollection
import org.junit.jupiter.api.Test
import java.nio.file.Path

class InMemoryFileToTargetMapTest {
  @Test
  fun `fresh sync file map resolves sources relative to the workspace root`() {
    val workspaceRoot = Path.of("/workspace")
    val key = WorkspaceTargetKey(label = Label.parse("//app:bin"))
    val target = TestBuildTarget(
      key = key,
      dependencies = emptyList(),
      kind = TargetKind(kind = "java_binary", ruleType = RuleType.BINARY, languageClasses = setOf(JavaLanguageClass.JAVA)),
      sources = SourceFileCollectionBuilder.build(relativeRoot = workspaceRoot, paths = listOf(workspaceRoot.resolve("app/Main.java"))),
      generatedSources = SourceFileCollection.EMPTY,
      resources = SourceFileCollection.EMPTY,
      baseDirectory = workspaceRoot.resolve("app"),
    )

    val map = File2TargetMapBuilder.build(targets = listOf(target))
    map.getTargetsByFile(workspaceRoot.resolve("app/Main.java")).shouldContainExactly(key)
  }
}
