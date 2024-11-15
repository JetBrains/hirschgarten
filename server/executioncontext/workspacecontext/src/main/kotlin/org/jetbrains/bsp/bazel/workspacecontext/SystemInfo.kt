package org.jetbrains.bsp.bazel.workspacecontext

object SystemInfo {
  private val os = System.getProperty("os.name").lowercase()

  @JvmField
  val isWindows: Boolean = os.startsWith("windows")

  @JvmField
  val isLinux: Boolean = os.startsWith("linux")

  @JvmField
  val isMac: Boolean = os.startsWith("mac")
}
