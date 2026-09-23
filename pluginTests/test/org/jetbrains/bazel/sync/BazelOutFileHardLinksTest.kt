package org.jetbrains.bazel.sync

import com.intellij.util.io.createDirectories
import com.intellij.util.io.delete
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.bazel.project.BazelProjectFixtures
import org.jetbrains.bazel.sync.workspace.mapper.normal.DefaultBazelOutputFileHardLinks
import org.jetbrains.bazel.test.framework.testBazelInfo
import org.jetbrains.bazel.workspace.model.test.framework.MockProjectBaseTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.DisabledOnOs
import org.junit.jupiter.api.condition.OS
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.nio.file.Files
import java.nio.file.attribute.FileTime
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
}
