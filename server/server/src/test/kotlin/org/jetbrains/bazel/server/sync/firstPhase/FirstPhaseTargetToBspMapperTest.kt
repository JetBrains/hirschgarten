package org.jetbrains.bazel.server.sync.firstPhase

import com.google.devtools.build.lib.query2.proto.proto2api.Build
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import org.jetbrains.bazel.bazelrunner.utils.BazelRelease
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.server.bzlmod.RepoMappingDisabled
import org.jetbrains.bazel.server.model.FirstPhaseProject
import org.jetbrains.bazel.workspacecontext.AllowManualTargetsSyncSpec
import org.jetbrains.bazel.workspacecontext.AndroidMinSdkSpec
import org.jetbrains.bazel.workspacecontext.BazelBinarySpec
import org.jetbrains.bazel.workspacecontext.BuildFlagsSpec
import org.jetbrains.bazel.workspacecontext.DEFAULT_TARGET_SHARD_SIZE
import org.jetbrains.bazel.workspacecontext.DirectoriesSpec
import org.jetbrains.bazel.workspacecontext.DotBazelBspDirPathSpec
import org.jetbrains.bazel.workspacecontext.EnableNativeAndroidRules
import org.jetbrains.bazel.workspacecontext.EnabledRulesSpec
import org.jetbrains.bazel.workspacecontext.ExperimentalAddTransitiveCompileTimeJars
import org.jetbrains.bazel.workspacecontext.IdeJavaHomeOverrideSpec
import org.jetbrains.bazel.workspacecontext.ImportDepthSpec
import org.jetbrains.bazel.workspacecontext.NoPruneTransitiveCompileTimeJarsPatternsSpec
import org.jetbrains.bazel.workspacecontext.ShardSyncSpec
import org.jetbrains.bazel.workspacecontext.ShardingApproachSpec
import org.jetbrains.bazel.workspacecontext.SyncFlagsSpec
import org.jetbrains.bazel.workspacecontext.TargetShardSizeSpec
import org.jetbrains.bazel.workspacecontext.TargetsSpec
import org.jetbrains.bazel.workspacecontext.TransitiveCompileTimeJarsTargetKindsSpec
import org.jetbrains.bazel.workspacecontext.WorkspaceContext
import org.jetbrains.bazel.workspacecontext.WorkspaceContextProvider
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.BuildTargetCapabilities
import org.jetbrains.bsp.protocol.FeatureFlags
import org.jetbrains.bsp.protocol.ResourcesItem
import org.jetbrains.bsp.protocol.ResourcesParams
import org.jetbrains.bsp.protocol.SourceItem
import org.jetbrains.bsp.protocol.SourceItemKind
import org.jetbrains.bsp.protocol.SourcesItem
import org.jetbrains.bsp.protocol.SourcesParams
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.net.URI
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.createFile
import kotlin.io.path.createParentDirectories
import kotlin.io.path.createTempDirectory
import kotlin.io.path.writeText

private class MockWorkspaceContextProvider(private val allowManualTargetsSync: Boolean) : WorkspaceContextProvider {
  override fun currentWorkspaceContext(): WorkspaceContext =
    WorkspaceContext(
      targets = TargetsSpec(listOf(Label.parse("//...")), emptyList()),
      directories = DirectoriesSpec(listOf(Path(".")), emptyList()),
      buildFlags = BuildFlagsSpec(emptyList()),
      syncFlags = SyncFlagsSpec(emptyList()),
      bazelBinary = BazelBinarySpec(Path("bazel")),
      allowManualTargetsSync = AllowManualTargetsSyncSpec(allowManualTargetsSync),
      dotBazelBspDirPath = DotBazelBspDirPathSpec(Path(".bazelbsp")),
      importDepth = ImportDepthSpec(-1),
      enabledRules = EnabledRulesSpec(emptyList()),
      ideJavaHomeOverrideSpec = IdeJavaHomeOverrideSpec(Path("java_home")),
      experimentalAddTransitiveCompileTimeJars = ExperimentalAddTransitiveCompileTimeJars(false),
      experimentalTransitiveCompileTimeJarsTargetKinds = TransitiveCompileTimeJarsTargetKindsSpec(emptyList()),
      experimentalNoPruneTransitiveCompileTimeJarsPatterns = NoPruneTransitiveCompileTimeJarsPatternsSpec(emptyList()),
      enableNativeAndroidRules = EnableNativeAndroidRules(false),
      androidMinSdkSpec = AndroidMinSdkSpec(null),
      shardSync = ShardSyncSpec(false),
      targetShardSize = TargetShardSizeSpec(DEFAULT_TARGET_SHARD_SIZE),
      shardingApproachSpec = ShardingApproachSpec(null),
    )

