package org.jetbrains.bazel.flow.open.exclude

import com.intellij.ide.projectView.actions.MarkRootsManager
import com.intellij.openapi.application.writeAction
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.util.Ref
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.platform.DirectoryProjectConfigurator
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.flow.open.findProjectFolderFromVFile

/**
 * Adds Bazel excludes to the fake module created by platform.
 * https://youtrack.jetbrains.com/issue/IDEA-321160/Platform-solution-for-the-initial-state-of-the-project-model-on-the-first-open
 * Code based on Angular2ProjectConfigurator.
 */
internal class BazelSymlinkExcludeDirectoryProjectConfigurator : DirectoryProjectConfigurator.AsyncDirectoryProjectConfigurator() {
  override suspend fun configure(
    project: Project,
    baseDir: VirtualFile,
    moduleRef: Ref<Module>,
    isProjectCreatedWithWizard: Boolean,
  ) {
    // Call BazelSymlinkExcludeService even if isBazelProject == true because it adds excludes to file watcher
    val bazelWorkspace = findProjectFolderFromVFile(baseDir) ?: return
    val symlinksToExclude = BazelSymlinkExcludeService.getInstance(project).getBazelSymlinksToExclude(bazelWorkspace.toNioPath())
    if (symlinksToExclude.isEmpty()) return

    // Fake Module is going to be removed by CounterPlatformProjectConfigurator anyway
    if (project.isBazelProject) return
    val module = moduleRef.get() ?: return
    val model = ModuleRootManager.getInstance(module).modifiableModel
    val entry = MarkRootsManager.findContentEntry(model, baseDir)
    if (entry != null) {
      val virtualFileManager = VirtualFileManager.getInstance()
      symlinksToExclude
        .mapNotNull { virtualFileManager.findFileByNioPath(it) }
        .forEach { exclude -> entry.addExcludeFolder(exclude) }
      writeAction { model.commit() }
      project.save()
    } else {
      model.dispose()
    }
  }
}
