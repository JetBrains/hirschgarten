package org.jetbrains.bazel.tests.flow

import com.intellij.driver.client.Driver
import com.intellij.driver.client.service
import com.intellij.driver.sdk.projectFileIndex
import com.intellij.driver.sdk.step
import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.ide.starter.driver.engine.runIdeWithDriver
import com.intellij.ide.starter.ide.IDETestContext
import com.intellij.openapi.util.SystemInfoRt
import com.intellij.tools.ide.performanceTesting.commands.waitForSmartMode
import org.jetbrains.bazel.base.IdeStarterBaseProjectTest
import org.jetbrains.bazel.base.execute
import org.jetbrains.bazel.base.projectRootDir
import org.jetbrains.bazel.base.syncBazelProject
import org.jetbrains.bazel.data.BazelProjectConfigurer
import org.jetbrains.bazel.data.IdeStarterBazelProject
import org.jetbrains.bazel.data.IdeaBazelCases
import org.jetbrains.bazel.data.simpleBazelProject
import org.jetbrains.bazel.tests.combined.VirtualFileManager
import org.jetbrains.bazel.utils.isWindowsJunction
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.nio.file.LinkOption
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.Path
import kotlin.io.path.appendText
import kotlin.io.path.div
import kotlin.io.path.name
import kotlin.io.path.readAttributes
import kotlin.time.Duration.Companion.minutes

private val BAZEL_SYMLINK_EXCLUDE_PROJECT = simpleBazelProject(
  path = "simpleKotlinTest",
  configureProject = { context ->
    BazelProjectConfigurer.configureProjectBeforeUse(
      context,
      createProjectView = false,
    )
  },
)

/**
 * Tests the exclusion of the convenience symlinks.
 *
 * The IDE must exclude a junction on Windows and a symbolic link on every other OS.
 * Bazel always uses junctions on Windows, even if the `--w
  *
 */
internal class BazelSymlinkExcludeTest : IdeStarterBaseProjectTest() {

  @Test
  fun `should exclude the convenience symlinks`() {
    runIdeAndAssertExclusion(BAZEL_SYMLINK_EXCLUDE_PROJECT)
  }

  @Test
  fun `should exclude the convenience symlinks with custom prefix`() {
    runIdeAndAssertExclusion(BAZEL_SYMLINK_EXCLUDE_PROJECT, symlinkPrefix = "foo-")
  }

  private fun runIdeAndAssertExclusion(
    project: IdeStarterBazelProject,
    symlinkPrefix: String? = null,
  ) {
    val context = createContext("bazelSymlinkExcludeTest", IdeaBazelCases.withProject(project))
    if (symlinkPrefix != null) {
      val bazelrc = context.resolvedProjectHome / ".bazelrc"
      bazelrc.appendText("build --symlink_prefix=$symlinkPrefix\n")
    }
    context
      .runIdeWithDriver(runTimeout = timeout)
      .useDriverAndCloseIde {
        ideFrame {
          syncBazelProject(buildAndSync = true)
          execute { waitForSmartMode() }
          waitForIndicators(5.minutes)
        }
        step("Refresh the VFS") {
          service<VirtualFileManager>().asyncRefresh()
          ideFrame { waitForIndicators(1.minutes) }
        }
        step("Check that the IDE excludes the convenience symlinks") {
          assertConvenienceSymlinksAreExcluded(context, symlinkPrefix ?: "bazel-")
        }
      }
  }
}

private fun Driver.assertConvenienceSymlinksAreExcluded(
  context: IDETestContext,
  symlinkPrefix: String,
) {
  val home = context.resolvedProjectHome
  val fileIndex = projectFileIndex()
  val symlinks = listOf("${symlinkPrefix}bin", "${symlinkPrefix}out", "${symlinkPrefix}testlogs", "${symlinkPrefix}${home.name}")
  symlinks.forEach { symlink ->
    val file = projectRootDir.findFileByRelativePath(symlink) ?: Assertions.fail("Symlink $symlink not found!")
    val attributes = Path(file.getPath()).readAttributes<BasicFileAttributes>(LinkOption.NOFOLLOW_LINKS)
    Assertions.assertTrue(attributes.isConvenienceSymlink, "$symlink is not a convenience symlink")
    Assertions.assertTrue(fileIndex.isExcluded(file), "$symlink is not excluded")
  }
}

private val BasicFileAttributes.isConvenienceSymlink: Boolean
  get() = if (SystemInfoRt.isWindows) isWindowsJunction else isSymbolicLink
