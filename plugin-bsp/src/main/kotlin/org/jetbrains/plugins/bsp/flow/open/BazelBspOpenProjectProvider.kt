package org.jetbrains.plugins.bsp.flow.open

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.externalSystem.importing.AbstractOpenProjectProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.isFile
import org.jetbrains.plugins.bsp.config.BazelBspConstants
import org.jetbrains.plugins.bsp.services.BspCoroutineService
import org.jetbrains.plugins.bsp.ui.actions.registered.performOpenBazelProjectViaBspPlugin

internal class BazelBspOpenProjectProvider : AbstractOpenProjectProvider() {
  private val log = logger<BazelBspOpenProjectProvider>()

  override val systemId = BazelBspConstants.SYSTEM_ID

  override fun isProjectFile(file: VirtualFile): Boolean =
    file.isFile && file.name in BazelBspConstants.BUILD_FILE_NAMES

  override fun linkToExistingProject(projectFile: VirtualFile, project: Project) {
    BspCoroutineService.getInstance(project).start {
      linkToExistingProjectAsync(projectFile, project)
    }
  }

  override suspend fun linkToExistingProjectAsync(projectFile: VirtualFile, project: Project) {
    log.debug("Link BazelBsp project $projectFile to existing project ${project.name}")
    performOpenBazelProjectViaBspPlugin(project, projectFile)
  }
}
