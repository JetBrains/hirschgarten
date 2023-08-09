package org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import com.intellij.util.io.isDirectory
import java.net.URI
import java.nio.file.Path
import kotlin.io.path.toPath

internal object RawUriToDirectoryPathTransformer : WorkspaceModelEntityBaseTransformer<String, Path> {
  override fun transform(inputEntity: String): Path {
    val path = URI.create(inputEntity).toPath()

    return if (path.isDirectory()) path else path.parent
  }
}
