package org.jetbrains.bazel.clion

import com.google.devtools.intellij.ideinfo.IntellijIdeInfo.CcDebugInfo
import com.google.devtools.intellij.ideinfo.IntellijIdeInfo.CcDebugInfo.CompilationMode
import com.google.devtools.intellij.ideinfo.IntellijIdeInfo.PlatformInfo
import com.google.devtools.intellij.ideinfo.IntellijIdeInfo.TargetIdeInfo
import com.google.devtools.intellij.ideinfo.IntellijIdeInfo.TargetKey
import com.intellij.build.events.MessageEvent
import com.intellij.openapi.project.ProjectManager
import com.intellij.testFramework.junit5.TestApplication
import com.intellij.util.system.OS
import com.jetbrains.cidr.lang.workspace.compiler.ClangClCompilerKind
import com.jetbrains.cidr.lang.workspace.compiler.MSVCCompilerKind
import com.jetbrains.cidr.lang.workspace.compiler.OCCompilerKind
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.bazel.clion.run.debug.CcDebugInfoCheck
import org.jetbrains.bazel.clion.run.debug.hasCompileDebugInfo
import org.jetbrains.bazel.clion.run.debug.hasLinkDebugInfo
import org.jetbrains.bazel.clion.workspace.CcCompilerInfo
import org.jetbrains.bazel.clion.workspace.CcCompilerKind
import org.jetbrains.bazel.fixtures.TestDebugContext
import org.jetbrains.bazel.fixtures.TestEvent
import org.jetbrains.bazel.label.Label
import org.jetbrains.bsp.protocol.OutputLocationCollection
import org.junit.jupiter.api.Test
import java.nio.file.Path

private val MAIN = Label.parse("@@//main:main")
private val LIB = Label.parse("@@//lib:lib")
private val EXTERNAL_LIB = Label.parse("@@external+//lib:lib")

private const val CC = "/usr/bin/cc"
private const val CL = "/msvc/bin/cl.exe"
private const val LINK = "/msvc/bin/link.exe"

@TestApplication
class CcDebugInfoCheckTest {

  @Test
  fun testGccCompile() {
    assertThat(compileAction("-g").hasCompileDebugInfo(CcCompilerKind.GCC)).isTrue()
    assertThat(compileAction("-g2").hasCompileDebugInfo(CcCompilerKind.GCC)).isTrue()
    assertThat(compileAction("-ggdb").hasCompileDebugInfo(CcCompilerKind.GCC)).isTrue()
    assertThat(compileAction("-O2").hasCompileDebugInfo(CcCompilerKind.GCC)).isFalse()
    assertThat(compileAction("-g0").hasCompileDebugInfo(CcCompilerKind.GCC)).isFalse()
    assertThat(compileAction("-ggdb0").hasCompileDebugInfo(CcCompilerKind.GCC)).isFalse()
  }

  @Test
  fun testGccCompileLastFlagWins() {
    assertThat(compileAction("-g2", "-g0").hasCompileDebugInfo(CcCompilerKind.GCC)).isFalse()
    assertThat(compileAction("-g0", "-g2").hasCompileDebugInfo(CcCompilerKind.GCC)).isTrue()
  }

  @Test
  fun testGccCompileIgnoresNegativeFlags() {
    assertThat(compileAction("-gno-column-info").hasCompileDebugInfo(CcCompilerKind.GCC)).isFalse()
    assertThat(compileAction("-g", "-gno-column-info").hasCompileDebugInfo(CcCompilerKind.GCC)).isTrue()
  }

  @Test
  fun testGccCompileIgnoresFormatFlags() {
    assertThat(compileAction("-gz").hasCompileDebugInfo(CcCompilerKind.GCC)).isFalse()
    assertThat(compileAction("-g0", "-gz").hasCompileDebugInfo(CcCompilerKind.GCC)).isFalse()
    assertThat(compileAction("-g0", "-gcolumn-info").hasCompileDebugInfo(CcCompilerKind.GCC)).isFalse()
    assertThat(compileAction("-gsplit-dwarf").hasCompileDebugInfo(CcCompilerKind.GCC)).isFalse()
    assertThat(compileAction("-g", "-gsplit-dwarf").hasCompileDebugInfo(CcCompilerKind.GCC)).isTrue()
  }

