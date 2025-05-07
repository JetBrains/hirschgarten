package org.jetbrains.bazel.cpp.sync

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.registry.Registry
import com.intellij.testFramework.registerServiceInstance
import com.jetbrains.cidr.lang.CLanguageKind
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.string.shouldMatch
import org.jetbrains.bazel.bazelrunner.utils.BazelInfo
import org.jetbrains.bazel.bazelrunner.utils.BazelRelease
import org.jetbrains.bazel.cpp.sync.compiler.CompilerVersionChecker
import org.jetbrains.bazel.cpp.sync.compiler.MockCompilerVersionChecker
import org.jetbrains.bazel.cpp.sync.flag.BazelCompilerFlagsProcessorProvider
import org.jetbrains.bazel.cpp.sync.flag.IncludeRootFlagsProcessorProvider
import org.jetbrains.bazel.cpp.sync.flag.SysrootFlagProcessorProvider
import org.jetbrains.bazel.sync.task.bazelProject
import org.jetbrains.bazel.workspace.model.test.framework.MockProjectBaseTest
import org.jetbrains.bsp.protocol.BazelProject
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Path
import kotlin.collections.listOf
import kotlin.io.path.Path

class BazelCompilerSettingTest : MockProjectBaseTest() {
  private val mockBazelInfo =
    BazelInfo(
      execRoot = Path("execRoot").toAbsolutePath(),
      outputBase = Path("outputBase").toAbsolutePath(),
      workspaceRoot = Path("workspaceRoot").toAbsolutePath(),
      bazelBin = Path("bazel-bin").toAbsolutePath(),
      release = BazelRelease(7),
      isBzlModEnabled = true,
      isWorkspaceEnabled = true,
      externalAutoloads = emptyList(),
    )

  private val bazelProject = BazelProject(emptyMap(), mockBazelInfo, false)

  @BeforeEach
  fun initTest() {
    project.bazelProject = bazelProject
    ApplicationManager.getApplication().registerServiceInstance(CompilerVersionChecker::class.java, MockCompilerVersionChecker("1234"))
    BazelCompilerFlagsProcessorProvider.ep.registerExtension(IncludeRootFlagsProcessorProvider())

    BazelCompilerFlagsProcessorProvider.ep.registerExtension(SysrootFlagProcessorProvider())
    Registry.get("bazel.sync.resolve.virtual.includes").setValue(true)
  }

  @Test
  fun testCompilerSwitchesSimple() {
    val cFlags: List<String> = listOf("-fast", "-slow")
    val settings =
      BazelCompilerSettings(
        project,
        Path.of("bin/c"),
        Path.of("bin/c++"),
        cFlags,
        cFlags,
        "cc version (trunk r123456)",
        emptyMap(),
        emptyList(),
      )

    val res = settings.getCompilerSwitches(CLanguageKind.C, null)
    res shouldContainExactly listOf("-fast", "-slow")
  }

  @Test
  fun relativeSysroot_makesAbsolutePathInMainWorkspace() {
    val cFlags: List<String> = listOf("--sysroot=third_party/toolchain/")
    val settings: BazelCompilerSettings =
      BazelCompilerSettings(
        project,
        Path.of("bin/c"),
        Path.of("bin/c++"),
        cFlags,
        cFlags,
        "cc version (trunk r123456)",
        mapOf(),
        listOf(),
      )
    val workspaceRoot = project.bazelProject.bazelInfo.workspaceRoot
    val res = settings.getCompilerSwitches(CLanguageKind.C, null)
    res shouldContainExactly listOf("--sysroot=" + workspaceRoot + "/third_party/toolchain")
  }

  @Test
  fun absoluteSysroot_doesNotChange() {
    val cFlags: List<String> = listOf("--sysroot=/usr")
    val settings: BazelCompilerSettings =
      BazelCompilerSettings(
        project,
        Path.of("bin/c"),
        Path.of("bin/c++"),
        cFlags,
        cFlags,
        "cc version (trunk r123456)",
        mapOf(),
        listOf(),
      )

    val res = settings.getCompilerSwitches(CLanguageKind.C, null)
    res shouldContainExactly listOf("--sysroot=/usr")
  }

  @Test
  fun relativeIsystem_makesAbsolutePathInWorkspaces() {
    val cFlags: List<String> =
      listOf("-isystem", "external/arm_gcc/include", "-DFOO=1", "-Ithird_party/stl")
    val settings: BazelCompilerSettings =
      BazelCompilerSettings(
        project,
        Path.of("bin/c"),
        Path.of("bin/c++"),
        cFlags,
        cFlags,
        "cc version (trunk r123456)",
        mapOf(),
        listOf(),
      )
    val workspaceRoot = project.bazelProject.bazelInfo.workspaceRoot

    val outputBase: String =
      project.bazelProject.bazelInfo.outputBase
        .toString()
    val res = settings.getCompilerSwitches(CLanguageKind.C, null)
    res shouldContainExactly

      listOf(
        "-isystem",
        outputBase + "/external/arm_gcc/include",
        "-DFOO=1",
        "-I",
        workspaceRoot.resolve("third_party/stl").toString(),
      )
  }

  @Test
  fun relativeIquote_makesAbsolutePathInExecRoot() {
    val cFlags: List<String> =
      listOf("-iquote", "bazel-out/android-arm64-v8a-opt/bin/external/boringssl")
    val settings: BazelCompilerSettings =
      BazelCompilerSettings(
        project,
        Path.of("bin/c"),
        Path.of("bin/c++"),
        cFlags,
        cFlags,
        "cc version (trunk r123456)",
        mapOf(),
        listOf(),
      )

    val execRoot: String =
      project.bazelProject.bazelInfo.execRoot
        .toString()
    val res = settings.getCompilerSwitches(CLanguageKind.C, null)
    res shouldContainExactly
      listOf(
        "-iquote",
        execRoot + "/bazel-out/android-arm64-v8a-opt/bin/external/boringssl",
      )
  }

  @Test
  fun absoluteISystem_doesNotChange() {
    val cFlags: List<String> = listOf("-isystem", "/usr/include")
    val settings: BazelCompilerSettings =
      BazelCompilerSettings(
        project,
        Path.of("bin/c"),
        Path.of("bin/c++"),
        cFlags,
        cFlags,
        "cc version (trunk r123456)",
        mapOf(),
        listOf(),
      )

    val res = settings.getCompilerSwitches(CLanguageKind.C, null)
    res shouldContainExactly listOf("-isystem", "/usr/include")
  }

  @Test
  fun developerDirEnvVar_doesNotChange() {
    val settings: BazelCompilerSettings =
      BazelCompilerSettings(
        project,
        Path.of("bin/c"),
        Path.of("bin/c++"),
        listOf(),
        listOf(),
        "cc version (trunk r123456)",
        mapOf("DEVELOPER_DIR" to "/tmp/foobar"),
        listOf(),
      )

    settings.getCompilerEnvironment("DEVELOPER_DIR") shouldMatch "/tmp/foobar"
  }
}
