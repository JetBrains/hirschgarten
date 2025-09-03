package org.jetbrains.bazel.sync.workspace.mapper.normal

import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.commons.EnvironmentProvider
import org.jetbrains.bazel.commons.FileUtil
import org.jetbrains.bazel.commons.SystemInfoProvider
import org.jetbrains.bazel.commons.Tag
import org.jetbrains.bazel.info.BspTargetInfo
import org.jetbrains.bazel.startup.FileUtilIntellij
import org.jetbrains.bazel.startup.IntellijEnvironmentProvider
import org.jetbrains.bazel.startup.IntellijSystemInfoProvider
import org.jetbrains.bazel.workspacecontext.WorkspaceContext
import org.jetbrains.bazel.workspacecontext.provider.DefaultWorkspaceContextProvider
import org.jetbrains.bsp.protocol.FeatureFlags
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.nio.file.Path
import kotlin.io.path.createTempDirectory

class TargetTagsResolverTest {
  private lateinit var workspaceRoot: Path
  private lateinit var projectViewFile: Path
  private lateinit var dotBazelBspDirPath: Path
  private lateinit var workspaceContext: WorkspaceContext

  @BeforeEach
  fun beforeEach() {
    // Initialize providers for tests
    SystemInfoProvider.provideSystemInfoProvider(IntellijSystemInfoProvider)
    FileUtil.provideFileUtil(FileUtilIntellij)
    EnvironmentProvider.provideEnvironmentProvider(IntellijEnvironmentProvider)

    workspaceRoot = createTempDirectory("workspaceRoot")
    projectViewFile = workspaceRoot.resolve("projectview.bazelproject")
    dotBazelBspDirPath = workspaceRoot.resolve(".bazelbsp")
    workspaceContext =
      DefaultWorkspaceContextProvider(workspaceRoot, projectViewFile, dotBazelBspDirPath, FeatureFlags())
        .readWorkspaceContext()
  }

  @Test
  fun `should map executable targets`() {
    val targetInfo =
      BspTargetInfo.TargetInfo
        .newBuilder()
        .also {
          it.executable = true
        }.build()

    val tags = TargetTagsResolver().resolveTags(targetInfo, workspaceContext)

    tags shouldBe setOf(Tag.APPLICATION)
  }

  @Test
  fun `should map a target with _binary to library if it is not executable`() {
    val targetInfo =
      BspTargetInfo.TargetInfo
        .newBuilder()
        .apply {
          kind = "it_only_looks_like_binary"
          executable = false
        }.build()

    val tags = TargetTagsResolver().resolveTags(targetInfo, workspaceContext)

    tags shouldBe setOf(Tag.LIBRARY)
  }

  @Test
  fun `should map test targets`() {
    val targetInfo =
      BspTargetInfo.TargetInfo
        .newBuilder()
        .also {
          it.kind = "blargh_test"
          it.executable = true
        }.build()

    val tags = TargetTagsResolver().resolveTags(targetInfo, workspaceContext)

    tags shouldBe setOf(Tag.TEST)
  }

  @Test
  fun `should map intellij_plugin_debug_target`() {
    val targetInfo =
      BspTargetInfo.TargetInfo
        .newBuilder()
        .also {
          it.kind = "intellij_plugin_debug_target"
        }.build()

    val tags = TargetTagsResolver().resolveTags(targetInfo, workspaceContext)

    tags shouldBe setOf(Tag.INTELLIJ_PLUGIN, Tag.APPLICATION)
  }

  @Test
  fun `should map no-ide and manual tags for library`() {
    val targetInfo =
      BspTargetInfo.TargetInfo
        .newBuilder()
        .also {
          it.addTags("no-ide")
          it.addTags("manual")
        }.build()

    val tags = TargetTagsResolver().resolveTags(targetInfo, workspaceContext)

    tags shouldBe setOf(Tag.LIBRARY, Tag.NO_IDE, Tag.MANUAL)
  }

  @Test
  fun `should map no-ide and manual tags for application`() {
    val targetInfo =
      BspTargetInfo.TargetInfo
        .newBuilder()
        .also {
          it.executable = true
          it.addTags("no-ide")
          it.addTags("manual")
        }.build()

    val tags = TargetTagsResolver().resolveTags(targetInfo, workspaceContext)

    tags shouldBe setOf(Tag.APPLICATION, Tag.NO_IDE, Tag.MANUAL)
  }

  // now in the implementation we don't have any special cases, but they used to be handled differently,
  // so let's keep the test just to make sure that they are handled correctly anyway
  @ParameterizedTest
  @ValueSource(strings = ["resources_union", "java_import", "aar_import"])
  fun `should handle special cases`(name: String) {
    val targetInfo =
      BspTargetInfo.TargetInfo
        .newBuilder()
        .also {
          it.kind = name
        }.build()

    val tags = TargetTagsResolver().resolveTags(targetInfo, workspaceContext)

    tags shouldBe setOf(Tag.LIBRARY)
  }
}