  @Test
  fun testGccCompileLevelFlags() {
    assertThat(compileAction("-gdwarf-4").hasCompileDebugInfo(CcCompilerKind.GCC)).isTrue()
    assertThat(compileAction("-gline-tables-only").hasCompileDebugInfo(CcCompilerKind.CLANG)).isTrue()
    assertThat(compileAction("-ggdb3").hasCompileDebugInfo(CcCompilerKind.GCC)).isTrue()
    assertThat(compileAction("-glldb").hasCompileDebugInfo(CcCompilerKind.CLANG)).isTrue()
  }

  @Test
  fun testClangCompileIgnoresGccToolchain() {
    assertThat(compileAction("-gcc-toolchain", "/usr").hasCompileDebugInfo(CcCompilerKind.CLANG)).isFalse()
  }

  @Test
  fun testClangCompile() {
    assertThat(compileAction("-g").hasCompileDebugInfo(CcCompilerKind.CLANG)).isTrue()
    assertThat(compileAction("-g").hasCompileDebugInfo(CcCompilerKind.APPLE_CLANG)).isTrue()
    assertThat(compileAction("-O2").hasCompileDebugInfo(CcCompilerKind.APPLE_CLANG)).isFalse()
  }

  @Test
  fun testMsvcCompile() {
    assertThat(compileAction("/Z7").hasCompileDebugInfo(MSVCCompilerKind)).isTrue()
    assertThat(compileAction("/Zi").hasCompileDebugInfo(MSVCCompilerKind)).isTrue()
    assertThat(compileAction("/Zc:__cplusplus").hasCompileDebugInfo(MSVCCompilerKind)).isFalse()
    assertThat(compileAction("/O2").hasCompileDebugInfo(MSVCCompilerKind)).isFalse()
    assertThat(compileAction("-Z7").hasCompileDebugInfo(MSVCCompilerKind)).isTrue()
  }

  @Test
  fun testClangClCompile() {
    assertThat(compileAction("/Z7").hasCompileDebugInfo(ClangClCompilerKind)).isTrue()
    assertThat(compileAction("/clang:-g").hasCompileDebugInfo(ClangClCompilerKind)).isTrue()
    assertThat(compileAction("/clang:-g0").hasCompileDebugInfo(ClangClCompilerKind)).isFalse()
    assertThat(compileAction("-clang:-g").hasCompileDebugInfo(ClangClCompilerKind)).isTrue()
    assertThat(compileAction("/O2").hasCompileDebugInfo(ClangClCompilerKind)).isFalse()
  }

  @Test
  fun testUnknownCompile() {
    assertThat(compileAction("-g").hasCompileDebugInfo(null)).isTrue()
    assertThat(compileAction("/Z7").hasCompileDebugInfo(null)).isTrue()
    assertThat(compileAction("-g0").hasCompileDebugInfo(null)).isFalse()
  }

  @Test
  fun testLinkMacOS() {
    assertThat(linkAction("-Wl,-oso_prefix,.").hasLinkDebugInfo(CcCompilerKind.CLANG, OS.macOS)).isTrue()
    assertThat(linkAction("-Wl,-oso_prefix,__BAZEL_EXECUTION_ROOT__/").hasLinkDebugInfo(CcCompilerKind.APPLE_CLANG, OS.macOS)).isTrue()
    assertThat(linkAction("-lc++").hasLinkDebugInfo(CcCompilerKind.APPLE_CLANG, OS.macOS)).isFalse()
  }

  @Test
  fun testLinkOtherOS() {
    assertThat(linkAction("-lc++").hasLinkDebugInfo(CcCompilerKind.CLANG, OS.Linux)).isTrue()
    assertThat(linkAction("-lstdc++").hasLinkDebugInfo(CcCompilerKind.GCC, OS.macOS)).isFalse()
  }

