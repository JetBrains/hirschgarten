package org.jetbrains.bsp.bazel.server.sync.firstStep.mappings

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.SourceItemKind
import ch.epfl.scala.bsp4j.SourcesItem
import ch.epfl.scala.bsp4j.SourcesParams
import com.google.devtools.build.lib.query2.proto.proto2api.Build.Target
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import org.jetbrains.bsp.bazel.bazelrunner.utils.BazelRelease
import org.jetbrains.bsp.bazel.server.model.Label
import org.jetbrains.bsp.bazel.server.model.Project
import org.jetbrains.bsp.bazel.workspacecontext.AllowManualTargetsSyncSpec
import org.jetbrains.bsp.bazel.workspacecontext.AndroidMinSdkSpec
import org.jetbrains.bsp.bazel.workspacecontext.BazelBinarySpec
import org.jetbrains.bsp.bazel.workspacecontext.BuildFlagsSpec
import org.jetbrains.bsp.bazel.workspacecontext.DirectoriesSpec
import org.jetbrains.bsp.bazel.workspacecontext.DotBazelBspDirPathSpec
import org.jetbrains.bsp.bazel.workspacecontext.EnableNativeAndroidRules
import org.jetbrains.bsp.bazel.workspacecontext.EnabledRulesSpec
import org.jetbrains.bsp.bazel.workspacecontext.ExperimentalAddTransitiveCompileTimeJars
import org.jetbrains.bsp.bazel.workspacecontext.IdeJavaHomeOverrideSpec
import org.jetbrains.bsp.bazel.workspacecontext.ImportDepthSpec
import org.jetbrains.bsp.bazel.workspacecontext.TargetsSpec
import org.jetbrains.bsp.bazel.workspacecontext.WorkspaceContext
import org.jetbrains.bsp.bazel.workspacecontext.WorkspaceContextProvider
import org.jetbrains.bsp.protocol.EnhancedSourceItem
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
      targets = TargetsSpec(listOf(BuildTargetIdentifier("//...")), emptyList()),
      directories = DirectoriesSpec(listOf(Path(".")), emptyList()),
      buildFlags = BuildFlagsSpec(emptyList()),
      bazelBinary = BazelBinarySpec(Path("bazel")),
      allowManualTargetsSync = AllowManualTargetsSyncSpec(allowManualTargetsSync),
      dotBazelBspDirPath = DotBazelBspDirPathSpec(Path(".bazelbsp")),
      importDepth = ImportDepthSpec(-1),
      enabledRules = EnabledRulesSpec(emptyList()),
      ideJavaHomeOverrideSpec = IdeJavaHomeOverrideSpec(Path("java_home")),
      experimentalAddTransitiveCompileTimeJars = ExperimentalAddTransitiveCompileTimeJars(false),
      enableNativeAndroidRules = EnableNativeAndroidRules(false),
      androidMinSdkSpec = AndroidMinSdkSpec(null),
    )
}

class TargetToBspMapperTest {
  @Nested
  @DisplayName(".toWorkspaceBuildTargetsResult(project)")
  inner class ToWorkspaceBuildTargetsResult {
    @Test
    fun `should map targets to bsp build targets and filter out manual, no ide targets and unsupported targets`() {
      // given
      val targets =
        listOf(
          createMockTarget(name = "//target1", kind = "java_library"),
          createMockTarget(name = "//manual_target", kind = "java_library", tags = listOf("manual")),
          createMockTarget(name = "//no_ide_target", kind = "java_library", tags = listOf("no-ide")),
          createMockTarget(name = "//unsupported_target", kind = "unsupported_target"),
        )
      val project = createMockProject(targets)

      val workspaceContextProvider = MockWorkspaceContextProvider(allowManualTargetsSync = false)
      val mapper = TargetToBspMapper(workspaceContextProvider, Path("/workspace/root"))

      // when
      val result = mapper.toWorkspaceBuildTargetsResult(project)

      // then
      result.targets.map { it.id.uri } shouldContainExactlyInAnyOrder listOf("//target1")
    }

    @Test
    fun `should map targets to bsp build targets and keep manual targets if manual targets sync is allowed`() {
      // given
      val targets =
        listOf(
          createMockTarget(name = "//target1", kind = "java_library"),
          createMockTarget(name = "//manual_target", kind = "java_library", tags = listOf("manual")),
        )
      val project = createMockProject(targets)

      val workspaceContextProvider = MockWorkspaceContextProvider(allowManualTargetsSync = true)
      val mapper = TargetToBspMapper(workspaceContextProvider, Path("/workspace/root"))

      // when
      val result = mapper.toWorkspaceBuildTargetsResult(project)

      // then
      result.targets.map { it.id.uri } shouldContainExactlyInAnyOrder listOf("//target1", "//manual_target")
    }
  }

