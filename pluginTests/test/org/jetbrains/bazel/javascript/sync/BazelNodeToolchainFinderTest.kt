package org.jetbrains.bazel.javascript.sync

import com.intellij.util.system.CpuArch
import com.intellij.util.system.OS
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile

class BazelNodeToolchainFinderTest {
  private val darwinArm64 = NodePlatform.of(OS.macOS, CpuArch.ARM64)!!

  @TempDir
  lateinit var externalDir: Path

  @Test
  fun `maps host OS and CPU to rules_nodejs platform names`() {
    assertThat(NodePlatform.of(OS.macOS, CpuArch.ARM64)).isEqualTo(NodePlatform("darwin_arm64", "bin/nodejs/bin/node"))
    assertThat(NodePlatform.of(OS.Linux, CpuArch.X86_64)).isEqualTo(NodePlatform("linux_amd64", "bin/nodejs/bin/node"))
    assertThat(NodePlatform.of(OS.Windows, CpuArch.X86_64)).isEqualTo(NodePlatform("windows_amd64", "bin/nodejs/node.exe"))
    assertThat(NodePlatform.of(OS.FreeBSD, CpuArch.X86_64)).isNull()
    assertThat(NodePlatform.of(OS.Linux, CpuArch.X86)).isNull()
  }

  @Test
  fun `finds node of a Bzlmod repository`() {
    val node = createNodeRepository("rules_nodejs++node+nodejs_darwin_arm64")

    assertThat(findBazelNodeToolchain(externalDir, darwinArm64))
      .isEqualTo(BazelNodeToolchain("rules_nodejs++node+nodejs_darwin_arm64", "nodejs", node))
  }

  @Test
  fun `finds node of Bazel 7 and WORKSPACE repositories`() {
    val bazel7Node = createNodeRepository("rules_nodejs~~node~node20_darwin_arm64")

    assertThat(findBazelNodeToolchain(externalDir, darwinArm64))
      .isEqualTo(BazelNodeToolchain("rules_nodejs~~node~node20_darwin_arm64", "node20", bazel7Node))

    val workspaceNode = createNodeRepository("nodejs_darwin_arm64")

    assertThat(findBazelNodeToolchain(externalDir, darwinArm64))
      .isEqualTo(BazelNodeToolchain("nodejs_darwin_arm64", "nodejs", workspaceNode))
  }

  @Test
  fun `prefers the default toolchain name when several runtimes were downloaded`() {
    createNodeRepository("rules_nodejs++node+node18_darwin_arm64")
    val defaultNode = createNodeRepository("rules_nodejs++node+nodejs_darwin_arm64")
    createNodeRepository("rules_nodejs++node+node22_darwin_arm64")

    assertThat(findBazelNodeToolchain(externalDir, darwinArm64)?.nodeBinary).isEqualTo(defaultNode)
  }

  @Test
  fun `ignores other platforms and repositories without a node binary`() {
    createNodeRepository("rules_nodejs++node+nodejs_linux_amd64")
    externalDir.resolve("rules_nodejs++node+nodejs_toolchains").createDirectories()
    externalDir.resolve("some_tool_darwin_arm64/bin").createDirectories()

    assertThat(findBazelNodeToolchain(externalDir, darwinArm64)).isNull()
  }

  @Test
  fun `returns null when the external directory does not exist`() {
    assertThat(findBazelNodeToolchain(externalDir.resolve("missing"), darwinArm64)).isNull()
  }

  private fun createNodeRepository(name: String): Path =
    externalDir.resolve(name).resolve(darwinArm64.relativeNodeBinary)
      .also { it.parent.createDirectories() }
      .createFile()
}
