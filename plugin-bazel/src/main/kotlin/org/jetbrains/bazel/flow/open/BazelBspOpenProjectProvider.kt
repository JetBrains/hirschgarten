package org.jetbrains.bazel.flow.open

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.externalSystem.model.ProjectSystemId
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.isFile
import org.jetbrains.bazel.config.BazelPluginConstants
import org.jetbrains.bsp.sdkcompat.flow.open.AbstractOpenProjectProviderCompat

internal class BazelBspOpenProjectProvider : AbstractOpenProjectProviderCompat() {
  private val log = logger<BazelBspOpenProjectProvider>()

  override fun systemIdCompat(): ProjectSystemId = BazelPluginConstants.SYSTEM_ID

  override fun isProjectFileCompat(file: VirtualFile): Boolean = file.isFile && file.name in BazelPluginConstants.WORKSPACE_FILE_NAMES

  override suspend fun linkProjectCompat(projectFile: VirtualFile, project: Project) {
    log.debug("Link BazelBsp project $projectFile to existing project ${project.name}")
    performOpenBazelProjectViaBspPlugin(project, projectFile)
  }
}
