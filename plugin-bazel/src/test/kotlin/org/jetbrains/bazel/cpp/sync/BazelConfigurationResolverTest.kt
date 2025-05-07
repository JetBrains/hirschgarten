package org.jetbrains.bazel.cpp.sync

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.registry.Registry
import com.intellij.testFramework.registerServiceInstance
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.equals.shouldBeEqual
import org.jetbrains.bazel.bazelrunner.utils.BazelInfo
import org.jetbrains.bazel.bazelrunner.utils.BazelRelease
import org.jetbrains.bazel.commons.TargetKey
import org.jetbrains.bazel.cpp.sync.compiler.CompilerVersionChecker
import org.jetbrains.bazel.cpp.sync.compiler.CompilerWrapperProvider
import org.jetbrains.bazel.cpp.sync.compiler.CompilerWrapperProviderImpl
import org.jetbrains.bazel.cpp.sync.compiler.MockCompilerVersionChecker
import org.jetbrains.bazel.cpp.sync.configuration.BazelConfigurationResolver
import org.jetbrains.bazel.cpp.sync.configuration.BazelConfigurationResolverResult
import org.jetbrains.bazel.cpp.sync.flag.BazelCompilerFlagsProcessorProvider
import org.jetbrains.bazel.cpp.sync.flag.IncludeRootFlagsProcessorProvider
import org.jetbrains.bazel.cpp.sync.flag.SysrootFlagProcessorProvider
import org.jetbrains.bazel.info.BspTargetInfo
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync.task.bazelProject
import org.jetbrains.bazel.workspace.model.test.framework.MockProjectBaseTest
import org.jetbrains.bazel.workspacecontext.AllowManualTargetsSyncSpec
import org.jetbrains.bazel.workspacecontext.AndroidMinSdkSpec
import org.jetbrains.bazel.workspacecontext.BazelBinarySpec
import org.jetbrains.bazel.workspacecontext.BuildFlagsSpec
import org.jetbrains.bazel.workspacecontext.DirectoriesSpec
import org.jetbrains.bazel.workspacecontext.DotBazelBspDirPathSpec
import org.jetbrains.bazel.workspacecontext.EnableNativeAndroidRules
import org.jetbrains.bazel.workspacecontext.EnabledRulesSpec
import org.jetbrains.bazel.workspacecontext.ExperimentalAddTransitiveCompileTimeJars
import org.jetbrains.bazel.workspacecontext.IdeJavaHomeOverrideSpec
import org.jetbrains.bazel.workspacecontext.ImportDepthSpec
import org.jetbrains.bazel.workspacecontext.ImportRunConfigurationsSpec
import org.jetbrains.bazel.workspacecontext.NoPruneTransitiveCompileTimeJarsPatternsSpec
import org.jetbrains.bazel.workspacecontext.PrioritizeLibrariesOverModulesTargetKindsSpec
import org.jetbrains.bazel.workspacecontext.ShardSyncSpec
import org.jetbrains.bazel.workspacecontext.ShardingApproachSpec
import org.jetbrains.bazel.workspacecontext.SyncFlagsSpec
import org.jetbrains.bazel.workspacecontext.TargetShardSizeSpec
import org.jetbrains.bazel.workspacecontext.TargetsSpec
import org.jetbrains.bazel.workspacecontext.TransitiveCompileTimeJarsTargetKindsSpec
import org.jetbrains.bazel.workspacecontext.WorkspaceContext
import org.jetbrains.bsp.protocol.BazelProject
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Path
import kotlin.io.path.Path

class BazelConfigurationResolverTest : MockProjectBaseTest() {
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

  @BeforeEach
  fun initTest() {
    ApplicationManager.getApplication().registerServiceInstance(CompilerVersionChecker::class.java, MockCompilerVersionChecker("1234"))
    BazelCompilerFlagsProcessorProvider.ep.registerExtension(IncludeRootFlagsProcessorProvider())
    BazelCompilerFlagsProcessorProvider.ep.registerExtension(SysrootFlagProcessorProvider())
    ApplicationManager.getApplication().registerServiceInstance(CompilerWrapperProvider::class.java, CompilerWrapperProviderImpl())

    Registry.get("bazel.sync.resolve.virtual.includes").setValue(true)
  }

