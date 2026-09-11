package org.jetbrains.bazel

import com.jetbrains.cidr.lang.toolchains.CidrCompilerSwitches
import com.jetbrains.cidr.lang.workspace.OCCompilerSettings

fun OCCompilerSettings.lookupCompilerSwitch(flag: String): List<String> {
  val switches = getCompilerSwitches() ?: return emptyList()

  return switches.getList(CidrCompilerSwitches.Format.BASH_SHELL)
    .map { it.trimStart('-') }
    .filter { it.startsWith(flag) }
    .map { it.substring(flag.length).trimStart('=') }
}
