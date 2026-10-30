package org.jetbrains.bazel.clion.run.debug

import com.google.devtools.intellij.ideinfo.IntellijIdeInfo.CcDebugInfo
import com.google.devtools.intellij.ideinfo.IntellijIdeInfo.PlatformInfo
import com.google.devtools.intellij.ideinfo.IntellijIdeInfo.TargetIdeInfo
import com.intellij.build.events.MessageEvent
import com.intellij.openapi.util.NlsSafe
import com.intellij.util.system.LowLevelLocalMachineAccess
import com.intellij.util.system.OS
import com.jetbrains.cidr.lang.workspace.compiler.OCCompilerId
import com.jetbrains.cidr.lang.workspace.compiler.OCCompilerKind
import com.jetbrains.cidr.lang.workspace.compiler.hasAnyId
import com.jetbrains.cidr.lang.workspace.compiler.hasId
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.annotations.VisibleForTesting
import org.jetbrains.bazel.clion.BazelCLionCoreBundle
import org.jetbrains.bazel.clion.workspace.CcCompilerInfo
import org.jetbrains.bazel.clion.workspace.CcCompilerInfoService
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.label.label

private val MSVC_DEBUG_INFO_FLAGS = setOf("/Z7", "/Zi", "/ZI")

private val GNU_COMPILERS = setOf(OCCompilerId.GCC, OCCompilerId.CLANG, OCCompilerId.APPLE_CLANG)

// the GCC and Clang flags that set the debug level, a debugger tuning like -glldb also enables the debug info
private val GCC_DEBUG_LEVEL_FLAG = Regex("-g(\\d|gdb\\d?|dwarf(-\\d)?|lldb|sce|dbx|line-tables-only|line-directives-only)?")

@ApiStatus.Internal
class CcDebugInfoCheck(private val ctx: CcDebugContext, private val compilers: List<CcCompilerInfo>) {

  data class Issue(val label: Label, val action: CcDebugInfo.Action, val compilationMode: CcDebugInfo.CompilationMode) {

    override fun toString(): @NlsSafe String {
      return "$label (${action.mnemonic}, ${compilationMode.name.lowercase()}): ${action.argumentsList.joinToString(" ")}"
    }
  }

  data class Result(val issues: List<Issue>) {

    /** Returns true if `--compilation_mode=dbg` might add the missing debug info. */
    val isFixableByCompilationMode: Boolean get() = issues.all { it.compilationMode != CcDebugInfo.CompilationMode.DBG }
  }

  /** Returns null if no toolchain of the last sync uses the executable of the action. */
  private fun resolveCompilerKind(action: CcDebugInfo.Action): OCCompilerKind? {
    val executable = action.argumentsList.firstOrNull() ?: return null

    return compilers.firstNotNullOfOrNull {
      when {
        it.cCompiler.endsWith(executable) -> it.cCompilerKind
        it.cppCompiler.endsWith(executable) -> it.cppCompilerKind
        else -> null
      }
    }
  }

  /**
   * Returns the compiler kind of the linker action, assumes that the linker matches the compiler or
   * the compiler is used for linking as well.
   */
  private fun resolveLinkerKind(target: TargetIdeInfo): OCCompilerKind? {
    return target.compileActions().firstNotNullOfOrNull(::resolveCompilerKind)
           ?: target.linkActions().firstNotNullOfOrNull(::resolveCompilerKind)
  }

  /** Reports the issues as one warning. */
  fun check(targets: Collection<TargetIdeInfo>): Result {
    val result = findDebugInfoIssues(targets)
    if (result.issues.isNotEmpty()) reportIssues(result)

    return result
  }

  private fun findDebugInfoIssues(targets: Collection<TargetIdeInfo>): Result {
    val compileIssues = targets.asSequence().flatMap { target ->
      target.compileActions()
        .filter { !it.hasCompileDebugInfo(resolveCompilerKind(it)) }
        .map { Issue(target.label(), it, target.compilationMode()) }
    }

    val linkIssues = targets.asSequence().flatMap { target ->
      target.linkActions()
        .filter { !it.hasLinkDebugInfo(resolveLinkerKind(target), target.os()) }
        .map { Issue(target.label(), it, target.compilationMode()) }
    }

    return Result((compileIssues + linkIssues).toList())
  }

  private fun reportIssues(result: Result) {
    val targetCount = result.issues.map { it.label }.distinct().size

    ctx.reportEvent(
      severity = MessageEvent.Kind.WARNING,
      message = BazelCLionCoreBundle.message("debug.warning.no.debug.info", targetCount),
      description = result.issues.joinToString("\n"),
    )
  }
}

