package org.jetbrains.bazel.sdkcompat

import com.intellij.ide.starters.local.GeneratorAsset
import com.intellij.ide.starters.local.GeneratorFile

// remove with v243 sdkcompat
object StarterWizardCompat {
  fun startersEnabled() = true

  fun generatorFile(relativePath: String, content: String): GeneratorAsset = GeneratorFile(relativePath, content)
}
