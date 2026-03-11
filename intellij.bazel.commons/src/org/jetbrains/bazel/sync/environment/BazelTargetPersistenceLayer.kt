package org.jetbrains.bazel.sync.environment

import com.intellij.openapi.project.Project
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.label.Label
import org.jetbrains.bsp.protocol.LibraryItem
import org.jetbrains.bsp.protocol.RawBuildTarget
import java.nio.file.Path

/**
 * Define the way of persisting bazel targets while sync
 */
@ApiStatus.Internal
interface BazelTargetPersistenceLayer {
  suspend fun saveAll(project: Project, spec: TargetPersistenceSpec)
  suspend fun notifyAll(project: Project)
}

@ApiStatus.Internal
data class TargetPersistenceSpec(
  val targets: List<RawBuildTarget>,
  val libraryItems: List<LibraryItem>,
  val file2Target: Map<Path, List<Label>>,
)