  @Test
  fun testLinkStripped() {
    assertThat(linkAction("-Wl,-S").hasLinkDebugInfo(CcCompilerKind.GCC, OS.Linux)).isFalse()
    assertThat(linkAction("-Wl,-S").hasLinkDebugInfo(CcCompilerKind.CLANG, OS.Linux)).isFalse()
    assertThat(linkAction("-Wl,--gc-sections,-S").hasLinkDebugInfo(CcCompilerKind.GCC, OS.Linux)).isFalse()
    assertThat(linkAction("-Wl,-oso_prefix,.", "-Wl,-S").hasLinkDebugInfo(CcCompilerKind.APPLE_CLANG, OS.macOS)).isFalse()
    assertThat(linkAction("-Wl,-Size").hasLinkDebugInfo(CcCompilerKind.GCC, OS.Linux)).isTrue()
  }

  @Test
  fun testMsvcLink() {
    assertThat(linkAction("/DEBUG:FULL").hasLinkDebugInfo(MSVCCompilerKind, OS.Windows)).isTrue()
    assertThat(linkAction("/DEBUG").hasLinkDebugInfo(MSVCCompilerKind, OS.Windows)).isTrue()
    assertThat(linkAction("/DEBUG:FASTLINK").hasLinkDebugInfo(MSVCCompilerKind, OS.Windows)).isFalse()
    assertThat(linkAction("/DEBUG:FULL", "/DEBUG:FASTLINK").hasLinkDebugInfo(MSVCCompilerKind, OS.Windows)).isFalse()
    assertThat(linkAction("/OPT:REF").hasLinkDebugInfo(MSVCCompilerKind, OS.Windows)).isFalse()
    assertThat(linkAction("/DEBUG:NONE").hasLinkDebugInfo(MSVCCompilerKind, OS.Windows)).isFalse()
  }

  @Test
  fun testMsvcLinkOptionSpelling() {
    assertThat(linkAction("/debug").hasLinkDebugInfo(MSVCCompilerKind, OS.Windows)).isTrue()
    assertThat(linkAction("-DEBUG:FULL").hasLinkDebugInfo(MSVCCompilerKind, OS.Windows)).isTrue()
    assertThat(linkAction("/DEBUG:FULL", "/DEBUGTYPE:CV").hasLinkDebugInfo(MSVCCompilerKind, OS.Windows)).isTrue()
  }

  @Test
  fun testNoIssues() {
    val (events, result) = check(
      CcCompilerKind.GCC,
      target(MAIN, compile = listOf(compileAction("-g")), link = listOf(linkAction("-lstdc++"))),
      target(LIB, compile = listOf(compileAction("-g"))),
    )

    assertThat(events).isEmpty()
    assertThat(result.issues).isEmpty()
  }

  @Test
  fun testCompileIssues() {
    val (events, result) = check(
      CcCompilerKind.GCC,
      target(MAIN, compile = listOf(compileAction("-g"))),
      target(LIB, compile = listOf(compileAction("-O2"), compileAction("-O2", "-c", "other.cc"))),
    )

    assertThat(result.issues.map { it.label }).containsExactly(LIB, LIB)

    val event = events.single()
    assertThat(event.severity).isEqualTo(MessageEvent.Kind.WARNING)
    assertThat(event.message).isEqualTo("1 target without debug info")
    assertThat(event.description).isEqualTo("$LIB (CppCompile, fastbuild): $CC -O2\n$LIB (CppCompile, fastbuild): $CC -O2 -c other.cc")
  }

  @Test
  fun testLinkIssuesOfEachTarget() {
    val (_, result) = check(
      CcCompilerKind.APPLE_CLANG,
      target(MAIN, compile = listOf(compileAction("-g")), link = listOf(linkAction("-lc++")), os = PlatformInfo.OS.MACOS),
      target(LIB, link = listOf(linkAction("-lc++")), os = PlatformInfo.OS.MACOS),
    )

    assertThat(result.issues.map { it.label to it.action.mnemonic }).containsExactly(MAIN to "CppLink", LIB to "CppLink")
  }

  @Test
  fun testChecksExternalTargets() {
    val (_, result) = check(
      CcCompilerKind.GCC,
      target(MAIN, compile = listOf(compileAction("-g"))),
      target(EXTERNAL_LIB, compile = listOf(compileAction("-O2"))),
    )

    assertThat(result.issues.map { it.label }).containsExactly(EXTERNAL_LIB)
  }

