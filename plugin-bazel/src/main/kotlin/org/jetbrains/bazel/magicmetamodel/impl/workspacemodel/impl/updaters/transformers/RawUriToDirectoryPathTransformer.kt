package org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import org.jetbrains.bazel.utils.safeCastToURI
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.toPath

object RawUriToDirectoryPathTransformer : WorkspaceModelEntityBaseTransformer<String, Path> {
  override fun transform(inputEntity: String): Path {
    val path = inputEntity.safeCastToURI().toPath()

    return if (path.isDirectory()) path else path.parent
  }
}
