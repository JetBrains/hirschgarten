package org.jetbrains.bazel.run.test

import com.intellij.execution.Executor
import com.intellij.execution.Location
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.testframework.sm.runner.SMTRunnerConsoleProperties
import com.intellij.execution.testframework.sm.runner.SMTestLocator
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.config.BazelPluginConstants

internal class BazelTestConsoleProperties(
  config: RunConfiguration,
  executor: Executor,
) : SMTRunnerConsoleProperties(config, BazelPluginConstants.BAZEL_DISPLAY_NAME, executor) {
  override fun getTestLocator(): SMTestLocator = CombinedTestLocator
}

private object CombinedTestLocator : SMTestLocator {
  override fun getLocation(protocol: String, path: String, project: Project, scope: GlobalSearchScope): List<Location<*>> =
    chooseFirstTestLocatorProvider { it.getLocation(protocol, path, project, scope) }

  override fun getLocation(
    protocol: String,
    path: String,
    metainfo: String?,
    project: Project,
    scope: GlobalSearchScope,
  ): List<Location<*>> =
    chooseFirstTestLocatorProvider { it.getLocation(protocol, path, metainfo, project, scope) }

  override fun getLocation(stacktraceLine: String, project: Project, scope: GlobalSearchScope): List<Location<*>> =
    chooseFirstTestLocatorProvider { it.getLocation(stacktraceLine, project, scope) }

  private inline fun chooseFirstTestLocatorProvider(block: (locator: SMTestLocator) -> List<Location<*>>): List<Location<*>> =
    BazelTestLocatorProvider.testLocators().firstNotNullOfOrNull { locator ->
      block(locator).takeIf { it.isNotEmpty() }
    }.orEmpty()

  override fun isDumbAware(): Boolean = BazelTestLocatorProvider.testLocators().all { it.isDumbAware }
}

@ApiStatus.Internal
interface BazelTestLocatorProvider {
  fun getTestLocator(): SMTestLocator

  companion object {
    val ep = ExtensionPointName.create<BazelTestLocatorProvider>("org.jetbrains.bazel.testLocatorProvider")

    fun testLocators(): List<SMTestLocator> = ep.extensionList.map { it.getTestLocator() }
  }
}