  override fun currentFeatureFlags(): FeatureFlags = FeatureFlags()
}

private fun createMockProject(lightweightModules: List<Build.Target>): FirstPhaseProject =
  FirstPhaseProject(
    workspaceRoot = URI.create("file:///path/to/workspace"),
    bazelRelease = BazelRelease(7),
    modules = lightweightModules.associateBy { Label.parse(it.rule.name) },
    repoMapping = RepoMappingDisabled,
  )

class FirstPhaseTargetToBspMapperTest {
  private lateinit var workspaceRoot: Path

  @BeforeEach
  fun beforeEach() {
    // given
    workspaceRoot = createTempDirectory("workspaceRoot").also { it.toFile().deleteOnExit() }
  }

  @Nested
  @DisplayName(".toWorkspaceBuildTargetsResult(project)")
  inner class ToWorkspaceBuildTargetsResult {
    @Test
    fun `should map targets to bsp build targets and filter out manual, no ide targets and unsupported targets`() {
      // given
      val targets =
        listOf(
          createMockTarget(
            name = "//target1",
            kind = "java_library",
            deps = listOf("//dep/target1", "//dep/target2"),
            srcs = listOf("//target1:src1.java", "//target1:a/src2.java"),
          ),
          createMockTarget(
            name = "//target2",
            kind = "java_binary",
            deps = listOf("//dep/target1", "//dep/target2"),
          ),
          createMockTarget(
            name = "//target3",
            kind = "java_test",
            deps = listOf("//dep/target1", "//dep/target2"),
          ),
          createMockTarget(
            name = "//target4",
            kind = "kt_jvm_library",
            deps = listOf("//dep/target1", "//dep/target2"),
          ),
          createMockTarget(
            name = "//target5",
            kind = "kt_jvm_binary",
            deps = listOf("//dep/target1", "//dep/target2"),
          ),
          createMockTarget(
            name = "//target6",
            kind = "kt_jvm_test",
            deps = listOf("//dep/target1", "//dep/target2"),
          ),
          createMockTarget(
            name = "//target7",
            kind = "custom_rule_with_supported_rules_library",
            srcs = listOf("//target7:src1.java", "//target7:a/src2.java"),
          ),
          createMockTarget(
            name = "//manual_target",
            kind = "java_library",
            tags = listOf("manual"),
          ),
          createMockTarget(
            name = "//no_ide_target",
            kind = "java_library",
            tags = listOf("no-ide"),
          ),
          createMockTarget(
            name = "//unsupported_target",
            kind = "unsupported_target",
          ),
        )
      val project = createMockProject(targets)

      val workspaceContextProvider = MockWorkspaceContextProvider(allowManualTargetsSync = false)
      val mapper = FirstPhaseTargetToBspMapper(workspaceContextProvider, workspaceRoot)

      // when
      val result = mapper.toWorkspaceBuildTargetsResult(project)

      // then
      result.targets shouldContainExactlyInAnyOrder
        listOf(
          BuildTarget(
            Label.parse("//target1"),
            tags = listOf("library"),
            languageIds = listOf("java"),
            dependencies = listOf(Label.parse("//dep/target1"), Label.parse("//dep/target2")),
            capabilities =
              BuildTargetCapabilities(
                canCompile = true,
                canRun = false,
                canTest = false,
                canDebug = false,
              ),
          ),
          BuildTarget(
            Label.parse("//target2"),
            tags = listOf("application"),
            languageIds = listOf("java"),
            dependencies = listOf(Label.parse("//dep/target1"), Label.parse("//dep/target2")),
            capabilities =
              BuildTargetCapabilities(
                canCompile = true,
                canRun = true,
                canTest = false,
                canDebug = false,
              ),
          ),
          BuildTarget(
            Label.parse("//target3"),
            tags = listOf("test"),
            languageIds = listOf("java"),
            dependencies = listOf(Label.parse("//dep/target1"), Label.parse("//dep/target2")),
            capabilities =
              BuildTargetCapabilities(
                canCompile = true,
                canRun = false,
                canTest = true,
                canDebug = false,
              ),
          ),
          BuildTarget(
            Label.parse("//target4"),
            tags = listOf("library"),
            languageIds = listOf("kotlin"),
            dependencies = listOf(Label.parse("//dep/target1"), Label.parse("//dep/target2")),
            capabilities =
              BuildTargetCapabilities(
                canCompile = true,
                canRun = false,
                canTest = false,
                canDebug = false,
              ),
          ),
          BuildTarget(
            Label.parse("//target5"),
            tags = listOf("application"),
            languageIds = listOf("kotlin"),
            dependencies = listOf(Label.parse("//dep/target1"), Label.parse("//dep/target2")),
            capabilities =
              BuildTargetCapabilities(
                canCompile = true,
                canRun = true,
                canTest = false,
                canDebug = false,
              ),
          ),
          BuildTarget(
            Label.parse("//target6"),
            tags = listOf("test"),
            languageIds = listOf("kotlin"),
            dependencies = listOf(Label.parse("//dep/target1"), Label.parse("//dep/target2")),
            capabilities =
              BuildTargetCapabilities(
                canCompile = true,
                canRun = false,
                canTest = true,
                canDebug = false,
              ),
          ),
          BuildTarget(
            Label.parse("//target7"),
            tags = listOf("library"),
            languageIds = listOf("java"),
            dependencies = emptyList(),
            capabilities =
              BuildTargetCapabilities(
                canCompile = true,
                canRun = false,
                canTest = false,
                canDebug = false,
              ),
          ),
        )
    }

    @Test
    fun `should map targets to bsp build targets and keep manual targets if manual targets sync is allowed`() {
      // given
      val targets =
        listOf(
          createMockTarget(
            name = "//target1",
            kind = "java_library",
          ),
          createMockTarget(
            name = "//manual_target",
            kind = "java_library",
            tags = listOf("manual"),
          ),
        )
      val project = createMockProject(targets)

      val workspaceContextProvider = MockWorkspaceContextProvider(allowManualTargetsSync = true)
      val mapper = FirstPhaseTargetToBspMapper(workspaceContextProvider, workspaceRoot)

      // when
      val result = mapper.toWorkspaceBuildTargetsResult(project)

      // then
      result.targets.map { it.id.toShortString() } shouldContainExactlyInAnyOrder
        listOf(
          "//target1",
          "//manual_target",
        )
    }
  }

