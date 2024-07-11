package org.jetbrains.bsp.protocol.jpsCompilation.utils

import java.nio.file.Path

public const val JPS_COMPILED_BASE_DIRECTORY: String = ".jps-compiled"

public object JpsPaths {
  private fun getJpsCompiledBasePath(projectBasePath: Path): Path =
    projectBasePath.resolve(JPS_COMPILED_BASE_DIRECTORY)

  public fun getJpsCompiledProductionPath(projectBasePath: Path, moduleName: String): Path =
    getJpsCompiledBasePath(projectBasePath)
      .resolve("production")
      .resolve("classes")
      .resolve(moduleName)

  public fun getJpsCompiledTestPath(projectBasePath: Path, moduleName: String): Path =
    getJpsCompiledBasePath(projectBasePath)
      .resolve("test")
      .resolve("classes")
      .resolve(moduleName)

  public fun getJpsImlModulesPath(projectBasePath: Path): Path =
    projectBasePath.resolve(".idea").resolve("modules")
}
