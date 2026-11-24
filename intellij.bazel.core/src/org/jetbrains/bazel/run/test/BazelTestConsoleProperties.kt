package org.jetbrains.bazel.run.test

import com.intellij.execution.Executor
import com.intellij.execution.Location
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.testframework.TestConsoleProperties
import com.intellij.execution.testframework.actions.AbstractRerunFailedTestsAction
import com.intellij.execution.testframework.sm.SMCustomMessagesParsing
import com.intellij.execution.testframework.sm.runner.OutputToGeneralTestEventsConverter
import com.intellij.execution.testframework.sm.runner.SMTRunnerConsoleProperties
import com.intellij.execution.testframework.sm.runner.SMTestLocator
import com.intellij.execution.testframework.sm.runner.ui.SMTRunnerConsoleView
import com.intellij.execution.ui.ConsoleView
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ModificationTracker
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.terminal.TerminalExecutionConsole
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.config.BazelPluginConstants
import org.jetbrains.bazel.run.BazelRerunFailedTestsAction
import org.jetbrains.bazel.run.BazelTestRerunExtension
import org.jetbrains.bazel.run.config.BazelRunConfiguration

internal class BazelTestConsoleProperties(
  config: BazelRunConfiguration,
  executor: Executor,
) : SMTRunnerConsoleProperties(config, BazelPluginConstants.BAZEL_DISPLAY_NAME, executor), SMCustomMessagesParsing {
  var usePty: Boolean = false

  override fun getTestLocator(): SMTestLocator = CombinedTestLocator

  override fun createConsole(): ConsoleView {
    if (!usePty) return super.createConsole()
    return object : TerminalExecutionConsole(project, null) {
      override fun attachToProcess(processHandler: ProcessHandler) {
        super.attachToProcess(processHandler, false)
      }
    }
  }

  override fun createTestEventsConverter(
    testFrameworkName: String,
    consoleProperties: TestConsoleProperties,
  ): OutputToGeneralTestEventsConverter =
    OutputToGeneralTestEventsConverter(
      testFrameworkName,
      // by default, the converter joins the text until a line break, and the terminal turns a `\r` inside a text into a line break.
      // Bazel ends a progress line with `\r` to write it again, so the terminal must get each chunk at once.
      isEditable || usePty, false, !isIdBasedTestTree,
    )

  override fun createRerunFailedTestsAction(consoleView: ConsoleView): AbstractRerunFailedTestsAction? {
    val testRerunExtension = BazelTestRerunExtension.getInstance(configuration as BazelRunConfiguration) ?: return null
    return BazelRerunFailedTestsAction(consoleView as SMTRunnerConsoleView, testRerunExtension)
  }
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

  override fun getLocationCacheModificationTracker(project: Project): ModificationTracker =
    CompositeModificationTracker(BazelTestLocatorProvider.testLocators().map { it.getLocationCacheModificationTracker(project) })

  override fun isDumbAware(): Boolean = BazelTestLocatorProvider.testLocators().all { it.isDumbAware }
}

@ApiStatus.Internal
class CompositeModificationTracker(private val trackers: List<ModificationTracker>) : ModificationTracker {
  override fun getModificationCount(): Long = trackers.sumOf { it.modificationCount }
}

@ApiStatus.Internal
interface BazelTestLocatorProvider {
  fun getTestLocator(): SMTestLocator

  companion object {
    val ep = ExtensionPointName.create<BazelTestLocatorProvider>("org.jetbrains.bazel.testLocatorProvider")

    fun testLocators(): List<SMTestLocator> = ep.extensionList.map { it.getTestLocator() }
  }
}
