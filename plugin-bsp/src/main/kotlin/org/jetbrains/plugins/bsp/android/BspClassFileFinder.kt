package org.jetbrains.plugins.bsp.android

import com.android.tools.idea.project.ModuleBasedClassFileFinder
import com.android.tools.idea.projectsystem.findClassFileInOutputRoot
import com.intellij.openapi.module.Module
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.javaModule

public class BspClassFileFinder(module: Module) : ModuleBasedClassFileFinder(module) {
  override fun findClassFileInModule(module: Module, fqcn: String): VirtualFile? {
    val binaryJars = module.javaModule?.jvmBinaryJars ?: return null

    return binaryJars
      .asSequence()
      .mapNotNull { jar -> VirtualFileManager.getInstance().refreshAndFindFileByNioPath(jar) }
      .mapNotNull { findClassFileInOutputRoot(it, fqcn) }
      .firstOrNull()
  }
}
