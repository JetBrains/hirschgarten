package org.jetbrains.bazel.magicmetamodel

import com.intellij.openapi.roots.ProjectModelExternalSource
import org.jetbrains.annotations.ApiStatus

@ApiStatus.Internal
interface MagicMetaModelEnvironment {
  val externalProjectModelSource: ProjectModelExternalSource
}