  @Nested
  @DisplayName(".toSourcesResult(project, params)")
  inner class ToSourcesResult {
    @Test
    fun `should map project to source items and filter out not requested targets`() {
      // given
      val target1Src1 = workspaceRoot.createMockSourceFile("target1/src1.java", "com.example")
      val target1Src2 = workspaceRoot.createMockSourceFile("target1/a/src2.java", "com.example.a")

      val target2Src1 = workspaceRoot.createMockSourceFile("target2/src1.kt", "com.example")
      val target2Src2 = workspaceRoot.createMockSourceFile("target2/src2.kt", "com.example")

      workspaceRoot.createMockSourceFile("target3/src1.java", "com.example")
      workspaceRoot.createMockSourceFile("target3/src2.java", "com.example")

      val targets =
        listOf(
          createMockTarget(
            name = "//target1",
            kind = "java_library",
            srcs = listOf("//target1:src1.java", "//target1:a/src2.java"),
          ),
          createMockTarget(
            name = "//target2",
            kind = "java_library",
            srcs = listOf("//target2:src1.kt", "//target2:src2.kt"),
          ),
          createMockTarget(
            name = "//target3",
            kind = "java_library",
            srcs = listOf("//target3:src1.java", "//target3:src2.java"),
          ),
        )
      val project = createMockProject(targets)

      val workspaceContextProvider = MockWorkspaceContextProvider(allowManualTargetsSync = false)
      val mapper = FirstPhaseTargetToBspMapper(workspaceContextProvider, workspaceRoot)

      // when
      val params =
        SourcesParams(
          listOf(
            Label.parse("//target1"),
            Label.parse("//target2"),
          ),
        )
      val result = mapper.toSourcesResult(project, params)

      // then
      result.items shouldContainExactlyInAnyOrder
        listOf(
          SourcesItem(
            Label.parse("//target1"),
            listOf(
              SourceItem(target1Src1.toUri().toString(), SourceItemKind.FILE, false, jvmPackagePrefix = "com.example"),
              SourceItem(target1Src2.toUri().toString(), SourceItemKind.FILE, false, jvmPackagePrefix = "com.example.a"),
            ),
          ),
          SourcesItem(
            Label.parse("//target2"),
            listOf(
              SourceItem(target2Src1.toUri().toString(), SourceItemKind.FILE, false, jvmPackagePrefix = "com.example"),
              SourceItem(target2Src2.toUri().toString(), SourceItemKind.FILE, false, jvmPackagePrefix = "com.example"),
            ),
          ),
        )
      result.items
        .flatMap { it.sources }
        .map { it.jvmPackagePrefix } shouldContainExactly
        listOf(
          "com.example",
          "com.example.a",
          "com.example",
          "com.example",
        )
    }

    @Test
    fun `should map project to source items including sources referenced via filegroup targets`() {
      // given
      val filegroupSrc1 = workspaceRoot.createMockSourceFile("filegroup/src1.java", "com.example")
      val filegroupSrc2 = workspaceRoot.createMockSourceFile("filegroup/a/src2.java", "com.example.a")

      val target1Root = workspaceRoot.resolve("target1")
      val target1Src1 = workspaceRoot.createMockSourceFile("target1/src1.kt", "com.example")
      val target1Src2 = workspaceRoot.createMockSourceFile("target1/src2.kt", "com.example")

      val targets =
        listOf(
          createMockTarget(
            name = "//filegroup",
            kind = "filegroup",
            srcs = listOf("//filegroup:src1.java", "//filegroup:a/src2.java"),
          ),
          createMockTarget(
            name = "//target1",
            kind = "java_library",
            srcs = listOf("//target1:src1.kt", "//target1:src2.kt", "//filegroup"),
          ),
        )
      val project = createMockProject(targets)

      val workspaceContextProvider = MockWorkspaceContextProvider(allowManualTargetsSync = false)
      val mapper = FirstPhaseTargetToBspMapper(workspaceContextProvider, workspaceRoot)

      // when
      val params =
        SourcesParams(
          listOf(
            Label.parse("//target1"),
          ),
        )
      val result = mapper.toSourcesResult(project, params)

      // then
      result.items shouldContainExactlyInAnyOrder
        listOf(
          SourcesItem(
            Label.parse("//target1"),
            listOf(
              SourceItem(target1Src1.toUri().toString(), SourceItemKind.FILE, false, jvmPackagePrefix = "com.example"),
              SourceItem(target1Src2.toUri().toString(), SourceItemKind.FILE, false, jvmPackagePrefix = "com.example"),
              SourceItem(filegroupSrc1.toUri().toString(), SourceItemKind.FILE, false, jvmPackagePrefix = "com.example"),
              SourceItem(filegroupSrc2.toUri().toString(), SourceItemKind.FILE, false, jvmPackagePrefix = "com.example.a"),
            ),
          ),
        )
      result.items
        .flatMap { it.sources }
        .map { it.jvmPackagePrefix } shouldContainExactly
        listOf(
          "com.example",
          "com.example",
          "com.example",
          "com.example.a",
        )
    }
  }

