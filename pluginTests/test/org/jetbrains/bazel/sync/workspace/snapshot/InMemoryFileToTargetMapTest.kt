package org.jetbrains.bazel.sync.workspace.snapshot

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import org.jetbrains.bazel.commons.RepoMappingDisabled
import org.jetbrains.bazel.test.framework.testproject.externalRepositoryPath
import org.jetbrains.bazel.test.framework.testproject.javaBinary
import org.jetbrains.bazel.test.framework.testproject.javaLibrary
import org.jetbrains.bazel.test.framework.testproject.testProjectBazelInfo
import org.jetbrains.bsp.protocol.BuildTargetData
import org.jetbrains.bsp.protocol.OutputLocation
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

  @Test
  fun `fresh sync file map holds the files that the target data declares`() {
    val workspaceRoot = Path.of("/workspace")
    val target = javaLibrary("//lib:lib") {
      srcs("lib/Lib.java", "lib/both.txt")
      data(DeclaredFiles(OutputLocation.Workspace("lib/declared.txt"), OutputLocation.Workspace("lib/both.txt")))
    }

    val map = File2TargetMapBuilder(testProjectBazelInfo(workspaceRoot = workspaceRoot), RepoMappingDisabled).build(targets = listOf(target))
    map.getTargetsByFile(workspaceRoot.resolve("lib/declared.txt")).shouldContainExactly(target.key)
    map.getTargetsByFile(workspaceRoot.resolve("lib/both.txt")).shouldContainExactly(target.key)
  }

  @Test
  fun `fresh sync file map skips a declared file of an external repository`() {
    val bazelInfo = testProjectBazelInfo(workspaceRoot = Path.of("/workspace"))
    val target = javaLibrary("//lib:lib") {
      data(DeclaredFiles(OutputLocation.External("ext+", "include/ext.h")))
    }

    val map = File2TargetMapBuilder(bazelInfo, RepoMappingDisabled).build(targets = listOf(target))
    map.getTargetsByFile(bazelInfo.externalRepositoryPath("ext+").resolve("include/ext.h")).shouldBeEmpty()
  }

  private class DeclaredFiles(private vararg val files: OutputLocation) : BuildTargetData {
    override fun additionalDeclaredFiles(): Sequence<OutputLocation> = files.asSequence()
  }
}
