package org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import ch.epfl.scala.bsp4j.SourceItemKind
import org.jetbrains.bsp.protocol.EnhancedSourceItem
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.WorkspaceModelEntity
import org.jetbrains.plugins.bsp.utils.safeCastToURI
import java.nio.file.Path
import kotlin.io.path.toPath

public data class SourceRoot(
  public val sourcePath: Path,
  public val generated: Boolean,
  public val isFile: Boolean,
  public val data: Any? = null,
) : WorkspaceModelEntity()

internal object SourceItemToSourceRootTransformer :
  WorkspaceModelEntityTransformer<EnhancedSourceItem, SourceRoot> {
  override fun transform(inputEntity: EnhancedSourceItem): SourceRoot {
    val sourcePath = inputEntity.uri.safeCastToURI().toPath()

    return SourceRoot(
      sourcePath = sourcePath,
      generated = inputEntity.generated,
      isFile = inputEntity.kind == SourceItemKind.FILE,
      data = inputEntity.data,
      )
  }
}
