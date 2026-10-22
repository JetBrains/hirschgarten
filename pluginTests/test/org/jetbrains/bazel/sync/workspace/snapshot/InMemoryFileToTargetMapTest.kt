package org.jetbrains.bazel.sync.workspace.snapshot

import io.kotest.matchers.collections.shouldContainExactly
import org.jetbrains.bazel.commons.RepoMappingDisabled
import org.jetbrains.bazel.test.framework.testproject.javaBinary
import org.jetbrains.bazel.test.framework.testproject.testProjectBazelInfo
import org.junit.jupiter.api.Test
import java.nio.file.Path

class InMemoryFileToTargetMapTest {
  @Test
  fun `fresh sync file map resolves sources relative to the workspace root`() {
    val workspaceRoot = Path.of("/workspace")
    val target = javaBinary("//app:bin") { srcs("app/Main.java") }

    val map = File2TargetMapBuilder(testProjectBazelInfo(workspaceRoot = workspaceRoot), RepoMappingDisabled).build(targets = listOf(target))
    map.getTargetsByFile(workspaceRoot.resolve("app/Main.java")).shouldContainExactly(target.key)
  }
}
