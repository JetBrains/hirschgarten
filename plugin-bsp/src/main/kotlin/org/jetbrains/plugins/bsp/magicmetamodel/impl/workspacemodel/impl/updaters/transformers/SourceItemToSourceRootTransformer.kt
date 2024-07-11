package org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import ch.epfl.scala.bsp4j.SourceItem
import ch.epfl.scala.bsp4j.SourceItemKind
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.WorkspaceModelEntity
import org.jetbrains.plugins.bsp.utils.safeCastToURI
import java.nio.file.Path
import kotlin.io.path.toPath

public data class SourceRoot(
  public val sourcePath: Path,
  public val generated: Boolean,
  public val isFile: Boolean,
) : WorkspaceModelEntity()

internal object SourceItemToSourceRootTransformer :
  WorkspaceModelEntityTransformer<SourceItem, SourceRoot> {
  override fun transform(inputEntity: SourceItem): SourceRoot {
    val sourceURI = inputEntity.uri.safeCastToURI().toPath()

    return SourceRoot(sourceURI, inputEntity.generated, inputEntity.kind == SourceItemKind.FILE)
  }
}
