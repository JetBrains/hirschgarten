package org.jetbrains.bazel.sync

import com.google.devtools.intellij.aspect.Common
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.testFramework.common.timeoutRunBlocking
import com.intellij.util.io.createDirectories
import com.intellij.util.io.delete
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.bazel.commons.BazelPathsResolver
import org.jetbrains.bazel.project.BazelProjectFixtures
import org.jetbrains.bazel.sync.workspace.DefaultOutputLocationResolver
import org.jetbrains.bazel.sync.workspace.mapper.normal.DefaultBazelOutputFileHardLinks
import org.jetbrains.bazel.test.framework.testBazelInfo
import org.jetbrains.bazel.workspace.model.test.framework.MockProjectBaseTest
import org.jetbrains.bsp.protocol.OutputLocationParser
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.DisabledOnOs
import org.junit.jupiter.api.condition.OS
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.FileTime
import java.util.concurrent.atomic.AtomicInteger
import kotlin.io.path.Path
import kotlin.io.path.createSymbolicLinkPointingTo
import kotlin.io.path.isSymbolicLink
import kotlin.io.path.readText
import kotlin.io.path.setLastModifiedTime
import kotlin.io.path.writeText

internal class BazelOutFileHardLinksTest : MockProjectBaseTest() {
  @ParameterizedTest
  @ValueSource(booleans = [false, true])
  fun `retargeted symlink with the same modified time`(removeOld: Boolean): Unit = runBlocking {
    val root = Path(checkNotNull(project.basePath)).toRealPath()
    BazelProjectFixtures.initializeBazelProject(project, root)
    val outputBase = root.resolve("qa-output").createDirectories()
    val info = testBazelInfo(workspaceRoot = root, outputBase = outputBase)
    val links = DefaultBazelOutputFileHardLinks(project, info)
    val first = root.resolve("first.h").also { it.writeText("#define VALUE 1\n") }
    val second = root.resolve("second.h").also { it.writeText("#define VALUE 2\n") }
    val sameTime = FileTime.fromMillis(1_700_000_000_000)
    first.setLastModifiedTime(sameTime)
    second.setLastModifiedTime(sameTime)
    val original = info.execRoot.resolve("bazel-out/k8-fastbuild/bin/_virtual_includes/lib/header.h")
    original.parent.createDirectories()
    original.createSymbolicLinkPointingTo(first)

    links.onBeforeSync()
    val cached = checkNotNull(links.createOutputFileHardLink(original))
    links.onAfterSync(false)
    assertThat(cached.isSymbolicLink()).isTrue()
    assertThat(cached.readText()).contains("VALUE 1")

    original.delete()
    if (removeOld) first.delete()
    original.createSymbolicLinkPointingTo(second)
    links.onBeforeSync()
    val refreshed = checkNotNull(links.createOutputFileHardLink(original))
    links.onAfterSync(false)
    assertThat(refreshed.readText()).contains("VALUE 2")
  }

  @Test
  @DisabledOnOs(OS.WINDOWS)
  fun `readable output symlink remains available when its target cannot be hardlinked`(): Unit = runBlocking {
    val root = Path(checkNotNull(project.basePath)).toRealPath()
    BazelProjectFixtures.initializeBazelProject(project, root)
    val outputBase = root.resolve("qa-output").createDirectories()
    val info = testBazelInfo(workspaceRoot = root, outputBase = outputBase)
    val links = DefaultBazelOutputFileHardLinks(project, info)
    val source = Path("/bin/ls").toRealPath()
    val original = info.execRoot.resolve("bazel-out/k8-fastbuild/bin/tool")
    original.parent.createDirectories()
    original.createSymbolicLinkPointingTo(source)

    links.onBeforeSync()
    val paths = links.createOutputFileHardLinks(listOf(original))
    assertThat(Files.isReadable(original)).isTrue()
    assertThat(links.allHardLinksCreatedSuccessfully).isFalse()
    assertThat(paths).containsExactly(source)
  }

  @Test
  fun `symlink to directory should work`(@TempDir outputBase: Path): Unit = timeoutRunBlocking {
    val root = Path(checkNotNull(project.basePath)).toRealPath()
    BazelProjectFixtures.initializeBazelProject(project, root)
    val info = testBazelInfo(workspaceRoot = root, outputBase = outputBase)
    val links = DefaultBazelOutputFileHardLinks(project, info)
    val tree = info.execRoot.resolve("bazel-out/k8-fastbuild/bin/tree").createDirectories()
    tree.resolve("header.h").writeText("VALUE 42")

    val original = tree.resolveSibling("tree-link").createSymbolicLinkPointingTo(tree)
    links.onBeforeSync()

    val cached = checkNotNull(links.createOutputFileHardLink(original))
    assertThat(cached).isEqualTo(links.resolveCachedPath(original))
    links.onAfterSync(true)
    assertThat(cached.resolve("header.h").readText()).isEqualTo("VALUE 42")
    assertThat(links.allHardLinksCreatedSuccessfully).isTrue()
  }

