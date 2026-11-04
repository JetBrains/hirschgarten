package org.jetbrains.bazel

import com.intellij.util.system.OS
import com.jetbrains.cidr.lang.toolchains.CidrCompilerSwitches
import com.jetbrains.cidr.lang.workspace.OCCompilerSettings

fun OCCompilerSettings.lookupCompilerSwitch(flag: String): List<String> {
  val switches = getCompilerSwitches() ?: return emptyList()

  return switches.getList(CidrCompilerSwitches.Format.BASH_SHELL)
    .map { it.trimStart('-') }
    .filter { it.startsWith(flag) }
    .map { it.substring(flag.length).trimStart('=') }
}

fun OCCompilerSettings.resolveCompilerSwitches(): List<String> {
  return compilerSwitches?.getList(CidrCompilerSwitches.Format.BASH_SHELL).orEmpty()
}

/** OS helper to avoid conflicts when importing OS from JUnit. */
fun isWindows(): Boolean = OS.CURRENT == OS.Windows

fun isLinux(): Boolean = OS.CURRENT == OS.Linux

fun isMacOS(): Boolean = OS.CURRENT == OS.macOS
