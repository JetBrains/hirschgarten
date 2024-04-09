package org.jetbrains.plugins.bsp.android

import com.android.tools.idea.project.ModuleBasedClassFileFinder
import com.android.tools.idea.projectsystem.findClassFileInOutputRoot
import com.intellij.openapi.module.Module
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.backend.workspace.virtualFile
import org.jetbrains.workspacemodel.entities.jvmBinaryJarsEntity

public class BspClassFileFinder(module: Module) : ModuleBasedClassFileFinder(module) {
  override fun findClassFileInModule(module: Module, fqcn: String): VirtualFile? {
    val binaryJars = module.moduleEntity?.jvmBinaryJarsEntity ?: return null

    return binaryJars
      .jars
      .asSequence()
      .mapNotNull { it.virtualFile }
      .mapNotNull { findClassFileInOutputRoot(it, fqcn) }
      .firstOrNull()
  }
}