  @Test
  fun testTargetWithoutSources() {
    val workspaceContext =
      createWorkspaceContext(
        listOf(Label.parse("//foo/bar:library")),
        listOf(Path.of("foo/bar")),
      )
    val toolChainDep = createDependency("//:toolchain")
    val targetMap =
      mapOf(
        Label.parse("//foo/bar:library") to
          BspTargetInfo.TargetInfo
            .newBuilder()
            .setId("//foo/bar:library")
            .setKind("cc_library")
            .addDependencies(toolChainDep)
            .setCppTargetInfo(BspTargetInfo.CppTargetInfo.newBuilder().build())
            .build(),
        Label.parse("//:toolchain") to createNewToolchain(),
      )
    val bazelProject = BazelProject(targetMap, mockBazelInfo, false)
    project.bazelProject = bazelProject

    val resolver = BazelConfigurationResolver(project, workspaceContext)
    val res = resolver.update(BazelConfigurationResolverResult.empty())
    res.allConfigurations.size shouldBeEqual 0
  }

  @Test
  fun testTargetWithMixedSources() {
    val workspaceContext =
      createWorkspaceContext(
        listOf(Label.parse("//foo/bar:library")),
        listOf(Path.of("foo/bar")),
      )
    val toolChainDep = createDependency("//:toolchain")
    val targetMap =
      mapOf(
        Label.parse("//foo/bar:library") to
          BspTargetInfo.TargetInfo
            .newBuilder()
            .setId("//foo/bar:library")
            .setKind("cc_library")
            .addDependencies(toolChainDep)
            .addSources(createGeneratedSource("foo/bar/generated.cc"))
            .addSources(createSource("foo/bar/library.cc"))
            .setCppTargetInfo(
              BspTargetInfo.CppTargetInfo.newBuilder().build(),
            ).build(),
        Label.parse("//:toolchain") to createNewToolchain(),
      )
    val bazelProject = BazelProject(targetMap, mockBazelInfo, false)
    project.bazelProject = bazelProject

    val resolver = BazelConfigurationResolver(project, workspaceContext)
    val res = resolver.update(BazelConfigurationResolverResult.empty())
    res.allConfigurations.size shouldBeEqual 1
    res.allConfigurations.map { it.displayName } shouldContainAll listOf("//foo/bar:library".toDisplayName())
  }

  @Test
  fun testTargetWithGeneratedSources() {
    val workspaceContext =
      createWorkspaceContext(
        listOf(Label.parse("//foo/bar:library")),
        listOf(Path.of("foo/bar")),
      )
    val toolChainDep = createDependency("//:toolchain")
    val targetMap =
      mapOf(
        Label.parse("//foo/bar:library") to
          BspTargetInfo.TargetInfo
            .newBuilder()
            .setId("//foo/bar:library")
            .setKind("cc_library")
            .addDependencies(toolChainDep)
            .addSources(createGeneratedSource("foo/bar/library.cc"))
            .setCppTargetInfo(BspTargetInfo.CppTargetInfo.newBuilder().build())
            .build(),
        Label.parse("//:toolchain") to createNewToolchain(),
      )
    val bazelProject =
      BazelProject(
        targetMap,
        mockBazelInfo,
        false,
      )
    project.bazelProject = bazelProject

    val resolver = BazelConfigurationResolver(project, workspaceContext)
    val res = resolver.update(BazelConfigurationResolverResult.empty())
    res.allConfigurations.size shouldBeEqual 0
  }

  @Test
  fun testSingleSourceTarget() {
    val workspaceContext =
      createWorkspaceContext(
        listOf(Label.parse("//foo/bar:binary")),
        listOf(Path.of("foo/bar")),
      )
    val toolChainDep = createDependency("//:toolchain")
    val targetMap =
      mapOf(
        Label.parse("//foo/bar:binary") to
          BspTargetInfo.TargetInfo
            .newBuilder()
            .setId("//foo/bar:binary")
            .setKind("cc_binary")
            .addDependencies(toolChainDep)
            .addSources(createSource("foo/bar/binary.cc"))
            .setCppTargetInfo(BspTargetInfo.CppTargetInfo.newBuilder().build())
            .build(),
        Label.parse("//:toolchain") to createNewToolchain(),
      )
    val bazelProject = BazelProject(targetMap, mockBazelInfo, false)
    project.bazelProject = bazelProject

    val resolver = BazelConfigurationResolver(project, workspaceContext)
    val res = resolver.update(BazelConfigurationResolverResult.empty())
    res.allConfigurations.size shouldBeEqual 1
    res.allConfigurations.map { it.displayName } shouldContainAll listOf("//foo/bar:binary".toDisplayName())
  }

