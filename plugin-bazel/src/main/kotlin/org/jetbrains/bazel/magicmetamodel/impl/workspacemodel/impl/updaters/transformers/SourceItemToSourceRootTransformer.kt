package org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import org.jetbrains.bazel.utils.safeCastToURI
import org.jetbrains.bazel.workspacemodel.entities.WorkspaceModelEntity
import org.jetbrains.bsp.protocol.SourceItem
import org.jetbrains.bsp.protocol.SourceItemKind
import java.nio.file.Path
import kotlin.io.path.toPath

data class SourceRoot(
  val sourcePath: Path,
  val generated: Boolean,
  val isFile: Boolean,
  val jvmPackagePrefix: String? = null,
) : WorkspaceModelEntity()

internal object SourceItemToSourceRootTransformer :
  WorkspaceModelEntityTransformer<SourceItem, SourceRoot> {
  override fun transform(inputEntity: SourceItem): SourceRoot {
    val sourcePath = inputEntity.uri.safeCastToURI().toPath()

    return SourceRoot(
      sourcePath = sourcePath,
      generated = inputEntity.generated,
      isFile = inputEntity.kind == SourceItemKind.FILE,
      jvmPackagePrefix = inputEntity.jvmPackagePrefix,
    )
  }
}
