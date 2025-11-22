package org.jetbrains.bazel.sync_new.lang

import com.intellij.openapi.extensions.ExtensionPointName
import org.jetbrains.bazel.sync_new.flow.SyncContext
import org.jetbrains.bsp.protocol.RawAspectTarget

interface SyncLanguagePlugin<T : SyncTargetData> {
  val languages: List<SyncLanguage>
  val languageDetectors: List<SyncLanguageDetector>
  val dataClasses: List<Class<out T>>

  suspend fun createTargetData(ctx: SyncContext, target: RawAspectTarget): T

  companion object {
    val ep: ExtensionPointName<SyncLanguagePlugin<*>> = ExtensionPointName.create("org.jetbrains.bazel.syncLanguagePlugin")
  }
}
