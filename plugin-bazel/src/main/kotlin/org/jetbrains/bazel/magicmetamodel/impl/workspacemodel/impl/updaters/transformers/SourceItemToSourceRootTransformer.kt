package org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import org.jetbrains.bazel.sdkcompat.workspacemodel.entities.WorkspaceModelEntity
import org.jetbrains.bsp.protocol.SourceItem
import java.nio.file.Path

data class SourceRoot(
  val sourcePath: Path,
  val generated: Boolean,
  val jvmPackagePrefix: String? = null,
) : WorkspaceModelEntity()

object SourceItemToSourceRootTransformer :
  WorkspaceModelEntityTransformer<SourceItem, SourceRoot> {
  override fun transform(inputEntity: SourceItem): SourceRoot {
    val sourcePath = inputEntity.path

    return SourceRoot(
      sourcePath = sourcePath,
      generated = inputEntity.generated,
      jvmPackagePrefix = inputEntity.jvmPackagePrefix,
    )
  }
}
