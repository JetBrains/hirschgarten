package org.jetbrains.bazel.sync

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
}
