package org.jetbrains.bazel.flow

import com.intellij.ide.impl.OpenProjectTask
import com.intellij.ide.impl.ProjectUtil
import com.intellij.openapi.externalSystem.autolink.ExternalSystemUnlinkedProjectAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.testFramework.common.timeoutRunBlocking
import com.intellij.testFramework.junit5.TestApplication
import com.intellij.testFramework.junit5.fixture.tempPathFixture
import com.intellij.testFramework.useProjectAsync
import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.commons.constants.Constants
import org.jetbrains.bazel.config.BazelPluginConstants
import org.junit.jupiter.api.Test
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.time.Duration.Companion.minutes

/**
 * A Bazel link replaces the open project with a Bazel project of the same folder. A link of another folder, such as a
 * workspace member of JetBrains Light, must leave the open project alone. Otherwise the open project closes, and the
 * caller opens it again, without an end.
 */
@TestApplication
internal class BazelUnlinkedProjectAwareTest {
  private val directoryRoot by tempPathFixture()

  @Test
  fun `a link of a folder that is not the project base path leaves the project open`(): Unit = timeoutRunBlocking(2.minutes) {
    val projectDir = directoryRoot.resolve("project")
    projectDir.resolve(Project.DIRECTORY_STORE_FOLDER).createDirectories()
    val bazelDir = directoryRoot.resolve("bazel").createDirectories()
    bazelDir.resolve(Constants.MODULE_BAZEL_FILE_NAME).createFile()

    ProjectUtil.openProject(projectDir, OpenProjectTask { createModule = false })!!.useProjectAsync { project ->
      val openProjects = ProjectManager.getInstance().openProjects.size

      bazelUnlinkedProjectAware().linkAndLoadProjectAsync(project, bazelDir.invariantSeparatorsPathString)

      project.isDisposed shouldBe false
      ProjectManager.getInstance().openProjects.size shouldBe openProjects
    }
  }

  private fun bazelUnlinkedProjectAware(): ExternalSystemUnlinkedProjectAware =
    ExternalSystemUnlinkedProjectAware.EP_NAME.extensionList.single { it.systemId == BazelPluginConstants.SYSTEM_ID }
}
