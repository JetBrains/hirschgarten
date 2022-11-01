package org.jetbrains.plugins.bsp.import

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.PlatformProjectOpenProcessor
import com.intellij.projectImport.ProjectOpenProcessor
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.config.BspPluginIcons
import org.jetbrains.plugins.bsp.extension.points.BspConnectionDetailsGeneratorExtension
import org.jetbrains.plugins.bsp.protocol.connection.BspConnectionDetailsGeneratorProvider
import org.jetbrains.plugins.bsp.protocol.connection.BspConnectionFilesProvider
import javax.swing.Icon

public class BspProjectOpenProcessor : ProjectOpenProcessor() {

  override val name: String = BspPluginBundle.message("plugin.name")

  override val icon: Icon = BspPluginIcons.bsp

  override fun canOpenProject(file: VirtualFile): Boolean {
    val bspConnectionFilesProvider = BspConnectionFilesProvider(file)
    val bspConnectionDetailsGeneratorProvider =
      BspConnectionDetailsGeneratorProvider(file, BspConnectionDetailsGeneratorExtension.extensions())

    return bspConnectionFilesProvider.isAnyBspConnectionFileDefined() or
      bspConnectionDetailsGeneratorProvider.canGenerateAnyBspConnectionDetailsFile()
  }

  override fun doOpenProject(
    virtualFile: VirtualFile,
    projectToClose: Project?,
    forceOpenInNewFrame: Boolean
  ): Project? =
    PlatformProjectOpenProcessor.getInstance().doOpenProject(virtualFile, projectToClose, forceOpenInNewFrame)
      ?.also { it.setProjectDir(virtualFile) }
      ?.also { it.markThatThisIsBspProject() }
}

// TODO move it? do we need it?
private val projectDirKey = Key<VirtualFile>("project-dir")
public fun Project.setProjectDir(projectDir: VirtualFile) {
  this.putUserData(projectDirKey, projectDir)
}

public fun Project.getProjectDirOrThrow(): VirtualFile =
  when (val projectDir = this.getUserData(projectDirKey)) {
    null -> throw IllegalStateException("Project dir is not set!")
    else -> projectDir
  }

private val isBspProjectKey = Key<Boolean>("is-bsp-project")
public fun Project.markThatThisIsBspProject() {
  this.putUserData(isBspProjectKey, true)
}

public fun Project.isBspProject(): Boolean =
  this.getUserData(isBspProjectKey) ?: false
