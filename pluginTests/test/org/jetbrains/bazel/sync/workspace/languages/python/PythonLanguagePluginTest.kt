package org.jetbrains.bazel.sync.workspace.languages.python

import com.google.devtools.intellij.aspect.Common.ArtifactLocation
import com.google.devtools.intellij.ideinfo.IntellijIdeInfo
import com.intellij.bazel.python.backend.sync.PythonLanguagePlugin
import com.intellij.testFramework.common.timeoutRunBlocking
import com.intellij.testFramework.junit5.TestApplication
import org.jetbrains.bazel.python.debug.PythonDebugUtils
import org.jetbrains.bazel.python.lang.PythonBuildTarget
import org.jetbrains.bazel.sync.BazelOutFileHardLinks
import org.jetbrains.bazel.test.framework.CopyingBazelOutHardLinks
import org.jetbrains.bazel.test.framework.testBazelInfo
import org.jetbrains.bazel.workspace.model.test.framework.BuildServerMock
import org.jetbrains.bazel.workspace.model.test.framework.TEST_OUTPUT_ROOT
import org.jetbrains.bazel.workspace.model.test.framework.generatedTestLocation
import org.jetbrains.bsp.protocol.OutputLocation
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.assertEquals

@TestApplication
class PythonLanguagePluginTest {
  @TempDir
  lateinit var directory: Path

  @Test
  fun `python target args are extracted from aspect target info`() {
    val target =
      IntellijIdeInfo.TargetIdeInfo.newBuilder()
        .setPyIdeInfo(
          IntellijIdeInfo.PyIdeInfo.newBuilder()
            .addAllArgs(listOf("--flag", "two words")),
        ).build()

    assertEquals(listOf("--flag", "two words"), PythonDebugUtils.extractPythonTargetArgs(target))
  }

  @Test
  fun `generated stubs are prepared before workspace import`(): Unit = timeoutRunBlocking {
    val info = testBazelInfo(workspaceRoot = directory.resolve("workspace"), outputBase = directory.resolve("output"))
    val sourceDir = info.execRoot.resolve("bazel-out/${TEST_OUTPUT_ROOT.path}/generated").createDirectories()
    sourceDir.resolve("module.py").writeText("VALUE = 1\n")
    val stub = sourceDir.resolve("module.pyi").also { it.writeText("VALUE: int\n") }
    val missing = sourceDir.resolve("missing.py")
    val links = CopyingBazelOutHardLinks(info.outputBase, directory.resolve("cache"))
    val server = object : BuildServerMock() {
      override val bazelInfo = info
      override val outFileHardLinks = object : BazelOutFileHardLinks by BazelOutFileHardLinks.NONE {
        override suspend fun createOutputFileHardLink(originalFile: Path): Path? = links.createOutputFileHardLink(originalFile)
        override fun resolveCachedPath(fileOrDir: Path): Path = links.resolveCachedPath(fileOrDir)
      }
    }
    fun artifact(name: String): ArtifactLocation = ArtifactLocation.newBuilder()
      .setRootPath("bazel-out/${TEST_OUTPUT_ROOT.path}")
      .setRelativePath("generated/$name")
      .build()
    val target = IntellijIdeInfo.TargetIdeInfo.newBuilder()
      .setKey(IntellijIdeInfo.TargetKey.newBuilder().setLabel("//generated:module"))
      .setPythonTargetInfo(
        IntellijIdeInfo.PythonTargetInfo.newBuilder()
          .addGeneratedSources(artifact("module.py"))
          .addGeneratedSources(artifact("missing.py")),
      )
      .build()

    val mapped = PythonLanguagePlugin().mapBuildTargetData(server, target).filterIsInstance<PythonBuildTarget>().single()

    val cachedStub = links.resolveCachedPath(stub)
    assertEquals(
      mapOf(generatedTestLocation("generated/missing.py") to OutputLocation.Host(missing.toString())),
      mapped.generatedSourceOverrides,
    )
    assertEquals(
      setOf(generatedTestLocation("generated/module.pyi"), generatedTestLocation("generated/missing.py")),
      mapped.generatedSources.getOutputLocations().toSet(),
    )
    assertEquals("VALUE: int\n", cachedStub.readText())
    assertEquals(listOf(cachedStub), links.linkedFiles)
  }
}
