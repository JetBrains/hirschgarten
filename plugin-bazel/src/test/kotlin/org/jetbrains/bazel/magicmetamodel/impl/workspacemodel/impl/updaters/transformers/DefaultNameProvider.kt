package org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.magicmetamodel.TargetNameReformatProvider

object DefaultNameProvider : TargetNameReformatProvider {
  override fun invoke(targetInfo: Label): String = targetInfo.toString()
}
