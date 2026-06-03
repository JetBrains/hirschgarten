package org.jetbrains.bazel.sync.workspace.languages.python

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.commons.BazelInfo
import org.jetbrains.bazel.commons.BazelPathsResolver
import org.jetbrains.bazel.commons.BazelRelease
import org.jetbrains.bazel.commons.LocalRepositoryMapping
import org.jetbrains.bazel.info.BspTargetInfo
import org.junit.jupiter.api.Test
import java.nio.file.Path
import kotlin.io.path.Path

class MainSourceFinderTest {
  @Test
  fun `should find explicitly defined main file`() {
    val sources = listOf("main.py", "library.py")
    val mainFile = "main.py"
    val targetInfo = createTargetInfo("//$PACKAGE_STRING:ccc", sources, mainFile)

    val fileFound = findMainFile(targetInfo)

    fileFound shouldBe absolutePackagePath().resolve(mainFile)
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

    fileFound shouldBe absolutePackagePath().resolve(mainSource)
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

    fileFound1 shouldBe absolutePackagePath().resolve(mainSource)
    fileFound2 shouldBe absolutePackagePath().resolve(Path.of("tools", mainSource))
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

    val expected1 = absolutePackagePath(REPO_MODULE).resolve(mainSource)
    val expected2 = absolutePackagePath(REPO_DEEPER).resolve(Path.of("tools", mainSource))

    fileFound1 shouldBe expected1
    fileFound2 shouldBe expected2
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

    fileFound1 shouldBe absolutePackagePath().resolve(mainSource)
    fileFound2 shouldBe absolutePackagePath(REPO_MODULE).resolve(mainSource)
  }
}

private val WORKSPACE_ROOT = Path.of("projects", "mockProject")
private const val REPO_MODULE = "module+"
private const val REPO_DEEPER = "deeper"

private const val PACKAGE_STRING = "aaa/bbb"
private val PACKAGE_RELATIVE_PATH = Path.of("aaa", "bbb")

private val repoPaths =
  mapOf(
    REPO_MODULE to Path.of("module"),
    REPO_DEEPER to Path.of("level", "deeper"),
  )

private val localRepositoryMapping = LocalRepositoryMapping(localRepositories = repoPaths)

private val mockBazelInfo =
  BazelInfo(
    execRoot = Path("execRoot"),
    outputBase = Path("outputBase"),
    workspaceRoot = WORKSPACE_ROOT,
    bazelBin = Path("bazel-bin"),
    release = BazelRelease(7),
    isBzlModEnabled = true,
    isWorkspaceEnabled = true,
    externalAutoloads = emptyList(),
  )

private val bazelPathsResolver = BazelPathsResolver(mockBazelInfo)

private fun findMainFile(targetInfo: BspTargetInfo.TargetInfo): Path? =
  MainSourceFinder.findMainFile(targetInfo, targetInfo.pythonTargetInfo, bazelPathsResolver, localRepositoryMapping)

private fun createTargetInfo(
  label: String,
  sources: List<String>,
  mainFileRelativePath: String?,
  repo: String? = null,
): BspTargetInfo.TargetInfo {
  val repoRootPath = if (repo != null) "external/$repo" else ""
  return BspTargetInfo.TargetInfo.newBuilder()
    .setKey(targetKey(label))
    .addAllSrcs(sources.map { artifactLocation(it, repoRootPath) })
    .setPythonTargetInfo(pythonInfo(mainFileRelativePath, repoRootPath))
    .build()
}

private fun artifactLocation(pathRelativeToPackage: String, rootPath: String = ""): BspTargetInfo.ArtifactLocation {
  val fullRelativePath =
    when (pathRelativeToPackage.isBlank()) {
      true -> ""
      false -> "$PACKAGE_STRING/$pathRelativeToPackage"
    }
  return BspTargetInfo.ArtifactLocation.newBuilder()
    .setRootPath(rootPath)
    .setRelativePath(fullRelativePath)
    .setIsSource(true)
    .build()
}

private fun targetKey(label: String) =
  BspTargetInfo.TargetKey.newBuilder().setLabel(label).build()

private fun pythonInfo(mainFileRelativePath: String?, rootPath: String = "") =
  BspTargetInfo.PythonTargetInfo.newBuilder()
    .apply {
      val mainValue = mainFileRelativePath?.let { artifactLocation(it, rootPath) }
      if (mainValue != null) {
        setMain(mainValue)
      }
    }.build()

private fun absolutePackagePath(repoName: String? = null): Path {
  val repoPath = when (repoName) {
    null -> WORKSPACE_ROOT
    else -> WORKSPACE_ROOT.resolve(repoPaths[repoName]!!)
  }
  return repoPath.resolve(PACKAGE_RELATIVE_PATH)
}
