package org.jetbrains.plugins.bsp.android

import com.android.tools.idea.model.ClassJarProvider
import com.intellij.openapi.module.Module
import java.io.File

public class BspClassJarProvider : ClassJarProvider {
  override fun getModuleExternalLibraries(module: Module): List<File> = emptyList()
}
