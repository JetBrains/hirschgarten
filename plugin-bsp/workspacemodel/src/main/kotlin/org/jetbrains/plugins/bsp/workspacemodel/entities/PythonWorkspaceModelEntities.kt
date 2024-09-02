package org.jetbrains.plugins.bsp.impl.magicmetamodel.impl.workspacemodel

import org.jetbrains.plugins.bsp.workspacemodel.entities.GenericModuleInfo
import org.jetbrains.plugins.bsp.workspacemodel.entities.GenericSourceRoot
import org.jetbrains.plugins.bsp.workspacemodel.entities.Module
import org.jetbrains.plugins.bsp.workspacemodel.entities.ResourceRoot
import org.jetbrains.plugins.bsp.workspacemodel.entities.WorkspaceModelEntity

public data class PythonSdkInfo(val version: String, val originalName: String) {
  override fun toString(): String = "$originalName$SEPARATOR$version"

  public companion object {
    public const val PYTHON_SDK_ID: String = "PythonSDK"
    private const val SEPARATOR = '-'

    public fun fromString(value: String): PythonSdkInfo? {
      val parts = value.split(SEPARATOR)
      return parts.takeIf { it.size == 2 }?.let {
        PythonSdkInfo(it[0], it[1])
      }
    }
  }
}

public data class PythonLibrary(val sources: List<String>) : WorkspaceModelEntity()

public data class PythonModule(
  val module: GenericModuleInfo,
  val sourceRoots: List<GenericSourceRoot>,
  val resourceRoots: List<ResourceRoot>,
  val libraries: List<PythonLibrary>,
  val sdkInfo: PythonSdkInfo?,
) : WorkspaceModelEntity(),
  Module {
  override fun getModuleName(): String = module.name
}
