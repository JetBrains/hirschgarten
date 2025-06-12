package org.jetbrains.bazel.server.sync.languages

import org.jetbrains.bazel.server.model.LanguageData
import org.jetbrains.bsp.protocol.RawBuildTarget

class EmptyLanguagePlugin : LanguagePlugin<LanguageData>() {
  override fun applyModuleData(moduleData: LanguageData, buildTarget: RawBuildTarget) {}
}
