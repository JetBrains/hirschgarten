package org.jetbrains.bazel.sync.workspace.snapshot

import io.kotest.matchers.collections.shouldContainExactly
import org.jetbrains.bazel.commons.RepoMappingDisabled
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync.JavaLanguageClass
import org.jetbrains.bazel.test.framework.target.TestBuildTarget
import org.jetbrains.bazel.test.framework.testBazelInfo
import org.jetbrains.bsp.protocol.OutputLocation
import org.jetbrains.bsp.protocol.OutputLocationCollection
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
      sources = OutputLocationCollectionBuilder.ofLocations(listOf(OutputLocation.Workspace("app/Main.java"))),
      resources = OutputLocationCollection.EMPTY,
      baseDirectory = workspaceRoot.resolve("app"),
    )

    val map = File2TargetMapBuilder(testBazelInfo(workspaceRoot = workspaceRoot), RepoMappingDisabled).build(targets = listOf(target))
    map.getTargetsByFile(workspaceRoot.resolve("app/Main.java")).shouldContainExactly(key)
  }
}
