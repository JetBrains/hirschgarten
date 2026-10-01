package org.jetbrains.bazel.sync.workspace.languages.python

import com.google.devtools.intellij.aspect.Common
import com.google.devtools.intellij.ideinfo.IntellijIdeInfo
import com.intellij.bazel.python.backend.sync.MainSourceFinder
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class MainSourceFinderTest {
  @Test
  fun `should find explicitly defined main file`() {
    val sources = listOf("main.py", "library.py")
    val mainFile = "main.py"
    val targetInfo = createTargetInfo("//$PACKAGE_STRING:ccc", sources, mainFile)

    val fileFound = findMainFile(targetInfo)

    fileFound shouldBe artifactLocation(mainFile)
  }

  @Test
  fun `should choose the only source file`() {
    val mainSource = "theOnlySourceFile.py"
    val sources = listOf(mainSource)
    val targetInfo =
      createTargetInfo(
        label = "//$PACKAGE_STRING:ccc",
        sources = sources,
        mainFileRelativePath = null,
      )

    val fileFound = findMainFile(targetInfo)

    fileFound shouldBe artifactLocation(mainSource)
  }

  @Test
  fun `should choose the source file matching the target name in main Bazel repo`() {
    val mainSource = "log_printer.py"
    val sources = listOf(mainSource, "tools/$mainSource", "LogPrinter.py", "tools/library.py")

    val targetInfo1 =
      createTargetInfo(
        label = "//$PACKAGE_STRING:log_printer",
        sources = sources,
        mainFileRelativePath = null,
      )
    val targetInfo2 =
      createTargetInfo(
        label = "//$PACKAGE_STRING:tools/log_printer",
        sources = sources,
        mainFileRelativePath = null,
      )

    val fileFound1 = findMainFile(targetInfo1)
    val fileFound2 = findMainFile(targetInfo2)

    fileFound1 shouldBe artifactLocation(mainSource)
    fileFound2 shouldBe artifactLocation("tools/$mainSource")
  }

  @Test
  fun `should choose the source file matching the target name in nested Bazel repos`() {
    val mainSource = "log_printer.py"
    val sources = listOf(mainSource, "tools/$mainSource", "LogPrinter.py", "library.py")

    val targetInfo1 =
      createTargetInfo(
        label = "@@$REPO_MODULE//$PACKAGE_STRING:log_printer",
        sources = sources,
        mainFileRelativePath = null,
        repo = REPO_MODULE,
      )
    val targetInfo2 =
      createTargetInfo(
        label = "@@$REPO_DEEPER//$PACKAGE_STRING:tools/log_printer",
        sources = sources,
        mainFileRelativePath = null,
        repo = REPO_DEEPER,
      )

    val fileFound1 = findMainFile(targetInfo1)
    val fileFound2 = findMainFile(targetInfo2)

    fileFound1 shouldBe artifactLocation(mainSource, repoRootPath(REPO_MODULE))
    fileFound2 shouldBe artifactLocation("tools/$mainSource", repoRootPath(REPO_DEEPER))
  }

  @Test
  fun `should not fail if unable to find any matching source`() {
    val sources = listOf("main.py", "library.py")
    val targetInfo =
      createTargetInfo(
        label = "//$PACKAGE_STRING:ccc",
        sources = sources,
        mainFileRelativePath = null,
      )

    val fileFound = shouldNotThrowAny { findMainFile(targetInfo) }
    fileFound.shouldBeNull()
  }

  @Test
  fun `should not accept explicitly defined main file with empty path`() {
    val mainSource = "theOnlySourceFile.py"
    val sources = listOf(mainSource)

    val targetInfo1 =
      createTargetInfo(
        label = "//$PACKAGE_STRING:ccc",
        sources = sources,
        mainFileRelativePath = "", // not null, but the path is empty
      )
    val targetInfo2 =
      createTargetInfo(
        label = "@@$REPO_MODULE//$PACKAGE_STRING:ccc",
        sources = sources,
        mainFileRelativePath = "", // not null, but the path is empty
        repo = REPO_MODULE,
      )

    val fileFound1 = findMainFile(targetInfo1)
    val fileFound2 = findMainFile(targetInfo2)

    fileFound1 shouldBe artifactLocation(mainSource)
    fileFound2 shouldBe artifactLocation(mainSource, repoRootPath(REPO_MODULE))
  }
}

private const val REPO_MODULE = "module+"
private const val REPO_DEEPER = "deeper"

private const val PACKAGE_STRING = "aaa/bbb"

private fun repoRootPath(repo: String): String = "external/$repo"

private fun findMainFile(targetInfo: IntellijIdeInfo.TargetIdeInfo): Common.ArtifactLocation? =
  MainSourceFinder.findMainFile(targetInfo, targetInfo.pythonTargetInfo)

private fun createTargetInfo(
  label: String,
  sources: List<String>,
  mainFileRelativePath: String?,
  repo: String? = null,
): IntellijIdeInfo.TargetIdeInfo {
  val repoRootPath = if (repo != null) repoRootPath(repo) else ""
  return IntellijIdeInfo.TargetIdeInfo.newBuilder()
    .setKey(targetKey(label))
    .addAllSrcs(sources.map { artifactLocation(it, repoRootPath) })
    .setPythonTargetInfo(pythonInfo(mainFileRelativePath, repoRootPath))
    .build()
}

private fun artifactLocation(pathRelativeToPackage: String, rootPath: String = ""): Common.ArtifactLocation {
  val fullRelativePath =
    when (pathRelativeToPackage.isBlank()) {
      true -> ""
      false -> "$PACKAGE_STRING/$pathRelativeToPackage"
    }
  return Common.ArtifactLocation.newBuilder()
    .setRootPath(rootPath)
    .setRelativePath(fullRelativePath)
    .setIsSource(true)
    .build()
}

private fun targetKey(label: String) =
  IntellijIdeInfo.TargetKey.newBuilder().setLabel(label).build()

private fun pythonInfo(mainFileRelativePath: String?, rootPath: String = "") =
  IntellijIdeInfo.PythonTargetInfo.newBuilder()
    .apply {
      val mainValue = mainFileRelativePath?.let { artifactLocation(it, rootPath) }
      if (mainValue != null) {
        setMain(mainValue)
      }
    }.build()