  @Test
  fun `relative output symlink remains readable`(@TempDir outputBase: Path): Unit = timeoutRunBlocking {
    val root = Path.of(checkNotNull(project.basePath)).toRealPath()
    BazelProjectFixtures.initializeBazelProject(project, root)
    val info = testBazelInfo(workspaceRoot = root, outputBase = outputBase)
    val links = DefaultBazelOutputFileHardLinks(project, info)
    val bin = info.execRoot.resolve("bazel-out/k8-fastbuild/bin/pkg").createDirectories()
    bin.resolve("generated.h").writeText("VALUE 42")
    val original = bin.resolve("alias.h").createSymbolicLinkPointingTo(Path.of("generated.h"))

    links.onBeforeSync()
    val cached = checkNotNull(links.createOutputFileHardLink(original))
    assertThat(cached).isEqualTo(links.resolveCachedPath(original))
    assertThat(cached.readText()).isEqualTo("VALUE 42")
    assertThat(links.allHardLinksCreatedSuccessfully).isTrue()
    links.onAfterSync(false)
  }

  @Test
  fun `generated jar of a sibling external repository remains readable after bazel removes it`(@TempDir outputBase: Path): Unit =
    timeoutRunBlocking {
      val root = Path(checkNotNull(project.basePath)).toRealPath()
      BazelProjectFixtures.initializeBazelProject(project, root)
      val info = testBazelInfo(workspaceRoot = root, outputBase = outputBase)
      val links = DefaultBazelOutputFileHardLinks(project, info)
      val parser = OutputLocationParser(BazelPathsResolver(info), links)
      val resolver = DefaultOutputLocationResolver.createHardlinkResolving(info, links)
      val original = info.execRoot.resolve("../repo+/bazel-out/k8-fastbuild/bin/pkg/lib.jar").normalize()
      original.parent.createDirectories()
      original.writeText("JAR")
      val aspectLocation = Common.ArtifactLocation.newBuilder()
        .setRootPath("../repo+")
        .setRelativePath("bazel-out/k8-fastbuild/bin/pkg/lib.jar")
        .setIsSource(false)
        .setIsExternal(true)
        .build()

      links.onBeforeSync()
      val location = parser.parse(aspectLocation)
      links.onAfterSync(true)
      val imported = checkNotNull(resolver.resolve(location))
      original.delete()

      assertThat(imported).isEqualTo(links.cacheDir.resolve("execroot/repo+/bazel-out/k8-fastbuild/bin/pkg/lib.jar"))
      assertThat(imported.readText()).isEqualTo("JAR")
      assertThat(links.allHardLinksCreatedSuccessfully).isTrue()
    }