  @Nested
  @DisplayName(".toSourcesResult(project, params)")
  inner class ToSourcesResult {
    @Test
    fun `should map project to source items and filter out not requested targets`() {
      // given
      val workspaceRoot = createTempDirectory("workspaceRoot").also { it.toFile().deleteOnExit() }

      val target1Root1 = workspaceRoot.resolve("target1")
      val target1Root2 = workspaceRoot.resolve("target1/a")
      val target1Src1 = workspaceRoot.createMockSourceFile("target1/src1.java", "com.example")
      val target1Src2 = workspaceRoot.createMockSourceFile("target1/a/src2.java", "com.example.a")

      val target2Root = workspaceRoot.resolve("target2")
      val target2Src1 = workspaceRoot.createMockSourceFile("target2/src1.java", "com.example")
      val target2Src2 = workspaceRoot.createMockSourceFile("target2/src2.java", "com.example")

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
            srcs = listOf("//target2:src1.java", "//target2:src2.java"),
          ),
          createMockTarget(
            name = "//target3",
            kind = "java_library",
            srcs = listOf("//target3:src1.java", "//target3:src2.java"),
          ),
        )
      val project = createMockProject(targets)

      val workspaceContextProvider = MockWorkspaceContextProvider(allowManualTargetsSync = false)
      val mapper = TargetToBspMapper(workspaceContextProvider, workspaceRoot)

      // when
      val params = SourcesParams(listOf(BuildTargetIdentifier("//target1"), BuildTargetIdentifier("//target2")))
      val result = mapper.toSourcesResult(project, params)

      // then
      result.items shouldContainExactlyInAnyOrder
        listOf(
          SourcesItem(
            BuildTargetIdentifier("//target1"),
            listOf(
              EnhancedSourceItem(target1Src1.toUri().toString(), SourceItemKind.FILE, false),
              EnhancedSourceItem(target1Src2.toUri().toString(), SourceItemKind.FILE, false),
            ),
          ).apply {
            roots = listOf(target1Root1.toUri().toString(), target1Root2.toUri().toString())
          },
          SourcesItem(
            BuildTargetIdentifier("//target2"),
            listOf(
              EnhancedSourceItem(target2Src1.toUri().toString(), SourceItemKind.FILE, false),
              EnhancedSourceItem(target2Src2.toUri().toString(), SourceItemKind.FILE, false),
            ),
          ).apply {
            roots = listOf(target2Root.toUri().toString())
          },
        )
    }
  }
}

private fun createMockProject(lightweightModules: List<Target>): Project =
  Project(
    workspaceRoot = URI.create("file:///path/to/workspace"),
    modules = emptyList(),
    libraries = emptyMap(),
    goLibraries = emptyMap(),
    invalidTargets = emptyList(),
    nonModuleTargets = emptyList(),
    bazelRelease = BazelRelease(7),
    lightweightModules = lightweightModules.associateBy { Label.parse(it.rule.name) },
  )

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
