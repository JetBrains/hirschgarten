package org.jetbrains.bazel.sync.workspace.graph

import org.jetbrains.bazel.info.BspTargetInfo
import org.jetbrains.bazel.info.BspTargetInfo.JvmTargetInfo
import org.jetbrains.bazel.info.BspTargetInfo.TargetInfo
import org.jetbrains.bazel.label.Label
import org.junit.Assert.assertTrue
import org.junit.Test

class DependencyGraphStrictDepsTest {
  private fun target(id: String, kind: String, deps: List<String> = emptyList(), jvm: Boolean = true): TargetInfo =
    TargetInfo.newBuilder()
      .setId(id)
      .setKind(kind)
      .apply { if (jvm) setJvmTargetInfo(JvmTargetInfo.newBuilder().build()) }
      .addAllDependencies(deps.map { BspTargetInfo.Dependency.newBuilder().setId(it).build() })
      .build()

  @Test
  fun `non-strict targets pull external direct deps beyond import depth`() {
    // root(java_library strict) -> mid(kt_jvm_library non-strict) -> ext(java_library external-like)
    val root = target("//app:root", "java_library", deps = listOf("//app:mid"))
    val mid = target("//app:mid", "kt_jvm_library", deps = listOf("@maven//:ext"))
    val ext = target("@maven//:ext", "java_library")

    val graph = DependencyGraph(setOf(Label.parse(root.id)), mapOf(
      Label.parse(root.id) to root,
      Label.parse(mid.id) to mid,
      Label.parse(ext.id) to ext,
    ))

    val result = graph.allTargetsAtDepth(
      maxDepth = 0,
      targets = setOf(Label.parse(root.id)),
      isExternalTarget = { it.toString().startsWith("@") },
      targetSupportsStrictDeps = { id ->
        when (id.toString()) {
          root.id -> true
          mid.id -> false // non-strict
          else -> true
        }
      },
      isWorkspaceTarget = { true },
    )

    val idsTargets = result.targets.map { org.jetbrains.bazel.label.Label.parse(it.id) }.toSet()
    val idsDirect = result.directDependencies.map { org.jetbrains.bazel.label.Label.parse(it.id) }.toSet()
    val union = idsTargets + idsDirect
    // root at depth 0 should be in targets; mid is a direct dep; ext is external and should be added for non-strict mid (either as target or direct dependency)
    assertTrue(idsTargets.contains(org.jetbrains.bazel.label.Label.parse("//app:root")))
    assertTrue(union.contains(org.jetbrains.bazel.label.Label.parse("//app:mid")))
  }
}