  @Nested
  @DisplayName(".toResourcesResult(project, params)")
  inner class ToResourcesResult {
    @Test
    fun `should map project to resource items and filter out not requested targets`() {
      // given
      val target1Resource1 = workspaceRoot.resolve("target1/resource1.txt").createParentDirectories().createFile()
      val target1Resource2 = workspaceRoot.resolve("target1/a/resource2.txt").createParentDirectories().createFile()

      val target2Resource1 = workspaceRoot.resolve("target2/resource1.txt").createParentDirectories().createFile()
      val target2Resource2 = workspaceRoot.resolve("target2/resource2.txt").createParentDirectories().createFile()

      workspaceRoot.resolve("target3/resource1.txt").createParentDirectories().createFile()
      workspaceRoot.resolve("target3/resource2.txt").createParentDirectories().createFile()

      val targets =
        listOf(
          createMockTarget(
            name = "//target1",
            kind = "java_library",
            resources = listOf("//target1:resource1.txt", "//target1:a/resource2.txt"),
          ),
          createMockTarget(
            name = "//target2",
            kind = "java_library",
            resources = listOf("//target2:resource1.txt", "//target2:resource2.txt"),
          ),
          createMockTarget(
            name = "//target3",
            kind = "java_library",
            resources = listOf("//target3:resource1.txt", "//target3:resource2.txt"),
          ),
        )
      val project = createMockProject(targets)

      val workspaceContextProvider = MockWorkspaceContextProvider(allowManualTargetsSync = false)
      val mapper = FirstPhaseTargetToBspMapper(workspaceContextProvider, workspaceRoot)

      // when
      val params = ResourcesParams(listOf(Label.parse("//target1"), Label.parse("//target2")))
      val result = mapper.toResourcesResult(project, params)

      // then
      result.items shouldContainExactlyInAnyOrder
        listOf(
          ResourcesItem(
            Label.parse("//target1"),
            listOf(
              target1Resource1.toUri().toString(),
              target1Resource2.toUri().toString(),
            ),
          ),
          ResourcesItem(
            Label.parse("//target2"),
            listOf(
              target2Resource1.toUri().toString(),
              target2Resource2.toUri().toString(),
            ),
          ),
        )
    }

    @Test
    fun `should map project to resource items including resources referenced via filegroup targets`() {
      // given
      val filegroupResource1 = workspaceRoot.resolve("filegroup/resource1.txt").createParentDirectories().createFile()
      val filegroupResource2 = workspaceRoot.resolve("filegroup/a/resource2.txt").createParentDirectories().createFile()

      val target1Resource1 = workspaceRoot.resolve("target1/resource1.txt").createParentDirectories().createFile()
      val target1Resource2 = workspaceRoot.resolve("target1/resource2.txt").createParentDirectories().createFile()

      val targets =
        listOf(
          createMockTarget(
            name = "//filegroup",
            kind = "filegroup",
            resources = listOf("//filegroup:resource1.txt", "//filegroup:a/resource2.txt"),
          ),
          createMockTarget(
            name = "//target1",
            kind = "java_library",
            resources = listOf("//target1:resource1.txt", "//target1:resource2.txt", "//filegroup"),
          ),
        )
      val project = createMockProject(targets)

      val workspaceContextProvider = MockWorkspaceContextProvider(allowManualTargetsSync = false)
      val mapper = FirstPhaseTargetToBspMapper(workspaceContextProvider, workspaceRoot)

      // when
      val params = ResourcesParams(listOf(Label.parse("//target1")))
      val result = mapper.toResourcesResult(project, params)

      // then
      result.items shouldContainExactlyInAnyOrder
        listOf(
          ResourcesItem(
            Label.parse("//target1"),
            listOf(
              target1Resource1.toUri().toString(),
              target1Resource2.toUri().toString(),
              filegroupResource1.toUri().toString(),
              filegroupResource2.toUri().toString(),
            ),
          ),
        )
    }
  }
}

private fun Path.createMockSourceFile(path: String, fullPackage: String): Path {
  val path = resolve(path).createParentDirectories().createFile()
  path.writeText(
    """"
        | package $fullPackage;
        |
        | class A { }
    """.trimMargin(),
  )

  return path
}