@ApiStatus.Internal
context(ctx: CcDebugContext)
fun checkCcDebugInfo(targets: Collection<TargetIdeInfo>): CcDebugInfoCheck.Result {
  val compilers = CcCompilerInfoService.getInstance(ctx.project).all().values.toList()
  return CcDebugInfoCheck(ctx, compilers).check(targets)
}

@VisibleForTesting
@ApiStatus.Internal
fun CcDebugInfo.Action.hasCompileDebugInfo(compilerKind: OCCompilerKind?): Boolean = when {
  compilerKind.hasAnyId(GNU_COMPILERS) -> {
    hasGccDebugInfo(argumentsList)
  }

  compilerKind.hasId(OCCompilerId.MSVC) -> {
    hasMsvcDebugInfo(argumentsList)
  }

  compilerKind.hasId(OCCompilerId.CLANG_CL) -> {
    hasGccDebugInfo(argumentsList) || hasClangClDebugInfo(argumentsList) || hasMsvcDebugInfo(argumentsList)
  }

  else -> {
    hasGccDebugInfo(argumentsList) || hasMsvcDebugInfo(argumentsList)
  }
}

/** The last flag that sets the debug level wins. The other `-g` flags, only change the format of the debug info. */
private fun hasGccDebugInfo(arguments: List<String>): Boolean {
  val last = arguments.lastOrNull { GCC_DEBUG_LEVEL_FLAG.matches(it) } ?: return false
  return last != "-g0" && last != "-ggdb0"
}

private fun hasClangClDebugInfo(arguments: List<String>): Boolean {
  return hasGccDebugInfo(arguments.map { it.toMsvcOption() }.filter { it.startsWith("/clang:") }.map { it.removePrefix("/clang:") })
}

/** The compiler options are case-sensitive, for example `/Zi` and `/ZI` are different options. */
private fun hasMsvcDebugInfo(arguments: List<String>): Boolean {
  return arguments.any { it.toMsvcOption() in MSVC_DEBUG_INFO_FLAGS }
}

/** MSVC accepts each option with a `/` or a `-` prefix. */
private fun String.toMsvcOption(): String {
  return if (startsWith('-')) "/" + substring(1) else this
}

@VisibleForTesting
@ApiStatus.Internal
fun CcDebugInfo.Action.hasLinkDebugInfo(compilerKind: OCCompilerKind?, os: OS): Boolean = when {
  compilerKind.hasAnyId(GNU_COMPILERS) && os == OS.macOS -> {
    // on macOS the binary refers to the object files, and lldb finds them only with a relative path
    !hasStripFlag(argumentsList) && argumentsList.any { it.contains("-oso_prefix,") }
  }

  compilerKind.hasAnyId(GNU_COMPILERS) -> {
    !hasStripFlag(argumentsList)
  }

  compilerKind.hasId(OCCompilerId.MSVC) -> {
    hasMsvcDebugLinkFlag(argumentsList)
  }

  else -> {
    true
  }
}

/** Bazel adds `-Wl,-S` for `--strip=always`, the linker then removes the debug info. */
private fun hasStripFlag(arguments: List<String>): Boolean {
  return arguments.any { it.startsWith("-Wl,") && "-S" in it.removePrefix("-Wl,").split(',') }
}

/** The last /DEBUG flag wins, a plain /DEBUG is /DEBUG:FULL. The linker options are not case-sensitive. */
private fun hasMsvcDebugLinkFlag(arguments: List<String>): Boolean {
  val last = arguments.map { it.toMsvcOption().uppercase() }.lastOrNull { it == "/DEBUG" || it.startsWith("/DEBUG:") }
  return last == "/DEBUG" || last == "/DEBUG:FULL"
}

private fun TargetIdeInfo.compileActions(): List<CcDebugInfo.Action> {
  return if (hasCcDebugInfo()) ccDebugInfo.compileActionsList else emptyList()
}

private fun TargetIdeInfo.linkActions(): List<CcDebugInfo.Action> {
  return if (hasCcDebugInfo()) ccDebugInfo.linkActionsList else emptyList()
}

@OptIn(LowLevelLocalMachineAccess::class)
private fun TargetIdeInfo.os(): OS = when (platformInfo.os) {
  PlatformInfo.OS.LINUX -> OS.Linux
  PlatformInfo.OS.MACOS -> OS.macOS
  PlatformInfo.OS.WINDOWS -> OS.Windows
  else -> OS.CURRENT
}

private fun TargetIdeInfo.compilationMode(): CcDebugInfo.CompilationMode {
  return ccDebugInfo?.compilationMode ?: CcDebugInfo.CompilationMode.UNRECOGNIZED
}
