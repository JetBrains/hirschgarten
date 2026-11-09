package org.jetbrains.bazel.run.import

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.run.RunHandlerProvider
import org.jetbrains.bazel.run.RunHandlerProvider.Companion.ep

@ApiStatus.Internal
interface GooglePluginAwareRunHandlerProvider : RunHandlerProvider {
  /**
   * Respective handler name in the Google Bazel plugin that should be used when importing run configurations.
   */
  val googleHandlerId: String

  companion object {
    fun getRunHandlerProvider(googleHandlerId: String, bazelCommand: String): RunHandlerProvider? {
      val isTestCommand = isTestCommand(bazelCommand)
      return ep.extensionList.firstOrNull {
        it is GooglePluginAwareRunHandlerProvider && it.googleHandlerId == googleHandlerId && it.isTestHandler == isTestCommand
      }
    }

    fun isTestCommand(bazelCommand: String): Boolean {
      val isTestCommand = bazelCommand == "test" || bazelCommand == "coverage"
      return isTestCommand
    }
  }
}
