package org.jetbrains.bazel.sync.workspace.mapper.normal

import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.info.BspTargetInfo
import org.jetbrains.bazel.info.BspTargetInfo.Dependency
import org.jetbrains.bazel.info.BspTargetInfo.TargetInfo
import org.jetbrains.bazel.info.BspTargetInfo.TargetKey
import org.jetbrains.bazel.label.DependencyLabel
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync.workspace.graph.DependencyGraph
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class AspectBazelProjectMapperTest {
  @Nested
  @DisplayName("resolveShardFolkDependencies")
  inner class ResolveShardFolkDependencies {
    @Test
    fun `should return empty list for non-shard target`() {
      val target = targetInfo("//pkg:target")
      val graph = dependencyGraph(target)

      AspectBazelProjectMapper.resolveShardFolkDependencies(target, graph) shouldBe emptyList()
    }

    @Test
    fun `should return all shards under umbrella for a shard target`() {
      // umbrella ──(runtime)──> shard0
      //          ──(runtime)──> shard1
      val shard0 = targetInfo("//pkg:lib.0", tags = listOf("shard"))
      val shard1 = targetInfo("//pkg:lib.1", tags = listOf("shard"))
      val umbrella = targetInfo(
        "//pkg:lib",
        runtimeDeps = listOf("//pkg:lib.0", "//pkg:lib.1"),
        tags = listOf("umbrella"),
        withJvmTargetInfo = true,
      )
      val graph = dependencyGraph(shard0, shard1, umbrella)

      // shard0 should see itself and shard1 as folk deps
      AspectBazelProjectMapper.resolveShardFolkDependencies(shard0, graph) shouldBe
        listOf(Label.parse("//pkg:lib.0"), Label.parse("//pkg:lib.1"))
    }

    @Test
    fun `should return empty list when umbrella has no JvmTargetInfo`() {
      // getSourcesFromReverseDependencies filters for hasJvmTargetInfo()
      val shard0 = targetInfo("//pkg:lib.0", tags = listOf("shard"))
      val umbrella = targetInfo(
        "//pkg:lib",
        runtimeDeps = listOf("//pkg:lib.0"),
        tags = listOf("umbrella"),
        withJvmTargetInfo = false,
      )
      val graph = dependencyGraph(shard0, umbrella)

      AspectBazelProjectMapper.resolveShardFolkDependencies(shard0, graph) shouldBe emptyList()
    }

    @Test
    fun `should return empty list for shard target with no umbrella parent`() {
      val shard0 = targetInfo("//pkg:lib.0", tags = listOf("shard"))
      val graph = dependencyGraph(shard0)

      AspectBazelProjectMapper.resolveShardFolkDependencies(shard0, graph) shouldBe emptyList()
    }
  }

  @Nested
  @DisplayName("resolveDirectDependencies — umbrella shard promotion")
  inner class ResolveDirectDependenciesShardPromotion {
    @Test
    fun `should promote shard runtime deps to compile-time for umbrella target`() {
      val shard = targetInfo("//pkg:lib.0", tags = listOf("shard"))
      val umbrella = targetInfo(
        "//pkg:lib",
        runtimeDeps = listOf("//pkg:lib.0"),
        tags = listOf("umbrella"),
      )
      val graph = dependencyGraph(shard, umbrella)

      val result = AspectBazelProjectMapper.resolveDirectDependencies(umbrella, graph)

      result shouldBe listOf(DependencyLabel(Label.parse("//pkg:lib.0"), isRuntime = false))
    }

    @Test
    fun `should not promote non-shard runtime deps for umbrella target`() {
      val runtimeLib = targetInfo("//pkg:runtime_lib")
      val umbrella = targetInfo(
        "//pkg:lib",
        runtimeDeps = listOf("//pkg:runtime_lib"),
        tags = listOf("umbrella"),
      )
      val graph = dependencyGraph(runtimeLib, umbrella)

      val result = AspectBazelProjectMapper.resolveDirectDependencies(umbrella, graph)

      result shouldBe listOf(DependencyLabel(Label.parse("//pkg:runtime_lib"), isRuntime = true))
    }

    @Test
    fun `should not promote shard runtime deps for non-umbrella target`() {
      val shard = targetInfo("//pkg:lib.0", tags = listOf("shard"))
      val other = targetInfo("//pkg:other", runtimeDeps = listOf("//pkg:lib.0"))
      val graph = dependencyGraph(shard, other)

      val result = AspectBazelProjectMapper.resolveDirectDependencies(other, graph)

      result shouldBe listOf(DependencyLabel(Label.parse("//pkg:lib.0"), isRuntime = true))
    }

    @Test
    fun `should keep compile deps unchanged for umbrella target`() {
      val compileDep = targetInfo("//pkg:deps")
      val shard = targetInfo("//pkg:lib.0", tags = listOf("shard"))
      val umbrella = targetInfo(
        "//pkg:lib",
        compileDeps = listOf("//pkg:deps"),
        runtimeDeps = listOf("//pkg:lib.0"),
        tags = listOf("umbrella"),
      )
      val graph = dependencyGraph(compileDep, shard, umbrella)

      val result = AspectBazelProjectMapper.resolveDirectDependencies(umbrella, graph)

      result shouldBe listOf(
        DependencyLabel(Label.parse("//pkg:deps"), isRuntime = false),
        DependencyLabel(Label.parse("//pkg:lib.0"), isRuntime = false),
      )
    }
  }

  // ── helpers ──────────────────────────────────────────────────────────────

  private fun targetInfo(
    id: String,
    compileDeps: List<String> = emptyList(),
    runtimeDeps: List<String> = emptyList(),
    tags: List<String> = emptyList(),
    withJvmTargetInfo: Boolean = false,
  ): TargetInfo =
    TargetInfo
      .newBuilder()
      .setKey(TargetKey.newBuilder().setLabel(id))
      .addAllTags(tags)
      .addAllDeps(
        compileDeps.map { dependency(it, Dependency.DependencyType.COMPILE) } +
          runtimeDeps.map { dependency(it, Dependency.DependencyType.RUNTIME) },
      )
      .apply {
        if (withJvmTargetInfo) setJvmTargetInfo(BspTargetInfo.JvmTargetInfo.getDefaultInstance())
      }
      .build()

  private fun dependency(id: String, type: Dependency.DependencyType): Dependency =
    Dependency
      .newBuilder()
      .setTarget(TargetKey.newBuilder().setLabel(id))
      .setDependencyType(type)
      .build()

  private fun dependencyGraph(vararg targets: TargetInfo): DependencyGraph =
    DependencyGraph(
      rootTargets = emptySet(),
      idToTargetInfo = targets.associateBy { Label.parse(it.key.label) },
    )
}
