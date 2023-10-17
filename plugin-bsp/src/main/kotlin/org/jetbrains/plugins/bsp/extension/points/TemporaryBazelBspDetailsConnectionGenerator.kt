package org.jetbrains.plugins.bsp.extension.points

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.plugins.bsp.config.BazelBspConstants
import org.jetbrains.plugins.bsp.config.BspPluginTemplates
import java.io.OutputStream
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.writeText

private const val DEFAULT_PROJECT_VIEW_FILE_NAME = "projectview.bazelproject"

public class TemporaryBazelBspDetailsConnectionGenerator : BspConnectionDetailsGeneratorExtension {
  private lateinit var projectViewFilePath: Path
  public override fun id(): String = BazelBspConstants.ID

  public override fun displayName(): String = "Bazel"

  public override fun canGenerateBspConnectionDetailsFile(projectPath: VirtualFile): Boolean =
    projectPath.children.any { it.name in BazelBspConstants.BUILD_FILE_NAMES }

  public override fun generateBspConnectionDetailsFile(
    projectPath: VirtualFile,
    outputStream: OutputStream,
    project: Project,
  ): VirtualFile {
    initializeProjectViewFile(projectPath)
    executeAndWait(
      command = calculateInstallerCommand(),
      projectPath = projectPath,
      outputStream = outputStream,
      project = project,
    )
    return getChild(projectPath, listOf(".bsp", "bazelbsp.json"))!!
  }

  private fun initializeProjectViewFile(projectPath: VirtualFile) {
    projectViewFilePath = calculateProjectViewFilePath(projectPath)
    setDefaultProjectViewFilePathContentIfNotExists()
  }

  private fun setDefaultProjectViewFilePathContentIfNotExists() {
    if (!projectViewFilePath.exists()) {
      projectViewFilePath.writeText(BspPluginTemplates.defaultBazelProjectViewContent)
    }
  }

  private fun calculateInstallerCommand(): List<String> = listOf(
    ExternalCommandUtils.calculateJavaExecPath(),
    "-cp",
    ExternalCommandUtils
      .calculateNeededJars(
        org = "org.jetbrains.bsp",
        name = "bazel-bsp",
        version = "3.1.0-20231017-e7faadc-NIGHTLY",
      )
      .joinToString(":"),
    "org.jetbrains.bsp.bazel.install.Install",
  ) + calculateProjectViewFileInstallerOption()

  private fun calculateProjectViewFileInstallerOption(): List<String> =
    listOf("-p", "$projectViewFilePath")

  private fun calculateProjectViewFilePath(projectPath: VirtualFile): Path =
    projectPath.toNioPath().toAbsolutePath().resolve(DEFAULT_PROJECT_VIEW_FILE_NAME)
}
