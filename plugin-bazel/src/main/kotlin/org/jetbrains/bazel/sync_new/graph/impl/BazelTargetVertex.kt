package org.jetbrains.bazel.sync_new.graph.impl

import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync_new.graph.ID
import org.jetbrains.bazel.sync_new.graph.TargetVertex
import java.nio.file.Path
import java.util.EnumSet

// TODO: use not-absolute paths
data class BazelTargetVertex(
  override val vertexId: ID,
  override val label: Label,
  val targetPath: Path,
  val genericData: BazelGenericTargetData,
) : TargetVertex

data class BazelGenericTargetData(
  val tags: EnumSet<BazelTargetTag>,
  val directDependencies: List<BazelTargetDependency>,
  val sources: List<BazelTargetSourceFile>,
  val resource: List<BazelTargetResourceFile>
)

enum class BazelTargetTag {
  LIBRARY,
  EXECUTABLE,
  TEST
}

data class BazelTargetDependency(
  val label: Label,
)

data class BazelTargetSourceFile(
  val path: Path,
  val priority: Int
)

data class BazelTargetResourceFile(
  val path: Path,
)
