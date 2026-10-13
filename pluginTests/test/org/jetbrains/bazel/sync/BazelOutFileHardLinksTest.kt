package org.jetbrains.bazel.sync

import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.testFramework.common.timeoutRunBlocking
import com.intellij.util.io.createDirectories
import com.intellij.util.io.delete
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.bazel.commons.BazelInfo
import org.jetbrains.bazel.commons.BazelRelease
import org.jetbrains.bazel.project.BazelProjectFixtures
import org.jetbrains.bazel.sync.workspace.mapper.normal.DefaultBazelOutputFileHardLinks
import org.jetbrains.bazel.workspace.model.test.framework.MockProjectBaseTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.nio.file.attribute.FileTime
import kotlin.io.path.readText
import kotlin.io.path.relativeTo
import kotlin.io.path.setLastModifiedTime
import kotlin.io.path.writeText

internal class BazelOutFileHardLinksTest : MockProjectBaseTest() {
  @Test
  fun `sync after a cancelled sync links the current content`(@TempDir outputBase: Path): Unit = timeoutRunBlocking {
    val links = hardLinksInOutputBase(outputBase)
    val original = outputBase.resolve("execroot/_main/bazel-out/k8-fastbuild/bin/pkg/lib.jar")
    original.parent.createDirectories()
    original.writeText("first")

    links.onBeforeSync()
    links.createOutputFileHardLink(original)
    // ProjectSyncTask calls onAfterSync in a finally block, also when the user cancels the sync
    val cancelledSync = launch(start = CoroutineStart.UNDISPATCHED) {
      try {
        awaitCancellation()
      }
      finally {
        links.onAfterSync(true)
      }
    }
    cancelledSync.cancelAndJoin()

    // Bazel deletes and recreates an output file when it changes it
    original.delete()
    original.writeText("second")
    original.setLastModifiedTime(FileTime.fromMillis(1_700_000_000_000))
    links.onBeforeSync()
    val link = checkNotNull(links.createOutputFileHardLink(original))
    links.onAfterSync(true)
    assertThat(link.readText()).isEqualTo("second")
  }

  @Test
  fun `hard links are visible in the VFS before the project model update`(@TempDir outputBase: Path): Unit = timeoutRunBlocking {
    val links = hardLinksInOutputBase(outputBase)
    val fileManager = VirtualFileManager.getInstance()
    val jar = outputBase.resolve("execroot/_main/bazel-out/k8-fastbuild/bin/lib.jar")
    jar.parent.createDirectories()
    jar.writeText("jar")
    // Load all children of the output base before the cache directory exists, as on CI with a reused output base
    checkNotNull(fileManager.refreshAndFindFileByNioPath(outputBase)).children

    links.onBeforeSync()
    val link = checkNotNull(links.createOutputFileHardLink(jar))
    assertThat(fileManager.findFileByNioPath(link)).describedAs("hard link in the VFS before the hook").isNull()
    links.onBeforeProjectModelUpdate()
    assertThat(fileManager.findFileByNioPath(link)).describedAs("hard link in the VFS after the hook").isNotNull()
    links.onAfterSync(true)
  }

  @Test
  fun `hard links are visible in the VFS after sync`(@TempDir outputBase: Path): Unit = timeoutRunBlocking {
    val links = hardLinksInOutputBase(outputBase)
    val fileManager = VirtualFileManager.getInstance()
    val bin = outputBase.resolve("execroot/_main/bazel-out/k8-fastbuild/bin/pkg").createDirectories()
    val first = bin.resolve("first.jar").also { it.writeText("first") }
    val second = bin.resolve("second.jar").also { it.writeText("second") }

    sync(links, first)
    val firstLink = cachedPath(outputBase, first)
    // Load all children, so that the VFS does not look for a new child on disk (BAZEL-3647)
    checkNotNull(fileManager.findFileByNioPath(firstLink.parent)).children

    sync(links, first, second)
    val secondLink = cachedPath(outputBase, second)
    assertThat(checkNotNull(fileManager.findFileByNioPath(secondLink)).contentsToByteArray().decodeToString()).isEqualTo("second")
  }

  @Test
  fun `cleanup deletes an unused file next to a used file`(@TempDir outputBase: Path): Unit = timeoutRunBlocking {
    val links = hardLinksInOutputBase(outputBase)
    val bin = outputBase.resolve("execroot/_main/bazel-out/k8-fastbuild/bin").createDirectories()
    val used = bin.resolve("used.jar").also { it.writeText("used") }
    val unused = bin.resolve("unused.jar").also { it.writeText("unused") }

    sync(links, used, unused)
    assertThat(cachedPath(outputBase, unused)).exists()

    sync(links, used)
    assertThat(cachedPath(outputBase, used)).hasContent("used")
    assertThat(cachedPath(outputBase, unused)).doesNotExist()
  }

  @Test
  fun `cleanup keeps the directories of a used hard link`(@TempDir outputBase: Path): Unit = timeoutRunBlocking {
    val links = hardLinksInOutputBase(outputBase)
    val bin = outputBase.resolve("execroot/_main/bazel-out/k8-fastbuild/bin")
    val used = bin.resolve("a/b/used.jar")
    val unusedFile = bin.resolve("a/unused.jar")
    val unusedNestedFile = bin.resolve("a/b/unused/file.jar")
    val unusedTopFile = bin.resolve("other/file.jar")
    for (file in listOf(used, unusedFile, unusedNestedFile, unusedTopFile)) {
      file.parent.createDirectories()
      file.writeText(file.fileName.toString())
    }

    sync(links, used, unusedFile, unusedNestedFile, unusedTopFile)
    assertThat(cachedPath(outputBase, unusedNestedFile)).exists()

    sync(links, used)
    assertThat(cachedPath(outputBase, used)).hasContent("used.jar")
    assertThat(cachedPath(outputBase, unusedFile)).doesNotExist()
    assertThat(cachedPath(outputBase, unusedNestedFile).parent).doesNotExist()
    assertThat(cachedPath(outputBase, unusedTopFile).parent).doesNotExist()
  }

  private fun hardLinksInOutputBase(outputBase: Path): DefaultBazelOutputFileHardLinks {
    val root = Path.of(checkNotNull(project.basePath)).toRealPath()
    BazelProjectFixtures.initializeBazelProject(project, root)
    val execRoot = outputBase.resolve("execroot").resolve("_main")
    val info = BazelInfo(
      execRoot = execRoot,
      outputBase = outputBase,
      workspaceRoot = root,
      bazelBin = execRoot.resolve("bazel-bin"),
      release = BazelRelease.FALLBACK_VERSION,
      isBzlModEnabled = true,
      isWorkspaceEnabled = false,
      externalAutoloads = emptyList(),
    )
    return DefaultBazelOutputFileHardLinks(project, info)
  }

  private fun cachedPath(outputBase: Path, file: Path): Path =
    outputBase.resolve("intellij-hardlinks").resolve(file.relativeTo(outputBase))

  private suspend fun sync(links: DefaultBazelOutputFileHardLinks, vararg files: Path) {
    links.onBeforeSync()
    links.createOutputFileHardLinks(files.toList())
    links.onAfterSync(true)
  }
}