  @Test
  fun testSingleSourceTargetWithSourceDependencies() {
    val workspaceContext =
      createWorkspaceContext(
        listOf(Label.parse("//foo/bar:binary")),
        listOf(Path.of("foo/bar")),
      )
    val toolChainDep = createDependency("//:toolchain")
    val targetMap =
      mapOf(
        Label.parse("//foo/bar:binary") to
          BspTargetInfo.TargetInfo
            .newBuilder()
            .setId("//foo/bar:binary")
            .setKind("cc_binary")
            .addDependencies(toolChainDep)
            .addDependencies(createDependency("//:toolchain"))
            .addDependencies(createDependency("//foo/bar:library"))
            .addDependencies(createDependency("//third_party:library"))
            .addSources(createSource("foo/bar/binary.cc"))
            .setCppTargetInfo(BspTargetInfo.CppTargetInfo.newBuilder().build())
            .build(),
        Label.parse("//foo/bar:library") to
          BspTargetInfo.TargetInfo
            .newBuilder()
            .setId("//foo/bar:library")
            .setKind("cc_library")
            .addDependencies(toolChainDep)
            .addSources(createSource("foo/bar/library.cc"))
            .setCppTargetInfo(
              BspTargetInfo.CppTargetInfo
                .newBuilder()
                .addCopts("-DSOME_DEFINE=1")
                .build(),
            ).build(),
        Label.parse("//third_party:library") to
          BspTargetInfo.TargetInfo
            .newBuilder()
            .setId("//third_party:library")
            .setKind("cc_library")
            .addDependencies(toolChainDep)
            .addSources(createSource("third_party/library.cc"))
            .setCppTargetInfo(BspTargetInfo.CppTargetInfo.newBuilder().build())
            .build(),
        Label.parse("//:toolchain") to createNewToolchain(),
      )
    val bazelProject = BazelProject(targetMap, mockBazelInfo, false)
    project.bazelProject = bazelProject

    val resolver = BazelConfigurationResolver(project, workspaceContext)
    val res = resolver.update(BazelConfigurationResolverResult.empty())
    res.allConfigurations.size shouldBeEqual 2
    res.allConfigurations.map { it.displayName } shouldContainAll
      listOf(
        "//foo/bar:binary".toDisplayName(),
        "//foo/bar:library".toDisplayName(),
      )
  }

  private fun createDependency(name: String) =
    BspTargetInfo.Dependency
      .newBuilder()
      .setId(name)
      .build()

  private fun createNewToolchain(): BspTargetInfo.TargetInfo =
    BspTargetInfo.TargetInfo
      .newBuilder()
      .setId("//:toolchain")
      .setKind("cc_toolchain")
      .setCToolchainInfo(
        BspTargetInfo.CToolchainInfo
          .newBuilder()
          .setTargetName("toolchain")
          .setCCompiler("cc")
          .build(),
      ).build()

  private fun createSource(name: String): BspTargetInfo.FileLocation =
    BspTargetInfo.FileLocation
      .newBuilder()
      .setRelativePath(name)
      .setIsSource(true)
      .build()

  private fun createGeneratedSource(name: String): BspTargetInfo.FileLocation =
    BspTargetInfo.FileLocation
      .newBuilder()
      .setRelativePath(name)
      .setIsSource(false)
      .build()

  private fun createWorkspaceContext(targets: List<Label>, directories: List<Path>): WorkspaceContext =
    WorkspaceContext(
      targets = TargetsSpec(targets, listOf()),
      directories = DirectoriesSpec(directories, listOf()),
      buildFlags = BuildFlagsSpec(listOf("flag1", "flag2")),
      syncFlags = SyncFlagsSpec(listOf("flag1", "flag2")),
      bazelBinary = BazelBinarySpec(Path("bazel")),
      allowManualTargetsSync = AllowManualTargetsSyncSpec(true),
      dotBazelBspDirPath = DotBazelBspDirPathSpec(Path(".bazelbsp")),
      importDepth = ImportDepthSpec(2),
      enabledRules = EnabledRulesSpec(listOf("rule1", "rule2")),
      ideJavaHomeOverrideSpec = IdeJavaHomeOverrideSpec(Path("java_home")),
      experimentalAddTransitiveCompileTimeJars = ExperimentalAddTransitiveCompileTimeJars(true),
      experimentalTransitiveCompileTimeJarsTargetKinds = TransitiveCompileTimeJarsTargetKindsSpec(emptyList()),
      experimentalNoPruneTransitiveCompileTimeJarsPatterns = NoPruneTransitiveCompileTimeJarsPatternsSpec(emptyList()),
      experimentalPrioritizeLibrariesOverModulesTargetKinds = PrioritizeLibrariesOverModulesTargetKindsSpec(emptyList()),
      enableNativeAndroidRules = EnableNativeAndroidRules(false),
      androidMinSdkSpec = AndroidMinSdkSpec(null),
      shardSync = ShardSyncSpec(false),
      targetShardSize = TargetShardSizeSpec(1000),
      shardingApproachSpec = ShardingApproachSpec(null),
      importRunConfigurations = ImportRunConfigurationsSpec(emptyList()),
    )

  private fun String.toDisplayName() = TargetKey(Label.parse(this), listOf()).toString()
}
