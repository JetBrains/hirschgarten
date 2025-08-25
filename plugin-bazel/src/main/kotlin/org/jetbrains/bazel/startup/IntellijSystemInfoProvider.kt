package org.jetbrains.bazel.startup

import org.jetbrains.bazel.commons.SystemInfoProvider
import com.intellij.openapi.util.SystemInfo as IntellijSystemInfo

object IntellijSystemInfoProvider : SystemInfoProvider {
  override val isWindows: Boolean = IntellijSystemInfo.isWindows
  override val isMac: Boolean = IntellijSystemInfo.isMac
  override val isLinux: Boolean = IntellijSystemInfo.isLinux
  override val isAarch64: Boolean = IntellijSystemInfo.isAarch64
}
