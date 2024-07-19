package org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.toPath
import org.jetbrains.plugins.bsp.utils.safeCastToURI

internal object RawUriToDirectoryPathTransformer :
    WorkspaceModelEntityBaseTransformer<String, Path> {
  override fun transform(inputEntity: String): Path {
    val path = inputEntity.safeCastToURI().toPath()

    return if (path.isDirectory()) path else path.parent
  }
}
