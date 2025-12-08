package org.jetbrains.bazel.bazelrunner.params

import org.jetbrains.bazel.commons.constants.Constants.NAME
import org.jetbrains.bazel.commons.constants.Constants.VERSION

object BazelFlag {
  @JvmStatic fun runUnder(command: String) = arg("run_under", command)

  @JvmStatic fun color(enabled: Boolean) = arg("color", if (enabled) "yes" else "no")

  @JvmStatic fun keepGoing() = flag("keep_going")

  @JvmStatic fun javaTestDebug() = flag("java_debug")

  @JvmStatic fun outputGroups(groups: List<String>) = arg("output_groups", groups.joinToString(","))

  @JvmStatic fun aspect(name: String) = arg("aspects", name)

  @JvmStatic fun buildManualTests(): String = flag("build_manual_tests")

  // Use file:// uri scheme for output paths in the build events.
  @JvmStatic fun buildEventBinaryPathConversion(enabled: Boolean): String =
    arg(
      "build_event_binary_file_path_conversion",
      enabled.toString(),
    )

  @JvmStatic fun curses(enabled: Boolean): String = arg("curses", if (enabled) "yes" else "no")

  @JvmStatic fun enableWorkspace(enabled: Boolean = true): String = arg("enable_workspace", enabled.toString())

  @JvmStatic fun testOutputAll(): String = arg("test_output", "all")

  @JvmStatic fun device(device: String): String = arg("device", device)

  @JvmStatic fun start(startType: String): String = arg("start", startType)

  @JvmStatic fun adb(adbPath: String): String = arg("adb", adbPath)

  @JvmStatic fun testFilter(filterExpression: String): String = arg("test_filter", filterExpression)

  @JvmStatic fun toolTag(): String = arg("tool_tag", "$NAME:$VERSION")

  @JvmStatic fun starlarkDebug(): String = flag("experimental_skylark_debug")

  @JvmStatic fun starlarkDebugPort(port: Int): String = arg("experimental_skylark_debug_server_port", port.toString())

  @JvmStatic fun noBuild(): String = flag("nobuild")

  @JvmStatic fun combinedReportLcov(): String = arg("combined_report", "lcov")

  @JvmStatic fun instrumentationFilter(filter: String): String = arg("instrumentation_filter", filter)

  private fun arg(name: String, value: String) = String.format("--%s=%s", name, value)

  private fun flag(name: String) = "--$name"
}
