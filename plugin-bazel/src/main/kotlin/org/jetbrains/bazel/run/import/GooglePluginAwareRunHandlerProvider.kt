package org.jetbrains.bazel.run.import

import org.jetbrains.bazel.run.RunHandlerProvider
import org.jetbrains.bazel.run.RunHandlerProvider.Companion.ep

interface GooglePluginAwareRunHandlerProvider : RunHandlerProvider {
  /**
   * Respective handler name in the Google Bazel plugin that should be used when importing run configurations.
   */
  val googleHandlerId: String

  val isTestHandler: Boolean

  companion object {
    fun getRunHandlerProvider(googleHandlerId: String, bazelCommand: String): RunHandlerProvider? {
      val isTestCommand = bazelCommand == "test" || bazelCommand == "coverage"
      return ep.extensionList.firstOrNull {
        it is GooglePluginAwareRunHandlerProvider && it.googleHandlerId == googleHandlerId && it.isTestHandler == isTestCommand
      }
    }
  }
}