  @Test
  fun `sync after a cancelled sync links the current content`(@TempDir outputBase: Path): Unit = timeoutRunBlocking {
    val root = Path(checkNotNull(project.basePath)).toRealPath()
    BazelProjectFixtures.initializeBazelProject(project, root)
    val info = testBazelInfo(workspaceRoot = root, outputBase = outputBase)
    val links = DefaultBazelOutputFileHardLinks(project, info)
    val original = info.execRoot.resolve("bazel-out/k8-fastbuild/bin/pkg/lib.jar")
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
  fun `hard links are visible in the VFS after sync`(@TempDir outputBase: Path): Unit = timeoutRunBlocking {
    val root = Path.of(checkNotNull(project.basePath)).toRealPath()
    BazelProjectFixtures.initializeBazelProject(project, root)
    val info = testBazelInfo(workspaceRoot = root, outputBase = outputBase)
    val links = DefaultBazelOutputFileHardLinks(project, info)
    val fileManager = VirtualFileManager.getInstance()
    val bin = info.execRoot.resolve("bazel-out/k8-fastbuild/bin/pkg").createDirectories()
    val first = bin.resolve("first.jar").also { it.writeText("first") }
    val second = bin.resolve("second.jar").also { it.writeText("second") }
    val third = bin.resolve("nested/third.jar").also { it.parent.createDirectories(); it.writeText("third") }

    links.onBeforeSync()
    val firstLink = checkNotNull(links.createOutputFileHardLink(first))
    links.onAfterSync(true)
    assertThat(VfsUtilCore.loadText(checkNotNull(fileManager.findFileByNioPath(firstLink)))).isEqualTo("first")
    // Load all children, so that the VFS does not look for a new child on disk (BAZEL-3647)
    checkNotNull(fileManager.findFileByNioPath(firstLink.parent)).children

    first.delete()
    first.writeText("first, rebuilt")
    first.setLastModifiedTime(FileTime.fromMillis(1_700_000_000_000))
    val refreshes = AtomicInteger()
    val connection = ApplicationManager.getApplication().messageBus.connect()
    connection.subscribe(VirtualFileManager.VFS_CHANGES, object : BulkFileListener {
      override fun after(events: List<VFileEvent>) {
        if (events.any { Path.of(it.path).startsWith(links.cacheDir) }) refreshes.incrementAndGet()
      }
    })
    val secondSyncLinks: List<Path>
    val newLinkBeforeRefresh: VirtualFile?
    try {
      links.onBeforeSync()
      secondSyncLinks = links.createOutputFileHardLinks(listOf(first, second, third))
      newLinkBeforeRefresh = fileManager.findFileByNioPath(links.resolveCachedPath(second))
      links.onAfterSync(true)
    }
    finally {
      connection.disconnect()
    }
    assertThat(refreshes.get()).describedAs("VFS refreshes of the cache directory").isEqualTo(1)
    assertThat(newLinkBeforeRefresh).describedAs("new hard link in the VFS before the refresh").isNull()
    assertThat(secondSyncLinks).containsExactly(firstLink, links.resolveCachedPath(second), links.resolveCachedPath(third))
    assertThat(secondSyncLinks.map { VfsUtilCore.loadText(checkNotNull(fileManager.findFileByNioPath(it))) })
      .containsExactly("first, rebuilt", "second", "third")

    links.onBeforeSync()
    links.createOutputFileHardLink(first)
    links.onAfterSync(true)
    assertThat(links.resolveCachedPath(second)).doesNotExist()
    assertThat(fileManager.findFileByNioPath(links.resolveCachedPath(second))).isNull()
    assertThat(links.resolveCachedPath(third).parent).doesNotExist()
    assertThat(firstLink.readText()).isEqualTo("first, rebuilt")
  }

  @Test
  fun `cleanup deletes an unused file next to a used file`(@TempDir outputBase: Path): Unit = timeoutRunBlocking {
    val links = hardLinksInOutputBase(outputBase)
    val bin = outputBase.resolve("execroot/_main/bazel-out/k8-fastbuild/bin").createDirectories()
    val used = bin.resolve("used.jar").also { it.writeText("used") }
    val unused = bin.resolve("unused.jar").also { it.writeText("unused") }

    sync(links, used, unused)
    assertThat(links.resolveCachedPath(unused)).exists()

    sync(links, used)
    assertThat(links.resolveCachedPath(used)).hasContent("used")
    assertThat(links.resolveCachedPath(unused)).doesNotExist()
  }

  @Test
  fun `cleanup keeps the directories of a used hard link`(@TempDir outputBase: Path): Unit = timeoutRunBlocking {
    val links = hardLinksInOutputBase(outputBase)
    val bin = outputBase.resolve("execroot/_main/bazel-out/k8-fastbuild/bin")
    val used = bin.resolve("a/b/used.jar")
    val unusedFile = bin.resolve("a/unused.jar")
    val unusedDir = bin.resolve("a/b/unused")
    val unusedTopDir = bin.resolve("other")
    for (file in listOf(used, unusedFile, unusedDir.resolve("file.jar"), unusedTopDir.resolve("file.jar"))) {
      file.parent.createDirectories()
      file.writeText(file.fileName.toString())
    }

    sync(links, used, unusedFile, unusedDir, unusedTopDir)
    assertThat(links.resolveCachedPath(unusedDir.resolve("file.jar"))).exists()

    sync(links, used)
    assertThat(links.resolveCachedPath(used)).hasContent("used.jar")
    assertThat(links.resolveCachedPath(unusedFile)).doesNotExist()
    assertThat(links.resolveCachedPath(unusedDir)).doesNotExist()
    assertThat(links.resolveCachedPath(unusedTopDir)).doesNotExist()
  }

  @Test
  fun `cleanup deletes an unused directory in the cache directory`(@TempDir outputBase: Path): Unit = timeoutRunBlocking {
    val links = hardLinksInOutputBase(outputBase)
    val used = outputBase.resolve("execroot/_main/bazel-out/k8-fastbuild/bin/used.jar")
    val unused = outputBase.resolve("external/repo/unused.jar")
    for (file in listOf(used, unused)) {
      file.parent.createDirectories()
      file.writeText(file.fileName.toString())
    }

    sync(links, used, unused)
    assertThat(links.cacheDir.resolve("external")).exists()

    sync(links, used)
    assertThat(links.resolveCachedPath(used)).hasContent("used.jar")
    assertThat(links.cacheDir.resolve("external")).doesNotExist()
  }

  private fun hardLinksInOutputBase(outputBase: Path): DefaultBazelOutputFileHardLinks {
    val root = Path.of(checkNotNull(project.basePath)).toRealPath()
    BazelProjectFixtures.initializeBazelProject(project, root)
    return DefaultBazelOutputFileHardLinks(project, testBazelInfo(workspaceRoot = root, outputBase = outputBase))
  }

  private suspend fun sync(links: DefaultBazelOutputFileHardLinks, vararg files: Path) {
    links.onBeforeSync()
    links.createOutputFileHardLinks(files.toList())
    links.onAfterSync(true)
  }
}
