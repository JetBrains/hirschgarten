package org.jetbrains.bsp.bazel.server.sync.languages

import org.jetbrains.bsp.bazel.server.model.LanguageData
import org.jetbrains.bsp.protocol.BuildTarget

class EmptyLanguagePlugin : LanguagePlugin<LanguageData>() {
  override fun applyModuleData(moduleData: LanguageData, buildTarget: BuildTarget) {}
}
