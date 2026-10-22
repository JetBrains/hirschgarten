package org.jetbrains.bazel.flow.exclude

import com.intellij.platform.backend.workspace.virtualFile
import kotlinx.coroutines.runBlocking
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.testproject.createConvenienceSymlink
import org.jetbrains.bazel.test.framework.testproject.projectDirectories
import org.jetbrains.bazel.test.framework.testproject.testBazelProjectFixture
import org.jetbrains.bazel.workspace.bazelProjectDirectoriesEntity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertIterableEquals
import org.junit.jupiter.api.Test

@BazelTestApplication
internal class BazelSymlinkExcludeStartupActivityTest {
  private val testBazel by testBazelProjectFixture(openAfterCreation = false)

  @Test
  fun `should scan for bazel symlinks on project startup and update workspace model`() = runBlocking {
    // GIVEN
    val bazelSymlinkExcludeService = BazelSymlinkExcludeService.getInstance(testBazel.project)
    val convenientSymlink = testBazel.createConvenienceSymlink("bazel-bin")

    testBazel.projectDirectories()

    // WHEN
    BazelSymlinkExcludeStartupActivity().execute(testBazel.project)
    bazelSymlinkExcludeService.refreshWorkspaceModel()

    // THEN
    assertEquals(setOf(convenientSymlink), bazelSymlinkExcludeService.getBazelSymlinksToExclude())

    // AND
    val actualPaths = testBazel.project.bazelProjectDirectoriesEntity()!!.excludedRoots.mapNotNull { it.virtualFile?.toNioPath() }
    assertIterableEquals(listOf(convenientSymlink), actualPaths)
  }
}