  @Test
  fun testMissingMainTarget() {
    // for example, the target to debug is a py_test that starts a cc_binary
    val (_, result) = check(
      CcCompilerKind.APPLE_CLANG,
      target(
        LIB,
        compile = listOf(compileAction("-O2")),
        link = listOf(linkAction("-lc++")),
        mode = CompilationMode.OPT,
        os = PlatformInfo.OS.MACOS,
      ),
    )

    assertThat(result.issues.map { it.label to it.action.mnemonic }).containsExactly(LIB to "CppCompile", LIB to "CppLink")
    assertThat(result.issues.map { it.compilationMode }).containsOnly(CompilationMode.OPT)
  }

  @Test
  fun testTargetsWithoutDebugInfo() {
    val (events, result) = check(
      CcCompilerKind.GCC,
      TargetIdeInfo.newBuilder().setKey(TargetKey.newBuilder().setLabel(MAIN.toString())).build(),
    )

    assertThat(events).isEmpty()
    assertThat(result.issues).isEmpty()
  }

  @Test
  fun testCompilationModeOfEachIssue() {
    val (_, result) = check(
      CcCompilerKind.GCC,
      target(LIB, compile = listOf(compileAction("-O2")), mode = CompilationMode.OPT),
      target(MAIN, compile = listOf(compileAction("-O2")), mode = CompilationMode.DBG),
    )

    assertThat(result.issues.map { it.label to it.compilationMode }).containsExactly(
      LIB to CompilationMode.OPT,
      MAIN to CompilationMode.DBG,
    )
  }

  @Test
  fun testFixableByCompilationMode() {
    fun result(vararg modes: CompilationMode): CcDebugInfoCheck.Result {
      return CcDebugInfoCheck.Result(modes.map { CcDebugInfoCheck.Issue(MAIN, compileAction("-O2"), it) })
    }

    assertThat(result(CompilationMode.FASTBUILD).isFixableByCompilationMode).isTrue()
    assertThat(result(CompilationMode.OPT, CompilationMode.FASTBUILD).isFixableByCompilationMode).isTrue()
    assertThat(result(CompilationMode.FASTBUILD, CompilationMode.DBG).isFixableByCompilationMode).isFalse()
    assertThat(result(CompilationMode.DBG).isFixableByCompilationMode).isFalse()
  }

  @Test
  fun testCompilerKindByExecutable() {
    val (_, result) = check(
      listOf(compiler(CC, CcCompilerKind.GCC), compiler(CL, MSVCCompilerKind)),
      target(MAIN, compile = listOf(compileAction("/Zi", executable = CL))),
      target(LIB, compile = listOf(compileAction("/Zi"))),
    )

    assertThat(result.issues.map { it.label }).containsExactly(LIB)
  }

  @Test
  fun testUnknownExecutable() {
    val (events, result) = check(
      emptyList(),
      target(MAIN, compile = listOf(compileAction("/Zi")), link = listOf(linkAction("-lc++")), os = PlatformInfo.OS.MACOS),
      target(LIB, compile = listOf(compileAction("-g"))),
    )

    assertThat(events).isEmpty()
    assertThat(result.issues).isEmpty()
  }

  @Test
  fun testRelativeExecutable() {
    val (_, result) = check(
      listOf(compiler("/output/external/cc/cc_wrapper.sh", CcCompilerKind.APPLE_CLANG)),
      target(MAIN, link = listOf(linkAction("-lc++", executable = "external/cc/cc_wrapper.sh")), os = PlatformInfo.OS.MACOS),
    )

    assertThat(result.issues.map { it.label to it.action.mnemonic }).containsExactly(MAIN to "CppLink")
  }

  @Test
  fun testActionWithoutArguments() {
    val action = CcDebugInfo.Action.newBuilder().setMnemonic("CppCompile").build()

    val (_, result) = check(CcCompilerKind.GCC, target(MAIN, compile = listOf(action)))

    assertThat(result.issues.map { it.label }).containsExactly(MAIN)
  }

