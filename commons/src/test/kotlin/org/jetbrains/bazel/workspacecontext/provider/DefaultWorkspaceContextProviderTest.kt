package org.jetbrains.bazel.workspacecontext.provider

import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.commons.EnvironmentProvider
import org.jetbrains.bazel.commons.FileUtil
import org.jetbrains.bazel.commons.SystemInfoProvider
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.label.TargetPattern
import org.jetbrains.bazel.startup.FileUtilIntellij
import org.jetbrains.bazel.startup.IntellijEnvironmentProvider
import org.jetbrains.bazel.startup.IntellijSystemInfoProvider
import org.jetbrains.bazel.workspacecontext.TargetsSpec
import org.jetbrains.bsp.protocol.FeatureFlags
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Path
import kotlin.io.path.createFile
import kotlin.io.path.createTempDirectory
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

class DefaultWorkspaceContextProviderTest {
  private lateinit var workspaceRoot: Path
  private lateinit var projectViewFile: Path
  private lateinit var dotBazelBspDirPath: Path

  @BeforeEach
  fun beforeEach() {
    // Initialize providers for tests
    SystemInfoProvider.provideSystemInfoProvider(IntellijSystemInfoProvider)
    FileUtil.provideFileUtil(FileUtilIntellij)
    EnvironmentProvider.provideEnvironmentProvider(IntellijEnvironmentProvider)

    workspaceRoot = createTempDirectory("workspaceRoot")
    projectViewFile = workspaceRoot.resolve("projectview.bazelproject")
    dotBazelBspDirPath = workspaceRoot.resolve(".bazelbsp")
  }

  @Test
  fun `should parse project view file and return workspace context`() {
    // given
    projectViewFile.createFile()
    projectViewFile.writeText(
      """
      |targets:
      |  //a/b/c
      """.trimMargin(),
    )

    val provider = DefaultWorkspaceContextProvider(workspaceRoot, projectViewFile, dotBazelBspDirPath, FeatureFlags())

    // when
    val workspaceContext = provider.readWorkspaceContext()

    // then
    workspaceContext.targets shouldBe TargetsSpec(listOf(TargetPattern.parse("//a/b/c")), emptyList())
  }

  @Test
  fun `should generate an empty project view file if the file doesn't exist`() {
    // given
    projectViewFile.deleteIfExists()
    val provider = DefaultWorkspaceContextProvider(workspaceRoot, projectViewFile, dotBazelBspDirPath, FeatureFlags())

    // when
    val workspaceContext = provider.readWorkspaceContext()

    // then
    workspaceContext.targets shouldBe TargetsSpec(emptyList(), emptyList())
    projectViewFile.exists() shouldBe true
    projectViewFile.readText().trim() shouldBe ""
  }
}
