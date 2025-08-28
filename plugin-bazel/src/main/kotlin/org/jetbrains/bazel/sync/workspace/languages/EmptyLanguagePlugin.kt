package org.jetbrains.bazel.sync.workspace.languages

import org.jetbrains.bazel.sync.workspace.model.LanguageData
import org.jetbrains.bsp.protocol.RawBuildTarget

class EmptyLanguagePlugin : LanguagePlugin<LanguageData>() {
  override fun applyModuleData(moduleData: LanguageData, buildTarget: RawBuildTarget) {}
}
