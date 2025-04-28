package org.jetbrains.bazel.sdkcompat

import com.intellij.openapi.application.readAction
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VirtualFile

object AssignFileToModuleListenerCompat {
  suspend fun getModulesForFile(newFile: VirtualFile, project: Project): Set<Module> =
    readAction { ProjectFileIndex.getInstance(project).getModulesForFile(newFile, true) }
}
