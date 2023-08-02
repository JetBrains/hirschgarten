package org.jetbrains.magicmetamodel.impl.workspacemodel

import org.jetbrains.magicmetamodel.impl.ModuleState
import org.jetbrains.magicmetamodel.impl.toState

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
) : WorkspaceModelEntity(), Module {
  override fun toState(): ModuleState = ModuleState(
    module = module.toState(),
    baseDirContentRoot = null,
    sourceRoots = sourceRoots.map { it.toState() },
    resourceRoots = resourceRoots.map { it.toString() },
    libraries = libraries.map { it.toState() },
    sdkInfo = sdkInfo?.toState(),
  )

  override fun getModuleName(): String = module.name
}
