package org.jetbrains.bazel.sdkcompat

import com.intellij.ide.starters.local.GeneratorAsset

// remove with v243 sdkcompat
object StarterWizardCompat {
  fun startersEnabled() = false

  fun generatorFile(relativePath: String, content: String): GeneratorAsset = TODO("not implemented in 243")
}
