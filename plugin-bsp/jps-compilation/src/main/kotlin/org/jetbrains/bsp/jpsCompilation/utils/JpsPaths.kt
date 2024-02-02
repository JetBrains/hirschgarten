package org.jetbrains.bsp.jpsCompilation.utils

import java.nio.file.Path

public object JpsPaths {
  private fun getJpsCompiledBaseDirectory(projectBasePath: Path): Path =
    projectBasePath.resolve(".jps-compiled")

  public fun getJpsCompiledProductionDirectory(projectBasePath: Path, moduleName: String): Path =
    getJpsCompiledBaseDirectory(projectBasePath)
      .resolve("production")
      .resolve("classes")
      .resolve(moduleName)

  public fun getJpsCompiledTestDirectory(projectBasePath: Path, moduleName: String): Path =
    getJpsCompiledBaseDirectory(projectBasePath)
      .resolve("test")
      .resolve("classes")
      .resolve(moduleName)

  public fun getJpsImlModulesPath(projectBasePath: Path): Path =
    projectBasePath.resolve(".idea").resolve("modules")
}
