package org.jetbrains.bazel.magicmetamodel

import com.intellij.openapi.roots.ProjectModelExternalSource

internal class DefaultMagicMetaModelEnvironment : MagicMetaModelEnvironment {
  override val externalProjectModelSource: ProjectModelExternalSource
    get() = BazelProjectModelExternalSource
}
