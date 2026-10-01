package org.jetbrains.bazel.jvm.run

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.run.BazelProcessHandler
import org.jetbrains.bazel.run.task.BazelTestTaskListener
import org.jetbrains.bazel.taskEvents.BazelTaskListener
import org.jetbrains.bsp.protocol.TestParams
import java.nio.file.Path

@ApiStatus.Internal
interface JvmTestRunnerExtension {
  fun isApplicable(project: Project, targets: List<Label>): Boolean

  val isIdBasedTestTree: Boolean

  val testRunnerEmitsServiceMessages: Boolean

  fun transformTestParams(params: TestParams): TestParams

  fun transformScriptPathEnvAndTestFilter(env: Map<String, String>, testFilter: String?): Pair<Map<String, String>, String?>

  fun createTaskListener(handler: BazelProcessHandler, coverageReportListener: ((Path) -> Unit)?): BazelTaskListener

  companion object {
    val ep: ExtensionPointName<JvmTestRunnerExtension> = ExtensionPointName("org.jetbrains.bazel.jvmTestRunnerExtension")

    fun getInstance(project: Project, targets: List<Label>): JvmTestRunnerExtension =
      ep.extensionList.first { it.isApplicable(project, targets) }
  }
}

internal class DefaultJvmTestRunnerExtension : JvmTestRunnerExtension {
  override fun isApplicable(project: Project, targets: List<Label>): Boolean = true

  override val isIdBasedTestTree: Boolean get() = false

  override val testRunnerEmitsServiceMessages: Boolean get() = false

  override fun transformTestParams(params: TestParams): TestParams = params

  override fun transformScriptPathEnvAndTestFilter(env: Map<String, String>, testFilter: String?): Pair<Map<String, String>, String?> =
    env to testFilter

  override fun createTaskListener(handler: BazelProcessHandler, coverageReportListener: ((Path) -> Unit)?): BazelTaskListener =
    BazelTestTaskListener(handler, coverageReportListener)
}
