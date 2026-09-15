package org.jetbrains.bazel.clion

import com.intellij.testFramework.junit5.fixture.projectFixture
import com.jetbrains.cidr.lang.workspace.compiler.AppleClangCompilerKind
import com.jetbrains.cidr.lang.workspace.compiler.ClangCompilerKind
import com.jetbrains.cidr.lang.workspace.compiler.CompilerSpecificSwitchBuilder
import com.jetbrains.cidr.lang.workspace.compiler.GCCCompilerKind
import com.jetbrains.cidr.lang.workspace.compiler.MSVCCompilerKind
import com.jetbrains.cidr.lang.workspace.compiler.OCCompilerKind
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.bazel.clion.workspace.CcCompilerKind
import org.jetbrains.bazel.clion.workspace.copts.applyCopts
import org.jetbrains.bazel.fixtures.CcTestApplication
import org.jetbrains.bazel.fixtures.withTestImportContext
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSnapshot
import org.junit.jupiter.api.Test
import java.nio.file.Path

private val EXECROOT: Path = Path.of("/execroot")

/** Every compiler that takes the GNU spelling, with the two wrapper kinds of the plugin. */
private val GNU = listOf(
  GCCCompilerKind,
  CcCompilerKind.GCC,
  ClangCompilerKind,
  AppleClangCompilerKind,
  CcCompilerKind.CLANG,
)

@CcTestApplication
class CcCoptsTest {

  private val project by projectFixture()

  @Test
  fun testDropsBlankOptions() = doTest(
    GNU,
    listOf("", "-Wall", "", "", "-Wextra", "-Werror", ""),
    expected = listOf("-Wall", "-Wextra", "-Werror"),
  )

  @Test
  fun testGnuDropsABlankValueWithItsFlag() = doTest(
    GNU,
    listOf("-Wall", "-I", "", "-DFOO"),
    expected = listOf("-Wall", "-DFOO"),
  )

  @Test
  fun testGnuDropsADanglingFlag() = doTest(
    GNU,
    listOf("-Wall", "-I"),
    expected = listOf("-Wall"),
  )

  @Test
  fun testGnuExpandsIncludes() = doTest(
    GNU,
    listOf("-I/absolute/path", "-Iinclude/default", "-isysteminclude/system", "-iquoteinclude/quote"),
    expected = listOf(
      "-I" + rooted("/absolute/path"),
      "-I" + rooted("include/default"),
      "-isystem" + rooted("include/system"),
      "-iquote" + rooted("include/quote"),
    ),
  )

  @Test
  fun testGnuExpandsSplitFlags() = doTest(
    GNU,
    listOf(
      "-I", "/absolute/path",
      "-I", "include/default",
      "-isystem", "include/system",
      "-iquote", "include/quote",
    ),
    expected = listOf(
      "-I" + rooted("/absolute/path"),
      "-I" + rooted("include/default"),
      "-isystem" + rooted("include/system"),
      "-iquote" + rooted("include/quote"),
    ),
  )

  @Test
  fun testGnuKeepsSysrootMarker() = doTest(
    GNU,
    listOf(
      "-I=/sysroot/default",
      "-isystem=/sysroot/system",
      "-iquote=/sysroot/quote",
      "-I", "=/sysroot/split",
    ),
    expected = listOf(
      "-I=/sysroot/default",
      "-isystem=/sysroot/system",
      "-iquote=/sysroot/quote",
      "-I=/sysroot/split",
    ),
  )

  @Test
  fun testGnuExpandsSysroot() = doTest(
    GNU - AppleClangCompilerKind,
    listOf("--sysroot=/absolute/path", "--sysroot=path/to/sysroot"),
    expected = listOf(
      "--sysroot=" + rooted("/absolute/path"),
      "--sysroot=" + rooted("path/to/sysroot"),
    ),
  )

  @Test
  fun testAppleClangExpandsSysroot() = doTest(
    listOf(AppleClangCompilerKind),
    listOf("--sysroot=/absolute/path", "--sysroot=path/to/sysroot"),
    expected = listOf(
      "-isysroot", rooted("/absolute/path"),
      "-isysroot", rooted("path/to/sysroot"),
    ),
  )

