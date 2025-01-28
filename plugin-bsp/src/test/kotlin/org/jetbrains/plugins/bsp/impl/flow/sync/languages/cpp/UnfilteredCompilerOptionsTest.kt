package org.jetbrains.plugins.bsp.impl.flow.sync.languages.cpp

import com.google.common.collect.ImmutableList
import io.kotest.matchers.collections.shouldContainExactly
import org.junit.jupiter.api.Test

class UnfilteredCompilerOptionsTest {
  @Test
  fun testUnfilteredOptionsParsingForISystemOptions() {
    val unfilteredOptions =
      listOf(
        "-isystem",
        "sys/inc1",
        "-VER2",
        "-isystem",
        "sys2/inc1",
        "-isystem",
        "sys3/inc1",
        "-isystm",
        "sys4/inc1",
      )
    val compilerOptions: UnfilteredCompilerOptions =
      UnfilteredCompilerOptions.Builder()
        .registerSingleOrSplitOption("-isystem")
        .build(unfilteredOptions)

    val sysIncludes = compilerOptions.getExtractedOptionValues("-isystem")
    val flags = compilerOptions.getUninterpretedOptions()
    sysIncludes shouldContainExactly listOf("sys/inc1", "sys2/inc1", "sys3/inc1")
    flags shouldContainExactly listOf("-VER2", "-isystm", "sys4/inc1")

  }

  @Test
  fun testUnfilteredOptionsParsingForISystemOptionsNoSpaceAfterIsystem() {
    val unfilteredOptions =
      listOf(
        "-isystem", "sys/inc1", "-VER2", "-isystemsys2/inc1", "-isystem", "sys3/inc1",
      )
    val compilerOptions: UnfilteredCompilerOptions =
      UnfilteredCompilerOptions.Builder()
        .registerSingleOrSplitOption("-isystem")
        .build(unfilteredOptions)

    val sysIncludes = compilerOptions.getExtractedOptionValues("-isystem")
    val flags = compilerOptions.getUninterpretedOptions()
    sysIncludes shouldContainExactly listOf("sys/inc1", "sys2/inc1", "sys3/inc1")

    flags shouldContainExactly listOf("-VER2")
  }

  @Test
  fun testMultipleFlagsToExtract() {
    val unfilteredOptions =
      listOf(
        "-I",
        "foo/headers1",
        "-fno-exceptions",
        "-Werror",
        "-DMACRO1=1",
        "-D",
        "MACRO2",
        "-Ifoo/headers2",
        "-I=sysroot_header",
        "-Wall",
        "-I",
        "foo/headers3",
      )
    val compilerOptions: UnfilteredCompilerOptions =
      UnfilteredCompilerOptions.Builder()
        .registerSingleOrSplitOption("-I")
        .registerSingleOrSplitOption("-D")
        .build(unfilteredOptions)

    val defines = compilerOptions.getExtractedOptionValues("-D")
    val includes = compilerOptions.getExtractedOptionValues("-I")
    val flags = compilerOptions.getUninterpretedOptions()
    includes shouldContainExactly listOf("foo/headers1", "foo/headers2", "=sysroot_header", "foo/headers3")

    defines shouldContainExactly listOf("MACRO1=1", "MACRO2")
    flags shouldContainExactly listOf("-fno-exceptions", "-Werror", "-Wall")
  }

  @Test
  fun testFlagAndValueInSameString() {
    // This is a functionality considered by the parser, so it should be tested.
    val unfilteredOptions =
      listOf(
        "-I foo/headers1",
        "-isystem foo/headers2",
      )
    val compilerOptions: UnfilteredCompilerOptions =
      UnfilteredCompilerOptions.Builder()
        .registerSingleOrSplitOption("-I")
        .registerSingleOrSplitOption("-isystem")
        .build(unfilteredOptions)

    val bigIIncludes = compilerOptions.getExtractedOptionValues("-I")
    bigIIncludes shouldContainExactly listOf("foo/headers1")

    val systemIncludes = compilerOptions.getExtractedOptionValues("-isystem")
    systemIncludes shouldContainExactly listOf("foo/headers2")
  }
}