  @Test
  fun testToolchainsShareCompiler() {
    val (_, result) = check(
      listOf(compiler(CC, CcCompilerKind.CLANG), compiler(CC, CcCompilerKind.CLANG), compiler(CL, MSVCCompilerKind)),
      target(MAIN, compile = listOf(compileAction("/Zi", executable = CL))),
      target(LIB, compile = listOf(compileAction("/Zi"))),
    )

    assertThat(result.issues.map { it.label }).containsExactly(LIB)
  }

  @Test
  fun testLinkerKindFromCompileAction() {
    val (_, result) = check(
      listOf(compiler(CL, MSVCCompilerKind)),
      target(
        MAIN,
        compile = listOf(compileAction("/Z7", executable = CL)),
        link = listOf(linkAction("/OPT:REF", executable = LINK)),
        os = PlatformInfo.OS.WINDOWS,
      ),
    )

    assertThat(result.issues.map { it.label to it.action.mnemonic }).containsExactly(MAIN to "CppLink")
  }

  @Test
  fun testLinkerKindFromLinkAction() {
    val (_, result) = check(
      CcCompilerKind.GCC,
      target(MAIN, compile = listOf(compileAction("-g", executable = "/opt/bin/cc")), link = listOf(linkAction("-Wl,-S"))),
    )

    assertThat(result.issues.map { it.label to it.action.mnemonic }).containsExactly(MAIN to "CppLink")
  }

  @Test
  fun testUnknownLinkerKind() {
    val (events, result) = check(
      emptyList(),
      target(MAIN, link = listOf(linkAction("-Wl,-S", executable = "/opt/bin/ld"))),
      target(LIB, link = listOf(linkAction("/OPT:REF", executable = LINK)), os = PlatformInfo.OS.WINDOWS),
    )

    assertThat(events).isEmpty()
    assertThat(result.issues).isEmpty()
  }

  private fun check(compilerKind: OCCompilerKind, vararg targets: TargetIdeInfo): Pair<List<TestEvent>, CcDebugInfoCheck.Result> {
    return check(listOf(compiler(CC, compilerKind)), *targets)
  }

  private fun check(compilers: List<CcCompilerInfo>, vararg targets: TargetIdeInfo): Pair<List<TestEvent>, CcDebugInfoCheck.Result> {
    // the check does not use the project, thus the default project is sufficient
    val ctx = TestDebugContext(ProjectManager.getInstance().defaultProject, MAIN)
    val result = CcDebugInfoCheck(ctx, compilers).check(targets.asList())
    return ctx.events to result
  }
}

private fun compileAction(vararg arguments: String, executable: String = CC): CcDebugInfo.Action {
  return action("CppCompile", executable, arguments.asList())
}

private fun linkAction(vararg arguments: String, executable: String = CC): CcDebugInfo.Action {
  return action("CppLink", executable, arguments.asList())
}

private fun action(mnemonic: String, executable: String, arguments: List<String>): CcDebugInfo.Action {
  return CcDebugInfo.Action.newBuilder()
    .setMnemonic(mnemonic)
    .addArguments(executable)
    .addAllArguments(arguments)
    .build()
}

private fun compiler(path: String, kind: OCCompilerKind): CcCompilerInfo {
  return CcCompilerInfo(
    cCompiler = Path.of(path),
    cCompilerKind = kind,
    cppCompiler = Path.of(path),
    cppCompilerKind = kind,
    cSwitches = emptyList(),
    cppSwitches = emptyList(),
    name = "compiler",
    environment = emptyMap(),
    builtinIncludes = OutputLocationCollection.EMPTY,
    sysroot = null,
  )
}

private fun target(
  label: Label,
  compile: List<CcDebugInfo.Action> = emptyList(),
  link: List<CcDebugInfo.Action> = emptyList(),
  mode: CompilationMode = CompilationMode.FASTBUILD,
  os: PlatformInfo.OS = PlatformInfo.OS.LINUX,
): TargetIdeInfo {
  return TargetIdeInfo.newBuilder()
    .setKey(TargetKey.newBuilder().setLabel(label.toString()))
    .setPlatformInfo(PlatformInfo.newBuilder().setOs(os))
    .setCcDebugInfo(CcDebugInfo.newBuilder().addAllCompileActions(compile).addAllLinkActions(link).setCompilationMode(mode))
    .build()
}