  @Test
  fun testKeepsDefinesWithSpaces() {
    val defines = listOf(
      """-DSPACE_DEFINE="1 2 3"""",
      "-DSIMPLE_DEFINE=42",
      """-DQUOTED_PATH="path/with spaces/file.h"""",
      """-DPARENS_DEFINE="foo(bar)"""",
    )

    doTest(GNU, defines, expected = defines)
  }

  @Test
  fun testGnuKeepsWarningFlags() = doTest(
    GNU,
    listOf("-Wall", "-Wpedantic", "-Wlogical-op", "-Wpointer-arith", "-Walloca"),
    expected = listOf("-Wall", "-Wpedantic", "-Wlogical-op", "-Wpointer-arith", "-Walloca"),
  )

  @Test
  fun testGnuExpandsXclangInclude() = doTest(
    GNU,
    listOf("-Xclang", "-isystem", "-Xclang", "external/sdk/include"),
    expected = listOf("-Xclang", "-isystem" + rooted("external/sdk/include")),
  )

  @Test
  fun testGnuAccumulatesForwardedOption() = doTest(
    GNU,
    listOf("-Wall", "-Wp,-Werror", "-Xclang", "-fmodule-map-file=module.modulemap", "-Werror"),
    expected = listOf("-Wall", "-Werror", "-Wp,-Werror", "-Xclang", "-fmodule-map-file=module.modulemap"),
  )

  @Test
  fun testGnuExpandsWpInclude() = doTest(
    GNU,
    listOf("-Wp,-I,include/foo", "-Wall"),
    expected = listOf("-Wall", "-Wp,-I" + rooted("include/foo")),
  )

  @Test
  fun testGnuDropsDanglingForwarder() = doTest(
    GNU,
    listOf("-Wall", "-Xclang"),
    expected = listOf("-Wall"),
  )

  @Test
  fun testGnuExpandsXarchInclude() = doTest(
    GNU,
    listOf("-Xarch_x86_64", "-isystem", "-Xarch_x86_64", "external/sdk/include", "-Xarch_host", "-Ihost/inc"),
    expected = listOf(
      "-Xarch_x86_64", "-isystem" + rooted("external/sdk/include"),
      "-Xarch_host", "-I" + rooted("host/inc"),
    ),
  )

  @Test
  fun testGnuExpandsOpenmpTargetInclude() = doTest(
    GNU,
    listOf("-Xopenmp-target", "-I", "-Xopenmp-target", "gen/inc", "-Xopenmp-target=nvptx64", "-isystemsdk/inc"),
    expected = listOf(
      "-Xopenmp-target", "-I" + rooted("gen/inc"),
      "-Xopenmp-target=nvptx64", "-isystem" + rooted("sdk/inc"),
    ),
  )

  @Test
  fun testGnuExpandsXclangasInclude() = doTest(
    GNU,
    listOf("-Xclangas", "-I", "-Xclangas", "asm/inc"),
    expected = listOf("-Xclangas", "-I" + rooted("asm/inc")),
  )

  @Test
  fun testKeepsOptionsOfAnUnknownCompiler() {
    val options = listOf("-Iinclude/default", "--sysroot=path/to/sysroot")

    doTest(listOf(MSVCCompilerKind), options, expected = options)
  }

  private fun doTest(compilers: List<OCCompilerKind>, copts: List<String>, expected: List<String>) {
    for (compiler in compilers) {
      val (ctx, switches) = withTestImportContext(WorkspaceSnapshot.EMPTY, project, EXECROOT) {
        CompilerSpecificSwitchBuilder.getBuilder(compiler).apply { runBlocking { applyCopts(compiler, copts) } }.buildRaw()
      }

      assertThat(ctx.events).isEmpty()
      assertThat(switches).describedAs("the switches of %s", compiler).containsExactlyElementsOf(expected)
    }
  }
}

private fun rooted(path: String): String = EXECROOT.resolve(path).toString()
