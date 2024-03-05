package org.jetbrains.plugins.bsp.android

import com.android.tools.idea.model.ClassJarProvider
import com.intellij.openapi.module.Module
import com.intellij.openapi.vfs.VfsUtilCore
import org.jetbrains.android.facet.AndroidRootUtil.getExternalLibraries
import java.io.File

public class BspClassJarProvider : ClassJarProvider {
  override fun getModuleExternalLibraries(module: Module): List<File> =
    getExternalLibraries(module).map { VfsUtilCore.virtualToIoFile(it) }
}
